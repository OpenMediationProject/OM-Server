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
import com.adtiming.om.server.util.Compressor;
import com.adtiming.om.server.util.RandomUtil;
import com.adtiming.om.server.util.Util;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.adtiming.om.server.dto.WaterfallResponse.*;
import static java.nio.charset.StandardCharsets.UTF_8;

@Controller
public class WaterfallController extends WaterfallBase {

    private static final Logger LOG = LogManager.getLogger();

    private static final int ADN_ADTIMING = 1;
    private static final int ADN_FACEBOOK = 3;
    private static final int ADN_MINTEGRAL = 14;

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

    private RequestConfig bidReqCfg;
    private final Header bidReqUa = new BasicHeader("User-Agent", "om-bid-s2s");
    private final Header bidReqAe = new BasicHeader("Accept-Encoding", "gzip, deflate");

    @PostConstruct
    private void init() {
        RequestConfig.Builder cfgBuilder = RequestConfig.custom()
                .setConnectTimeout(2000)
                .setSocketTimeout(1000)
                .setRedirectsEnabled(false);
        String proxy = System.getProperty("bid.proxy");
        if (StringUtils.isNotBlank(proxy)) {
            cfgBuilder.setProxy(HttpHost.create(proxy));
        }
        bidReqCfg = cfgBuilder.build();
    }

    /**
     * waterfall
     */
    @PostMapping(value = "/wf", params = "v=1")
    @ResponseBody
    public Object wf(HttpServletRequest req, HttpServletResponse res,
                     @RequestParam("v") int version, // api version
                     @RequestParam("plat") int plat, // platform
                     @RequestParam("sdkv") String sdkv,
                     @RequestHeader("Host") String reqHost,
                     @RequestHeader(value = "User-Agent", required = false) String ua,
                     @RequestHeader(value = "debug", required = false) String debug,
                     @RequestBody byte[] data) throws IOException {
        WaterfallRequest o = fillRequestParams(data, version, sdkv, plat, ua, reqHost, req, geoService, cfg, cacheService);
        if (o == null) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }

        final boolean DEBUG = debug != null;
        final List<CharSequence> dmsg = DEBUG ? new DebugMsgs() : null;

        LrRequest lr = o.copyTo(new LrRequest());
        lr.setType(LrRequest.TYPE_WATERFALL_REQUEST);

        Placement placement = cacheService.getPlacement(o.getPid());
        if (placement == null) {
            response(res, new WaterfallResponse(CODE_PLACEMENT_INVALID, "placement invalid", CommonPB.ABTest.None_VALUE, dmsg));
            lr.setStatus(0, "placement invalid").writeToLog(logService);
            return null;
        }
        o.setPlacement(placement);
        lr.setPlacement(placement);

        o.setAdType(placement.getAdTypeValue());
//        o.setAbt(cacheService.getAbTestMode(placement.getId(), o));

        if (DEBUG) {
            dmsg.add(String.format("request ip:%s, country:%s", o.getIp(), o.getCountry()));
//            if (o.getAbt() > 0) {
//                dmsg.add("Placement ABTest status: On");
//                dmsg.add("Placement Device ABTest Mode:" + CommonPB.ABTest.forNumber(o.getAbt()));
//            }
        }
        Integer devDevicePubId = cacheService.getDevDevicePub(o.getDid());
        // dev device uses the configured abTest mode
        Integer devAbMode = null;
        if (devDevicePubId != null) {
            if (DEBUG) {
                Integer devAdnId = cacheService.getDevAppAdnId(placement.getPubAppId());
                dmsg.add("Is Dev Device,dev publisher:" + devDevicePubId);
                dmsg.add("Is Dev model,dev adnId:" + devAdnId);
            }
            devAbMode = cacheService.getDevDeviceAbtMode(o.getDid());
        }
        if (devAbMode != null) {
            o.setAbt(devAbMode);
            if (DEBUG) dmsg.add("Dev Device ABTest Mode:" + CommonPB.ABTest.forNumber(o.getAbt()));
        }

        PublisherApp pubApp = cacheService.getPublisherApp(placement.getPubAppId());
        if (pubApp == null) {
            response(res, new WaterfallResponse(CODE_PUB_APP_INVALID, "app invalid", o.getAbt(), dmsg));
            lr.setStatus(0, "app invalid").writeToLog(logService);
            return null;
        }

        if (StringUtils.isEmpty(o.getCountry())) {
            response(res, new WaterfallResponse(CODE_COUNTRY_NOT_FOUND, "country not found", o.getAbt(), dmsg));
            lr.setStatus(0, "country not found").writeToLog(logService);
            return null;
        }

        //placement target filter
        if (!matchPlacement(o, placement)) {
            response(res, new WaterfallResponse(CODE_PLACEMENT_INVALID, "placement not allowed", o.getAbt(), dmsg));
            lr.setStatus(0, "placement not allowed").writeToLog(logService);
            return null;
        }

        if (o.getBids2s() != null) {
            o.getBids2s().removeIf(bidderToken -> {
                Instance instance = cacheService.getInstanceById(bidderToken.iid);
                if (instance == null || !instance.isHeadBidding()) {
                    LOG.debug("instance not found or bid off, {}", bidderToken.iid);
                    return true;
                }
                AdNetworkPB.AdNetwork adn = cacheService.getAdNetwork(instance.getAdnId());
                if (adn == null || StringUtils.isBlank(adn.getBidEndpoint())) {
                    LOG.debug("adn not found or s2s bidding not support, {}", bidderToken.iid);
                    return true;
                }
                AdNetworkApp adnApp = cacheService.getAdnApp(instance.getPubAppId(), instance.getAdnId());
                if (adnApp == null) {
                    LOG.debug("adnApp not found, {}", bidderToken.iid);
                    return true;
                }
                bidderToken.adn = instance.getAdnId();
                bidderToken.pkey = instance.getPlacementKey();
                bidderToken.appId = adnApp.getAppKey();

                bidderToken.endpoint = adn.getBidEndpoint();
                if (bidderToken.adn == ADN_FACEBOOK) {
                    bidderToken.endpoint = bidderToken.endpoint.replace("${PLATFORM_ID}", bidderToken.appId);
                }
                return false;
            });
        }

        WaterfallResponse resp = new WaterfallResponse(0, null, o.getAbt(), dmsg);

        if (CollectionUtils.isEmpty(o.getBids2s())) {
            List<Integer> ins = getIns(devDevicePubId, o, placement, dmsg, DEBUG);
            if (ins == null || ins.isEmpty()) {
                resp.setCode(CODE_INSTANCE_EMPTY);
                resp.setMsg("instance empty");
                response(res, resp);
                lr.setStatus(0, resp.getMsg()).writeToLog(logService);
                return null;
            }

            resp.setIns(ins);
            response(res, resp);
            lr.setStatus(1, null).writeToLog(logService);
            return null;
        } else {
            // process s2s bid
            // setAttribute 用于 #asyncExceptionHandler
            req.setAttribute("resp", resp);
            req.setAttribute("lr", lr);
            req.setAttribute("params", o);

            boolean isTest = devDevicePubId != null;
            return bid(o, isTest, placement, resp, dr -> {
                try {
                    if (dr.isSetOrExpired()) {
                        LOG.warn("dr isSetOrExpired, ignore");
                        return;
                    }

                    List<Integer> ins = getIns(devDevicePubId, o, placement, dmsg, DEBUG);
                    if (ins == null || ins.isEmpty()) {
                        resp.setCode(CODE_INSTANCE_EMPTY);
                        resp.setMsg("instance not found");
                        dr.setResult(resp);
                        lr.setStatus(0, "instance not found").writeToLog(logService);
                        return;
                    }

                    resp.setIns(ins);
                    lr.setStatus(1, null).writeToLog(logService);

                    ResponseEntity.BodyBuilder b = ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON_UTF8);
                    byte[] respData = objectMapper.writeValueAsBytes(resp);
                    if (respData.length > 200) {
                        b.header(HTTP.CONTENT_ENCODING, "gzip");
                        respData = Compressor.gzip(respData);
                    }
                    dr.setResult(b.contentLength(respData.length).body(respData));
                } catch (IllegalArgumentException | IllegalStateException e) {
                    LOG.warn("set dr result error, {}", e.toString());
                } catch (Exception e) {
                    LOG.error("set dr result error", e);
                    dr.setResult(ResponseEntity.noContent().build());
                }
            });

        }

    }

    List<Integer> getIns(Integer devDevicePubId, WaterfallRequest o, Placement p,
                         List<CharSequence> dmsg, boolean DEBUG) throws IOException {
        //dev mode
        List<Integer> devIns = matchDev(devDevicePubId, p, cacheService);
        if (devIns != null && !devIns.isEmpty()) {
            return devIns;
        }

        List<InstanceRule> rules = cacheService.getCountryRules(p.getId(), o.getCountry());
        InstanceRule matchedRule = getMatchedRule(rules, o);

        if (DEBUG) {
            if (matchedRule != null) {
                dmsg.add("hit rule:" + matchedRule.getId());
            } else {
                dmsg.add("miss rule");
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
        List<Integer> sortIns;
        Map<Integer, Integer> insAdnIdMap = new HashMap<>(pmInstances.size());
        if (matchedRule != null) {//has rule
            Set<Integer> insIdSet = matchedRule.getInstanceList();
            if (DEBUG) {
                dmsg.add("Rule instance list:" + objectMapper.writeValueAsString(insIdSet));
            }
            if (insIdSet.isEmpty()) {
                return null;
            }
            if (matchedRule.isAutoOpt()) {//auto waterfall, ecpm priority
                Map<Integer, Float> insEcpm = new HashMap<>(insIdSet.size());
                float totalEcpm = 0;
                Map<Float, List<Integer>> priorityIns = new HashMap<>(insIdSet.size());
                for (Instance ins : pmInstances) {
                    if (!insIdSet.contains(ins.getId())) {
                        continue;
                    }
                    if (blockAdnIds.contains(ins.getAdnId()) || !ins.matchInstance(o)) {
                        continue;
                    }
                    if (ins.isHeadBidding() && o.getBidPrice(ins.getId()) == null) {
                        // Remove bid instance of nobid
                        continue;
                    }
                    insAdnIdMap.put(ins.getId(), ins.getAdnId());
                    totalEcpm += setInstanceEcpm(o, ins, priorityIns, insEcpm, totalEcpm, DEBUG, dmsg, p.getAdTypeValue(), cacheService);
                }
                if (priorityIns.isEmpty()) {
                    if (DEBUG) {
                        dmsg.add("Has no instance match");
                    }
                    return null;
                }
                if (priorityIns.containsKey(-1F)) {
                    float avgEcpm = totalEcpm / insEcpm.size();//avg ecpm
                    List<Integer> needAvgEcpmIns = priorityIns.get(-1F);
                    if (DEBUG) {
                        dmsg.add(String.format("Rule auto optimize,Instance AvgEcpm:%f,UseAvgEcpm:%s", avgEcpm, objectMapper.writeValueAsString(needAvgEcpmIns)));
                    }
                    priorityIns.put(avgEcpm, needAvgEcpmIns);
                    priorityIns.remove(-1F);
                }
                sortIns = Util.sortByEcpm(priorityIns);
                if (DEBUG) {
                    dmsg.add("Instance Ecpm:" + objectMapper.writeValueAsString(insEcpm));
                    dmsg.add("Instance sort:" + objectMapper.writeValueAsString(sortIns));
                }
            } else { // Non-auto sort by rule configuration
                Map<Integer, Float> instanceEcpm = null;
                if (!o.getBidPriceMap().isEmpty())
                    instanceEcpm = new HashMap<>(insIdSet.size());

                Map<Integer, Integer> insConfigWeight = matchedRule.getInstanceWeightMap();
                Map<Integer, Integer> insWeight = new HashMap<>(insIdSet.size());
                for (Instance ins : pmInstances) {
                    if (!insIdSet.contains(ins.getId())) {
                        continue;
                    }
                    if (blockAdnIds.contains(ins.getAdnId()) || !ins.matchInstance(o)) {
                        continue;
                    }
                    if (ins.isHeadBidding() && o.getBidPrice(ins.getId()) == null) {
                        // Remove bid instance of nobid
                        continue;
                    }
                    insAdnIdMap.put(ins.getId(), ins.getAdnId());

                    if (instanceEcpm != null) {
                        if (ins.isHeadBidding()) {
                            continue; // HeadBidding does not participate in manual sorting
                        }
                        InstanceEcpm ecpm = cacheService.getInstanceEcpm(ins.getAdnId(), ins.getId(), o.getCountry(), p.getAdTypeValue());
                        if (ecpm != null && ecpm.ecpm > -1F) {
                            instanceEcpm.put(ins.getId(), ecpm.ecpm);
                        }
                    }

                    int weight = insConfigWeight.get(ins.getId());
                    if (weight > 0) {
                        insWeight.put(ins.getId(), weight);
                    }
                }
                if (insAdnIdMap.isEmpty()) {
                    if (DEBUG) {
                        dmsg.add("Has no instance match");
                    }
                    return null;
                }

                // Sorting type, 0: use weight; 1: use absolute priority
                if (matchedRule.getSortType() == 0) {
                    sortIns = RandomUtil.sortByWeight(insWeight);
                } else {
                    // When absolute priority, an instance is randomly selected with the same weight
                    sortIns = Util.sortByPriority(insWeight);
                }

                if (DEBUG) {
                    String sortName = (matchedRule.getSortType() == 1 ? "Priority" : "Weight");
                    dmsg.add(String.format("Sort type:%s, %s:%s", sortName, sortName, objectMapper.writeValueAsString((insWeight))));
                    dmsg.add("Instance sort:" + objectMapper.writeValueAsString(sortIns));
                }

                if (instanceEcpm != null) {// Insert HeadBidding Instance
                    if (!sortIns.isEmpty()) {// There are instances other than headerbidding
                        sortIns = new ArrayList<>(sortIns);
                        List<Integer> finalSortIns = sortIns;
                        Map<Integer, Float> finalInstanceEcpm = instanceEcpm;
                        if (DEBUG && !o.getBidPriceMap().isEmpty()) {
                            dmsg.add(String.format("instance bid info:%s", objectMapper.writeValueAsString(o.getBidPriceMap())));
                            dmsg.add(String.format("instance ecpm info:%s", objectMapper.writeValueAsString(instanceEcpm)));
                        }
                        o.getBidPriceMap().forEach((iid, bidPrice) -> {
                            for (int i = 0; i < finalSortIns.size(); i++) {
                                Float ecpm = finalInstanceEcpm.getOrDefault(finalSortIns.get(i), 0f);
                                if (bidPrice > ecpm) {
                                    finalSortIns.add(i, iid);
                                    break;
                                }
                            }
                        });
                    } else {
                        // Only configured with headerbidding instances
                        sortIns = Util.sortByPrice(o.getBidPriceMap());
                    }
                    if (DEBUG) {
                        dmsg.add("Instance finally sort:" + objectMapper.writeValueAsString(sortIns));
                    }
                }
            }
        } else {// Rule is not configured by ecpm absolute priority ordering
            Map<Integer, Float> insEcpm = new HashMap<>(pmInstances.size());
            float totalEcpm = 0;
            Map<Float, List<Integer>> priorityIns = new HashMap<>(pmInstances.size());
            for (Instance ins : pmInstances) {
                if (blockAdnIds.contains(ins.getAdnId()) || !ins.matchInstance(o)) {
                    continue;
                }
                if (ins.isHeadBidding() && o.getBidPrice(ins.getId()) == null) {
                    // Remove bid instance of nobid
                    continue;
                }
                insAdnIdMap.put(ins.getId(), ins.getAdnId());

                totalEcpm += setInstanceEcpm(o, ins, priorityIns, insEcpm, totalEcpm, DEBUG, dmsg,
                        p.getAdTypeValue(), cacheService);
            }
            if (priorityIns.isEmpty()) {
                if (DEBUG) {
                    dmsg.add("Has no instance match");
                }
                return null;
            }
            if (priorityIns.containsKey(-1F)) {//No ecpm use average ecpm
                float avgEcpm = totalEcpm / insEcpm.size();
                List<Integer> needAvgEcpmIns = priorityIns.get(-1F);
                if (DEBUG) {
                    dmsg.add(String.format("No rule auto optimize,Instance AvgEcpm:%f,UseAvgEcpm:%s", avgEcpm, objectMapper.writeValueAsString(needAvgEcpmIns)));
                }
                priorityIns.put(avgEcpm, needAvgEcpmIns);
                priorityIns.remove(-1F);
            }
            sortIns = Util.sortByEcpm(priorityIns);
            if (DEBUG) {
                dmsg.add("Instance Ecpm:" + objectMapper.writeValueAsString(insEcpm));
                dmsg.add("Instance sort:" + objectMapper.writeValueAsString(sortIns));
            }
        }
        return sortIns;
    }

    private float setInstanceEcpm(WaterfallRequest o, Instance ins, Map<Float, List<Integer>> priorityIns,
                                  Map<Integer, Float> insEcpm, float totalEcpm, boolean DEBUG, List<CharSequence> dmsg,
                                  int adType, CacheService cacheService) {
        Float bidPrice = o.getBidPrice(ins.getId());
        if (bidPrice != null) {
            priorityIns.computeIfAbsent(bidPrice, k -> new ArrayList<>()).add(ins.getId());
            insEcpm.put(ins.getId(), bidPrice);
            totalEcpm += bidPrice;
            if (DEBUG) {
                dmsg.add("Instance: " + ins.getId() + ", BidPrice:" + bidPrice);
            }
        } else {
            InstanceEcpm ecpm = cacheService.getInstanceEcpm(ins.getAdnId(), ins.getId(), o.getCountry(), adType);
            priorityIns.computeIfAbsent(ecpm.ecpm, k -> new ArrayList<>()).add(ins.getId());
            if (ecpm.ecpm > -1F) {
                insEcpm.put(ins.getId(), ecpm.ecpm);
            }
            totalEcpm += ecpm.ecpm;
            if (DEBUG) {
                try {
                    dmsg.add("Instance Ecpm:" + objectMapper.writeValueAsString(ecpm));
                } catch (JsonProcessingException e) {
                    LOG.error("error", e);
                }
            }
        }
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
            }

            String reqData = buildBidReqData(o, isTest, placement, bidderToken).toJSONString();
            bidreq.setEntity(new StringEntity(reqData, ContentType.APPLICATION_JSON));

            if (DEBUG) {
                resp.getDebug().add(String.format("bidreq to %d, %s, bid: %s", bidderToken.iid, bidderToken.endpoint, reqData));
            }

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
                            LOG.debug("adn {} nobid: {}, err: {}", bidderToken.adn, sl, err);
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
                    handleS2SBidResponse(count, bidderToken, isTest, bidresp, err, o, resp, placement, callback, dr);
                }

                @Override
                public void failed(Exception ex) {
                    String msg = ex.toString();
                    LOG.debug("adn {} failed: {}", bidderToken.adn, msg);
                    if (DEBUG) {
                        resp.getDebug().add(String.format("bidresp from %d failed: %s", bidderToken.iid, ex.toString()));
                    }
                    handleS2SBidResponse(count, bidderToken, isTest, null, msg, o, resp, placement, callback, dr);
                }

                @Override
                public void cancelled() {
                    handleS2SBidResponse(count, bidderToken, isTest, null, "cancelled", o, resp, placement, callback, dr);
                }
            });

            // write bid request log
            LrRequest lr = o.copyTo(new LrRequest());
            lr.setType(EventLogRequest.INSTANCE_BID_REQUEST);
            lr.setMid(bidderToken.adn);
            lr.setPlacement(placement);
            lr.setIid(bidderToken.iid);
            lr.writeToLog(logService);
        }
        return dr;
    }

    private void handleS2SBidResponse(
            AtomicInteger count, WaterfallRequest.S2SBidderToken bidderToken, boolean isTest,
            JSONObject bidresp, String err, WaterfallRequest o, WaterfallResponse resp, Placement placement,
            S2SBidCallback callback, DeferredResult<Object> dr) {
        try {
            WaterfallResponse.S2SBidResponse bres = new WaterfallResponse.S2SBidResponse();
            bres.iid = bidderToken.iid;
            bres.err = err;
            resp.bidresp.add(bres);
            if (bidresp != null) {
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
                bres.price = price;
                bres.adm = bid.getString("adm");
                bres.nurl = bid.getString("nurl");
                bres.lurl = bid.getString("lurl");
                o.setBidPrice(bidderToken.iid, price);

                // write bid response log
                LrRequest lr = o.copyTo(new LrRequest());
                lr.setType(EventLogRequest.INSTANCE_BID_RESPONSE);
                lr.setMid(bidderToken.adn);
                lr.setPlacement(placement);
                lr.setIid(bidderToken.iid);
                if (!isTest) {
                    lr.setPrice(price);
                }
                lr.writeToLog(logService);
            }
        } catch (Exception e) {
            LOG.error("set s2s bidresp error", e);
        } finally {
            if (count.decrementAndGet() == 0) {
                callback.cb(dr);
            }
        }
    }

    private JSONObject buildBidReqData(WaterfallRequest ps, boolean isTest, Placement placement, WaterfallRequest.S2SBidderToken bidderToken) {
        JSONObject bid = new JSONObject().fluentPut("id", ps.getReqId());
        if (isTest) { // Test mode
            bid.put("test", 1);
        }
        bid.put("at", 1);
        bid.put("tmax", 1000);

        if (bidderToken.adn == ADN_ADTIMING) {
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
    @ResponseBody
    public Object asyncExceptionHandler(HttpServletRequest req, HttpServletResponse res) throws IOException {
        WaterfallResponse resp = (WaterfallResponse) req.getAttribute("resp");
        LrRequest lr = (LrRequest) req.getAttribute("lr");
        if (resp == null || lr == null) {
            return ResponseEntity.noContent();
        }
        lr.writeToLog(logService);
        if (resp.getIns() == null || resp.getIns().isEmpty()) {
            resp.setCode(CODE_INSTANCE_EMPTY);
        }
        response(res, resp);
        return null;
    }

}
