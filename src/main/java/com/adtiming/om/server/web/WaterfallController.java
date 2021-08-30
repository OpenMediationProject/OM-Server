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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.adtiming.om.server.dto.AdNetwork.ADN_CROSSPROMOTION;
import static com.adtiming.om.server.dto.AdNetwork.ADN_FACEBOOK;
import static com.adtiming.om.server.dto.WaterfallResponse.*;

@RestController
public class WaterfallController extends WaterfallBase {

    private static final Logger LOG = LogManager.getLogger();

    @Resource
    private AppConfig cfg;

    @Resource
    private GeoService geoService;

    @Resource
    private CacheService cacheService;

    @Resource
    private LogService logService;

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
            /*lr.setStatus(0, "country not found").writeToLog(logService);
            res.setCode(CODE_COUNTRY_NOT_FOUND).setMsg("country not found");
            return response(res);*/
            o.setCountry("00");//Set All when the country is not fetched
        }

        //placement target filter
        if (matchPlacement(o, placement)) {
            lr.setStatus(0, "placement not allowed").writeToLog(logService);
            res.setCode(CODE_PLACEMENT_INVALID).setMsg("placement not allowed");
            return response(res);
        }
        InstanceRule rule = cacheService.matchPlacementRule(placement.getId(), o.getCountry(), o);
        if (rule != null) {
            lr.setRuleId(rule.getId());
            lr.setRuleType(rule.getSortType());
            lr.setRp(rule.getPriority());
        }

        Map<Integer, Instance> bidInsMap = new HashMap<>();
        List<WaterfallInstance> insList = getIns(devDevicePubId, o, placement, res, bidInsMap, rule);

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
                if (adn.getBidType() != 1) {//Non-s2s adn
                    LOG.debug("adn not support s2s, {}", bidderToken.iid);
                    res.addDebug(String.format("adn not support s2s, remove adn:%d, instance: %d", adn.getId(), bidderToken.iid));
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
            return bid(o, lr, isTest, placement, res, dr -> {
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
                    if (CollectionUtils.isEmpty(ins)) {
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

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<?> asyncExceptionHandler(HttpServletRequest req) throws IOException {
        return super.asyncExceptionHandler(req);
    }

}
