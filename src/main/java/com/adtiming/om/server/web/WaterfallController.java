// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.web;

import com.adtiming.om.pb.AdNetworkPB;
import com.adtiming.om.pb.CommonPB;
import com.adtiming.om.server.dto.*;
import com.adtiming.om.server.service.AppConfig;
import com.adtiming.om.server.service.CacheService;
import com.adtiming.om.server.service.GeoService;
import com.adtiming.om.server.service.LogService;
import com.adtiming.om.server.util.RandomUtil;
import com.adtiming.om.server.util.Util;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.lang.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.adtiming.om.server.dto.AdNetwork.*;
import static com.adtiming.om.server.dto.WaterfallResponse.*;
import static java.nio.charset.StandardCharsets.UTF_8;

@RestController
public class WaterfallController extends WaterfallBase {

    private static final Logger LOG = LogManager.getLogger();

    @Resource
    private AppConfig cfg;

    @Resource
    private GeoService geoService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private CacheService cacheService;

    @Resource
    private LogService logService;

    @Resource
    private HttpAsyncClient httpAsyncClient;

    @Resource
    private ResponseContentEncoding responseContentEncoding;

    private final RequestConfig bidReqCfg = RequestConfig.custom()
            .setConnectTimeout(2000)
            .setSocketTimeout(1000)
            .setRedirectsEnabled(false)
            .build();
    private final Header bidReqUa = new BasicHeader("User-Agent", "om-bid-s2s");
    private final Header bidReqAe = new BasicHeader("Accept-Encoding", "gzip, deflate");

    /**
     * waterfall
     */
    @PostMapping(value = "/wf", params = "v=1")
    public Object wf(HttpServletRequest req,
                     @RequestParam("v") int version, // api version
                     @RequestParam("plat") int plat, // platform
                     @RequestParam("sdkv") String sdkv,
                     @RequestHeader("Host") String reqHost,
                     @RequestHeader(value = "User-Agent", required = false) String ua,
                     @RequestHeader(value = "x-om-debug", required = false) String debug,
                     @RequestBody byte[] data) throws IOException {
        WaterfallRequest o = fillRequestParams(data, version, sdkv, plat, ua, reqHost, req, geoService, cfg, cacheService);
        if (o == null) {
            return ResponseEntity.badRequest().build();
        }

        WaterfallResponse res = new WaterfallResponse(o.getAbt(), debug != null);

        LrRequest lr = o.copyTo(new LrRequest());
        lr.setWfReq(1);
        lr.setType(LrRequest.TYPE_WATERFALL_REQUEST);

        Placement placement = cacheService.getPlacement(o.getPid());
        if (placement == null) {
            lr.setStatus(0, "placement invalid").writeToLog(logService);
            res.setCode(CODE_PLACEMENT_INVALID).setMsg("placement invalid");
            return response(res);
        }
        o.setPlacement(placement);
        lr.setPlacement(placement);

        o.setAdType(placement.getAdTypeValue());
//        o.setAbt(cacheService.getAbTestMode(placement.getId(), o));

        res.addDebug("request ip:%s, country:%s", o.getIp(), o.getCountry());

        Integer devDevicePubId = cacheService.getDevDevicePub(o.getDid());
        // dev device uses the configured abTest mode
        Integer devAbMode = null;
        if (devDevicePubId != null) {
            if (res.isDebugEnabled()) {
                Integer devAdnId = cacheService.getDevAppAdnId(placement.getPubAppId());
                res.addDebug("Is Dev Device,dev publisher: %d", devDevicePubId);
                res.addDebug("Is Dev model,dev adnId: %d", devAdnId);
            }
            devAbMode = cacheService.getDevDeviceAbtMode(o.getDid());
        }
        if (devAbMode != null) {
            o.setAbt(devAbMode);
            res.setAbt(devAbMode);
            res.addDebug("Dev Device ABTest Mode: %s", CommonPB.ABTest.forNumber(o.getAbt()));
        }

        PublisherApp pubApp = cacheService.getPublisherApp(placement.getPubAppId());
        if (pubApp == null) {
            lr.setStatus(0, "app invalid").writeToLog(logService);
            res.setCode(CODE_PUB_APP_INVALID).setMsg("app invalid");
            return response(res);
        }

        if (StringUtils.isEmpty(o.getCountry())) {
            lr.setStatus(0, "country not found").writeToLog(logService);
            res.setCode(CODE_COUNTRY_NOT_FOUND).setMsg("country not found");
            return response(res);
        }

        //placement target filter
        if (!matchPlacement(o, placement)) {
            lr.setStatus(0, "placement not allowed").writeToLog(logService);
            res.setCode(CODE_PLACEMENT_INVALID).setMsg("placement not allowed");
            return response(res);
        }

        Map<Integer, Instance> bidInsMap = new HashMap<>();
        List<WaterfallInstance> insList = getIns(devDevicePubId, o, placement, res, bidInsMap);

        if (o.getBids2s() != null) {
            o.getBids2s().removeIf(bidderToken -> {
                Instance instance = bidInsMap.get(bidderToken.iid);
                if (instance == null) {
                    LOG.debug("instance not in rule, {}", bidderToken.iid);
                    res.addDebug("instance not found or bid off, remove instance: %d", bidderToken.iid);
                    return true;
                }
                AdNetworkPB.AdNetwork adn = cacheService.getAdNetwork(instance.getAdnId());
                if (adn == null || StringUtils.isBlank(adn.getBidEndpoint())) {
                    LOG.debug("adn not found or s2s bidding not support, {}", bidderToken.iid);
                    res.addDebug("adnApp not found, remove instance: %d", bidderToken.iid);
                    return true;
                }
                AdNetworkApp adnApp = cacheService.getAdnApp(instance.getPubAppId(), instance.getAdnId());
                if (adnApp == null) {
                    LOG.debug("adnApp not found, {}", bidderToken.iid);
                    res.addDebug("adnApp not found, remove instance: %d", bidderToken.iid);
                    return true;
                }
                bidderToken.adn = instance.getAdnId();
                if (bidderToken.adn == ADN_CROSSPROMOTION) {
                    bidderToken.pkey = String.valueOf(placement.getId());
                    bidderToken.appId = pubApp.getAppKey();
                } else {
                    bidderToken.pkey = instance.getPlacementKey();
                    bidderToken.appId = adnApp.getAppKey();
                }

                bidderToken.endpoint = adn.getBidEndpoint();
                if (bidderToken.adn == ADN_FACEBOOK) {
                    bidderToken.endpoint = bidderToken.endpoint.replace("${PLATFORM_ID}", bidderToken.appId);
                }
                return false;
            });
        }

        if (CollectionUtils.isEmpty(o.getBids2s())) {
            List<Integer> ins = getInsWithBidInstance(o, insList);
            if (ins == null || ins.isEmpty()) {
                res.setCode(CODE_INSTANCE_EMPTY).setMsg("instance empty");
                lr.setStatus(0, res.getMsg()).writeToLog(logService);
                return response(res);
            }

            res.setIns(ins);
            lr.setStatus(1, null).writeToLog(logService);
            return response(res);
        } else {
            // process s2s bid
            // setAttribute 用于 #asyncExceptionHandler
            req.setAttribute("res", res);
            req.setAttribute("lr", lr);
            req.setAttribute("params", o);
            req.setAttribute("insList", insList);

            boolean isTest = devDevicePubId != null;
            return bid(o, isTest, placement, res, dr -> {
                try {
                    if (dr.isSetOrExpired()) {
                        LOG.warn("dr isSetOrExpired, ignore");
                        return;
                    }

                    if (res.isDebugEnabled() && o.getBidPriceMap() != null && !o.getBidPriceMap().isEmpty()) {
                        o.getBidPriceMap().forEach((iid, bidPrice) ->
                                res.addDebug("instance:%d, bidPrice:%f", iid, bidPrice));
                    }

                    List<Integer> ins = getInsWithBidInstance(o, insList);
                    if (ins == null || ins.isEmpty()) {
                        res.setCode(CODE_NOAVAILABLE_INSTANCE).setMsg("no available instance");
                        dr.setResult(response(res));
                        lr.setStatus(0, res.getMsg()).writeToLog(logService);
                        return;
                    }

                    res.setIns(ins);
                    lr.setStatus(1, null).writeToLog(logService);
                    dr.setResult(response(res));
                } catch (IllegalArgumentException | IllegalStateException e) {
                    LOG.warn("set dr result error, {}", e.toString());
                } catch (Exception e) {
                    LOG.error("set dr result error", e);
                    dr.setResult(ResponseEntity.noContent().build());
                }
            });

        }

    }

    List<WaterfallInstance> getIns(Integer devDevicePubId, WaterfallRequest o, Placement p, WaterfallResponse res,
                                   @Nullable Map<Integer, Instance> returnBidIids) {
        //dev mode
        List<Instance> devIns = matchDev(devDevicePubId, p, cacheService);
        if (devIns != null && !devIns.isEmpty()) {
            List<WaterfallInstance> rv = new ArrayList<>(devIns.size());
            for (Instance ins : devIns) {
                if (ins.isHeadBidding()) {
                    if (returnBidIids != null) returnBidIids.put(ins.getId(), ins);
                    continue;
                }
                rv.add(new WaterfallInstance(ins, 0F));
            }
            return rv.isEmpty() ? null : rv;
        }

        List<InstanceRule> rules = cacheService.getCountryRules(p.getId(), o.getCountry());
        InstanceRule matchedRule = getMatchedRule(rules, o);

        if (res.isDebugEnabled()) {
            if (matchedRule != null) {
                res.addDebug("hit rule: %d", matchedRule.getId());
            } else {
                res.addDebug("miss rule");
            }
        }

        List<AdNetworkApp> adnApps = cacheService.getAdnApps(p.getPubAppId());
        if (adnApps == null || adnApps.isEmpty()) {
            return null;
        }

        Set<Integer> blockAdnIds = getBlockAdnIds(adnApps, o);

        List<Instance> mps = cacheService.getPlacementInstances(p.getId());
        if (mps == null || mps.isEmpty()) {
            return null;
        }

        List<Instance> pmInstances = new ArrayList<>(mps);
        List<WaterfallInstance> sortIns;
        Map<Integer, Integer> insAdnIdMap = new HashMap<>(pmInstances.size());
        if (matchedRule != null) {//has rule
            Set<Integer> insIdSet = matchedRule.getInstanceList();
            if (res.isDebugEnabled()) {
                res.addDebug("Rule instance list: %s", insIdSet);
            }
            if (insIdSet.isEmpty()) {
                return null;
            }
            if (matchedRule.isAutoOpt()) {//auto waterfall, ecpm priority
                Map<Integer, Float> insEcpm = new HashMap<>(insIdSet.size());
                float totalEcpm = 0;
                Map<Float, List<WaterfallInstance>> priorityIns = new HashMap<>(insIdSet.size());
                for (Instance ins : pmInstances) {
                    if (!insIdSet.contains(ins.getId())) {
                        continue;
                    }
                    if (blockAdnIds.contains(ins.getAdnId()) || !ins.matchInstance(o)) {
                        continue;
                    }
                    if (ins.isHeadBidding()) {
                        if (returnBidIids != null) returnBidIids.put(ins.getId(), ins);
                        // Remove bid instance of nobid
                        continue;
                    }
                    insAdnIdMap.put(ins.getId(), ins.getAdnId());
                    totalEcpm += setInstanceEcpm(o, ins, priorityIns, insEcpm, totalEcpm, res, p.getAdTypeValue(), cacheService);
                }
                if (priorityIns.isEmpty()) {
                    res.addDebug("Has no instance match");
                    return null;
                }
                if (priorityIns.containsKey(-1F)) {
                    float avgEcpm = totalEcpm / insEcpm.size();//avg ecpm
                    List<WaterfallInstance> needAvgEcpmIns = priorityIns.get(-1F);
                    if (res.isDebugEnabled() && !CollectionUtils.isEmpty(needAvgEcpmIns)) {
                        res.addDebug("Rule auto optimize,Instance AvgEcpm:%f,UseAvgEcpm:%s", avgEcpm, needAvgEcpmIns);
                    }
                    priorityIns.put(avgEcpm, needAvgEcpmIns);
                    priorityIns.remove(-1F);
                }
                sortIns = Util.sortByEcpm(priorityIns);
                if (res.isDebugEnabled()) {
                    res.addDebug("Instance Ecpm: %s", insEcpm);
                    res.addDebug("Instance sort: %s", sortIns);
                }
            } else { // Non-auto sort by rule configuration

                Map<Integer, Integer> insConfigWeight = matchedRule.getInstanceWeightMap();
                Map<WaterfallInstance, Integer> insWeight = new HashMap<>(insIdSet.size());
                for (Instance ins : pmInstances) {
                    if (!insIdSet.contains(ins.getId())) {
                        continue;
                    }
                    if (blockAdnIds.contains(ins.getAdnId()) || !ins.matchInstance(o)) {
                        continue;
                    }
                    if (ins.isHeadBidding()) {
                        if (returnBidIids != null) returnBidIids.put(ins.getId(), ins);
                        // Remove bid instance of nobid
                        continue;
                    }
                    insAdnIdMap.put(ins.getId(), ins.getAdnId());

                    InstanceEcpm ecpm = cacheService.getInstanceEcpm(ins.getAdnId(), ins.getId(), o.getCountry(), p.getAdTypeValue());
                    float ecpmVal = 0F;
                    if (ecpm != null && ecpm.ecpm > -1F) {
                        ecpmVal = ecpm.ecpm;
                    }

                    int weight = insConfigWeight.get(ins.getId());
                    if (weight > 0) {
                        insWeight.put(new WaterfallInstance(ins, ecpmVal), weight);
                    }
                }
                if (insAdnIdMap.isEmpty()) {
                    res.addDebug("Has no instance match");
                    return null;
                }

                // Sorting type, 0: use weight; 1: use absolute priority
                if (matchedRule.getSortType() == 0) {
                    sortIns = RandomUtil.sortByWeight(insWeight);
                } else {
                    // When absolute priority, an instance is randomly selected with the same weight
                    sortIns = Util.sortByPriority(insWeight);
                }

                if (res.isDebugEnabled()) {
                    String sortName = (matchedRule.getSortType() == 1 ? "Priority" : "Weight");
                    res.addDebug("Sort type:%s, %s:%s", sortName, sortName, insWeight);
                    res.addDebug("Instance sort: %s", sortIns);
                }
            }
        } else {// Rule is not configured by ecpm absolute priority ordering
            Map<Integer, Float> insEcpm = new HashMap<>(pmInstances.size());
            float totalEcpm = 0;
            Map<Float, List<WaterfallInstance>> priorityIns = new HashMap<>(pmInstances.size());
            for (Instance ins : pmInstances) {
                if (blockAdnIds.contains(ins.getAdnId()) || !ins.matchInstance(o)) {
                    continue;
                }
                if (ins.isHeadBidding()) {
                    if (returnBidIids != null) returnBidIids.put(ins.getId(), ins);
                    // Remove bid instance of nobid
                    continue;
                }
                insAdnIdMap.put(ins.getId(), ins.getAdnId());

                totalEcpm += setInstanceEcpm(o, ins, priorityIns, insEcpm, totalEcpm, res, p.getAdTypeValue(), cacheService);
            }
            if (priorityIns.isEmpty()) {
                res.addDebug("Has no instance match");
                return null;
            }
            if (priorityIns.containsKey(-1F)) {//No ecpm use average ecpm
                float avgEcpm = totalEcpm / insEcpm.size();
                List<WaterfallInstance> needAvgEcpmIns = priorityIns.get(-1F);
                if (res.isDebugEnabled() && !CollectionUtils.isEmpty(needAvgEcpmIns)) {
                    res.addDebug("No rule auto optimize,Instance AvgEcpm:%f,UseAvgEcpm:%s", avgEcpm, needAvgEcpmIns);
                }
                priorityIns.put(avgEcpm, needAvgEcpmIns);
                priorityIns.remove(-1F);
            }
            sortIns = Util.sortByEcpm(priorityIns);
            if (res.isDebugEnabled()) {
                res.addDebug("Instance Ecpm: %s", insEcpm);
                res.addDebug("Instance sort: %s", sortIns);
            }
        }
        return sortIns;
    }

    private float setInstanceEcpm(WaterfallRequest o, Instance ins, Map<Float, List<WaterfallInstance>> priorityIns,
                                  Map<Integer, Float> insEcpm, float totalEcpm, WaterfallResponse res,
                                  int adType, CacheService cacheService) {
        /*Float bidPrice = o.getBidPrice(ins.getId());
        if (bidPrice != null) {
            priorityIns.computeIfAbsent(bidPrice, k -> new ArrayList<>()).add(ins.getId());
            insEcpm.put(ins.getId(), bidPrice);
            totalEcpm += bidPrice;
            if (DEBUG) {
                dmsg.add("Instance: " + ins.getId() + ", BidPrice:" + bidPrice);
            }
        } else {*/
        InstanceEcpm ecpm = cacheService.getInstanceEcpm(ins.getAdnId(), ins.getId(), o.getCountry(), adType);
        priorityIns.computeIfAbsent(ecpm.ecpm, k -> new ArrayList<>()).add(new WaterfallInstance(ins, ecpm.ecpm));
        if (ecpm.ecpm > -1F) {
            insEcpm.put(ins.getId(), ecpm.ecpm);
        }
        totalEcpm += ecpm.ecpm;
        if (res.isDebugEnabled()) {
            try {
                res.addDebug("Instance Ecpm: %s", objectMapper.writeValueAsString(ecpm));
            } catch (JsonProcessingException e) {
                LOG.error("error", e);
            }
        }
        //}
        return totalEcpm;
    }

    /*void removeUnsupportedInstance(CacheService cs, int deviceAbTest, boolean isAppAbTestOn,
                                   Placement p, List<Instance> instances) {
        //App opens abtest, placement is not open, device belongs to group B
        if (isAppAbTestOn && !p.isAbTestOn() && deviceAbTest == 1) {
            List<AdNetworkApp> bGroupAdnApp = cs.getAdnApps(p.getPubAppId());
            if (bGroupAdnApp != null && !bGroupAdnApp.isEmpty()) {
                Map<Integer, AdNetworkApp> bGroupMsdkAppMap = bGroupAdnApp.stream().collect(Collectors.toMap(AdNetworkApp::getAdnId, o -> o));
                //Remove AdNetwork not included in group B
                instances.removeIf(o -> !bGroupMsdkAppMap.containsKey(o.getAdnId()));
            }
        }
    }*/

    public List<Integer> getInsWithBidInstance(WaterfallRequest req, List<WaterfallInstance> insList) {
        if (CollectionUtils.isEmpty(insList) && req.getBidPriceMap().isEmpty()) {
            return null;
        }

        if (CollectionUtils.isEmpty(insList)) {
            insList = new ArrayList<>();
        }

        // insert bid instance
        if (!req.getBidPriceMap().isEmpty()) {
            List<WaterfallInstance> finalInsList = insList;
            req.getBidPriceMap().forEach((iid, bidPrice) -> {
                boolean added = false;
                Instance ins = cacheService.getInstanceById(iid);
                if (ins == null) {
                    return;
                }
                WaterfallInstance bidIns = new WaterfallInstance(ins, bidPrice);
                for (int i = 0; i < finalInsList.size(); i++) {
                    float ecpm = finalInsList.get(i).ecpm;
                    if (bidPrice > ecpm) {
                        finalInsList.add(i, bidIns);
                        added = true;
                        break;
                    }
                }
                if (!added) {
                    finalInsList.add(bidIns);
                }
            });
        }

        return insList.stream().map(o -> o.instance.getId()).collect(Collectors.toList());
    }

    @FunctionalInterface
    public interface S2SBidCallback {
        void cb(DeferredResult<Object> dr);
    }

    private DeferredResult<Object> bid(WaterfallRequest o, boolean isTest, Placement placement, WaterfallResponse resp, S2SBidCallback callback) {
        final boolean DEBUG = resp.getDebug() != null;
        resp.bidresp = new ArrayList<>(o.getBids2s().size());
        DeferredResult<Object> dr = new DeferredResult<>(1500L);

        AtomicInteger count = new AtomicInteger(o.getBids2s().size());
        for (WaterfallRequest.S2SBidderToken bidderToken : o.getBids2s()) {
            HttpPost bidreq = new HttpPost(bidderToken.endpoint);
            bidreq.setConfig(bidReqCfg);
            bidreq.setHeader(bidReqUa);
            bidreq.setHeader(bidReqAe);
            if (bidderToken.adn == ADN_FACEBOOK) {
                bidreq.setHeader("X-FB-Pool-Routing-Token", bidderToken.token);
            } else if (bidderToken.adn == ADN_MINTEGRAL) {
                bidreq.setHeader("openrtb", "2.5");
            } else if (bidderToken.adn == ADN_VUNGLE) {
                bidreq.setHeader("X-OpenRTB-Version", "2.5");
                bidreq.setHeader("Accept", "application/json");
            }

            String reqData = buildBidReqData(o, isTest, placement, bidderToken).toJSONString();
            bidreq.setEntity(new StringEntity(reqData, ContentType.APPLICATION_JSON));

            if (DEBUG) {
                resp.getDebug().add(String.format("bidreq to %d, %s, bid: %s", bidderToken.iid, bidderToken.endpoint, reqData));
            }

            // write bid request log
            LrRequest lr = o.copyTo(new LrRequest());
            lr.setBid(1);
            lr.setBidReq(1);
            lr.setType(EventLogRequest.INSTANCE_BID_REQUEST);
            lr.setMid(bidderToken.adn);
            lr.setPlacement(placement);
            lr.setIid(bidderToken.iid);
//            lr.writeToLog(logService);

            HttpClientContext hcc = HttpClientContext.create();
            httpAsyncClient.execute(bidreq, hcc, new FutureCallback<HttpResponse>() {
                @Override
                public void completed(HttpResponse result) {
                    HttpEntity entity = null;
                    String content = null;
                    JSONObject bidresp = null;
                    String err = null;
                    String headers = null;
                    try {
                        if (DEBUG) {
                            headers = StringUtils.join(result.getAllHeaders(), '\n');
                        }
                        responseContentEncoding.process(result, hcc);
                        entity = result.getEntity();
                        StatusLine sl = result.getStatusLine();
                        int code = sl.getStatusCode();
                        if (code != 200) {
                            if (bidderToken.adn == ADN_FACEBOOK) {
                                Header hErr = result.getFirstHeader("x-fb-an-errors");
                                if (hErr != null) {
                                    err = hErr.getValue();
                                }
                            }
                            LOG.debug("pid: {}, adn {} nobid: {}, err: {}", o.getPid(), bidderToken.adn, sl, err);
                        }
                        if (entity != null) {
                            content = EntityUtils.toString(entity, UTF_8);
                            if (ContentType.APPLICATION_JSON.getMimeType().equals(ContentType.get(entity).getMimeType())) {
                                bidresp = JSONObject.parseObject(content);
                            }
                        }
                        if (DEBUG) {
                            resp.getDebug().add(String.format("bidresp from %d, headers: %s, body: %s",
                                    bidderToken.iid, headers, content));
                        }
                    } catch (Exception e) {
                        if (DEBUG) {
                            resp.getDebug().add(String.format("bidresp from %d error, %s, headers: %s, body: %s",
                                    bidderToken.iid, e.toString(), headers, content));
                        }
                        LOG.error("adn {} parse error, req: {}, resp: {}", bidderToken.adn, reqData, content, e);
                    } finally {
                        EntityUtils.consumeQuietly(entity);
                    }
                    handleS2SBidResponse(count, bidderToken, isTest, bidresp, err, o, resp, lr, callback, dr);
                }

                @Override
                public void failed(Exception ex) {
                    String msg = ex.toString();
                    LOG.debug("adn {} failed: {}", bidderToken.adn, msg);
                    if (DEBUG) {
                        resp.getDebug().add(String.format("bidresp from %d failed: %s", bidderToken.iid, ex.toString()));
                    }
                    handleS2SBidResponse(count, bidderToken, isTest, null, msg, o, resp, lr, callback, dr);
                }

                @Override
                public void cancelled() {
                    handleS2SBidResponse(count, bidderToken, isTest, null, "cancelled", o, resp, lr, callback, dr);
                }
            });
        }
        return dr;
    }

    private void handleS2SBidResponse(
            AtomicInteger count, WaterfallRequest.S2SBidderToken bidderToken, boolean isTest,
            JSONObject bidresp, String err, WaterfallRequest o, WaterfallResponse resp, LrRequest lr,
            S2SBidCallback callback, DeferredResult<Object> dr) {
        try {
            WaterfallResponse.S2SBidResponse bres = new WaterfallResponse.S2SBidResponse();
            bres.iid = bidderToken.iid;
            bres.err = err;
            resp.bidresp.add(bres);
            if (bidresp == null) {
                bres.nbr = 0;
            } else {
                bres.nbr = bidresp.getInteger("nbr");
                if (bres.nbr != null) {
                    return;
                }
                String cur = bidresp.getString("cur");
//                String bidid = bidresp.getString("bidid");
                JSONObject bid = bidresp
                        .getJSONArray("seatbid").getJSONObject(0)
                        .getJSONArray("bid").getJSONObject(0);
                float price = cacheService.getUsdMoney(cur, bid.getFloatValue("price"));
                bres.price = isTest ? 0F : price; // TestMode, no billing
                bres.adm = bid.getString("adm");
                bres.nurl = bid.getString("nurl");
                bres.lurl = bid.getString("lurl");
                o.setBidPrice(bidderToken.iid, price);
                if (bidderToken.adn == ADN_VUNGLE) {
                    bres.expire = 5;
                } else {
                    bres.expire = 30;
                }

                // write bid response log
//                LrRequest lr = o.copyTo(new LrRequest());
//                lr.setBid(1);
//                lr.setType(EventLogRequest.INSTANCE_BID_RESPONSE);
//                lr.setMid(bidderToken.adn);
//                lr.setPlacement(placement);
//                lr.setIid(bidderToken.iid);
                lr.setBidRes(1);
                if (!isTest) {
                    lr.setBidResPrice(price);
                }
            }
        } catch (Exception e) {
            LOG.error("set s2s bidresp error", e);
        } finally {
            if (count.decrementAndGet() == 0) {
                callback.cb(dr);
            }
            lr.writeToLog(logService);
        }
    }

    private JSONObject buildBidReqData(WaterfallRequest ps, boolean isTest, Placement placement, WaterfallRequest.S2SBidderToken bidderToken) {
        JSONObject bid = new JSONObject().fluentPut("id", ps.getReqId());
        if (isTest) { // Test mode
            bid.put("test", 1);
        }
        bid.put("at", 1);
        bid.put("tmax", 1000);

        if (bidderToken.adn == ADN_ADTIMING || bidderToken.adn == ADN_CROSSPROMOTION) {
            bid.put("user", new JSONObject().fluentPut("id", bidderToken.token));
        } else if (bidderToken.adn == ADN_FACEBOOK || bidderToken.adn == ADN_MINTEGRAL) {
            bid.put("user", new JSONObject().fluentPut("buyeruid", bidderToken.token));
        }

        JSONObject app = new JSONObject()
                .fluentPut("ver", ps.getAppv())
                .fluentPut("bundle", ps.getBundle());

        if (bidderToken.adn == ADN_FACEBOOK) {
            app.put("publisher", new JSONObject().fluentPut("id", bidderToken.appId));
            bid.put("ext", new JSONObject().fluentPut("platformid", bidderToken.appId));
        } else if (bidderToken.adn == ADN_MINTEGRAL) {
            int i = bidderToken.appId.indexOf('#');
            app.put("id", i == -1 ? bidderToken.appId : bidderToken.appId.substring(0, i));
        } else {
            app.put("id", bidderToken.appId);
        }
        bid.put("app", app);

        JSONObject device = new JSONObject()
                .fluentPut("ip", ps.getIp())
                .fluentPut("ifa", ps.getDid())
                .fluentPut("ua", ps.getUa())
                .fluentPut("make", ps.getMake())
                .fluentPut("model", ps.getModel())
                .fluentPut("os", ps.getPlat() == 0 ? "iOS" : "Android")
                .fluentPut("osv", ps.getOsv())
                .fluentPut("language", ps.getLang())
                .fluentPut("carrier", ps.getCarrier())
                .fluentPut("mccmnc", ps.getMccmnc())
                .fluentPut("connectiontype", ps.getContype());
        if (bidderToken.adn == ADN_MINTEGRAL) {
            device.fluentPut("w", ps.getWidth()).fluentPut("h", ps.getHeight());
        }
        bid.put("device", device);

        JSONObject imp = new JSONObject()
                .fluentPut("id", "1")
                .fluentPut("tagid", bidderToken.pkey);

        if (bidderToken.adn == ADN_VUNGLE) {
            imp.put("ext", Collections.singletonMap("vungle", Collections.singletonMap("bid_token", bidderToken.token)));
        }

        JSONObject size = new JSONObject()
                .fluentPut("w", ps.getWidth())
                .fluentPut("h", ps.getHeight());
        switch (placement.getAdType()) {
            case Banner:
                if (bidderToken.adn == ADN_FACEBOOK) {
                    size.put("w", -1);
                }
                imp.put("banner", size);
                break;
            case RewardVideo:
                if (bidderToken.adn == ADN_FACEBOOK) {
                    size.put("ext", new JSONObject().fluentPut("videotype", "rewarded"));
                    size.fluentPut("w", 0).fluentPut("h", 0);
                }
                imp.put("video", size);
                break;
            case Interstitial:
                if (bidderToken.adn == ADN_FACEBOOK) {
                    size.fluentPut("w", 0).fluentPut("h", 0);
                    imp.put("instl", 1);
                    imp.put("banner", size);
                } else {
                    imp.put("video", size);
                }
                break;
            case Native:
                if (bidderToken.adn == ADN_FACEBOOK) {
                    size.fluentPut("w", -1).fluentPut("h", -1);
                }
                imp.put("native", size);
                break;
            default:
                LOG.warn("unsupport adType for bid request");
                break;
        }
        bid.put("imp", Collections.singletonList(imp));

        Regs dRegs = ps.getRegs();
        if (dRegs != null) {
            JSONObject regs = new JSONObject();
            if (dRegs.getCoppa() != null) {
                regs.put("coppa", dRegs.getCoppa());
            }
            if (dRegs.getGdpr() != null) {
                ((JSONObject) regs.computeIfAbsent("ext", k -> new JSONObject())).put("gdpr", dRegs.getGdpr());
            }
            if (dRegs.getCcpa() != null) {
                ((JSONObject) regs.computeIfAbsent("ext", k -> new JSONObject())).put("ccpa", dRegs.getCcpa());
            }
            if (!regs.isEmpty()) {
                bid.put("regs", regs);
            }
        }

        return bid;
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<?> asyncExceptionHandler(HttpServletRequest req) throws IOException {
        WaterfallResponse res = (WaterfallResponse) req.getAttribute("res");
        LrRequest lr = (LrRequest) req.getAttribute("lr");
        WaterfallRequest o = (WaterfallRequest) req.getAttribute("params");
        if (res == null || lr == null || o == null) {
            return ResponseEntity.noContent().build();
        }
        Object insObject = req.getAttribute("insList");
        if (insObject != null) {
            //noinspection unchecked
            List<WaterfallInstance> insList = (List<WaterfallInstance>) insObject;
            if (!CollectionUtils.isEmpty(insList)) {
                res.setIns(getInsWithBidInstance(o, insList));
            }
        }
        lr.writeToLog(logService);
        if (res.getIns() == null || res.getIns().isEmpty()) {
            res.setCode(CODE_NOAVAILABLE_INSTANCE).setMsg("no available instance");
        }
        return response(res);
    }

}
