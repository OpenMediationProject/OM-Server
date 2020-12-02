package com.adtiming.om.server.web;

import com.adtiming.om.pb.CommonPB;
import com.adtiming.om.server.dto.*;
import com.adtiming.om.server.service.AppConfig;
import com.adtiming.om.server.service.CacheService;
import com.adtiming.om.server.service.GeoService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static com.adtiming.om.server.dto.WaterfallResponse.*;

/**
 * This interface has expired, use /wf interface directly
 */
@Deprecated
@RestController
public class HBController extends WaterfallBase {

    @Resource
    private AppConfig cfg;

    @Resource
    private GeoService geoService;

    @Resource
    private CacheService cacheService;

//    @Resource
//    private LogService logService;

    /**
     * Get the instance collection with header bidding turned on
     * Return the instances where you need to request a header bidding
     */
    @Deprecated
    @PostMapping(value = "/hb", params = "v=1")
    public ResponseEntity<?> getHBInstances(
            HttpServletRequest req,
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

//        LrRequest lr = o.copyTo(new LrRequest());
//        lr.setType(LrRequest.TYPE_HB_REQUEST);
        WaterfallResponse resp = new WaterfallResponse(o.getAbt(), debug != null);
        resp.addDebug("request ip:%s, country:%s", o.getIp(), o.getCountry());

        Placement placement = cacheService.getPlacement(o.getPid());
        if (placement == null) {
//            lr.setStatus(0, "placement invalid").writeToLog(logService);
            resp.setCode(CODE_PLACEMENT_INVALID).setMsg("placement invalid").setAbt(CommonPB.ABTest.None_VALUE);
            return response(resp);
        }
        o.setPlacement(placement);
//        lr.setPlacement(placement);

        o.setAdType(placement.getAdTypeValue());
//        o.setAbt(cacheService.getAbTestMode(placement.getId(), o));


//        if (DEBUG) {
//            if (o.getAbt() > 0) {
//                dmsg.add("Placement ABTest status: On");
//                dmsg.add("Placement Device ABTest Mode:" + CommonPB.ABTest.forNumber(o.getAbt()));
//            }
//        }
        Integer devDevicePubId = cacheService.getDevDevicePub(o.getDid());
        // dev device uses the configured abTest mode
        Integer devAbMode = null;
        if (devDevicePubId != null) {
            if (resp.isDebugEnabled()) {
                Integer devAppAdnId = cacheService.getDevAppAdnId(placement.getPubAppId());
                resp.addDebug("Is Dev Device,dev publisher: %d", devDevicePubId);
                resp.addDebug("Is Dev model,Dev Mediation: %d", devAppAdnId);
            }
            devAbMode = cacheService.getDevDeviceAbtMode(o.getDid());
        }
        if (devAbMode != null) {
            o.setAbt(devAbMode);
            resp.setAbt(devAbMode);
            resp.addDebug("Dev Device ABTest Mode: %s", CommonPB.ABTest.forNumber(o.getAbt()));
        }

        PublisherApp pubApp = cacheService.getPublisherApp(placement.getPubAppId());
        if (pubApp == null) {
            resp.setCode(CODE_PUB_APP_INVALID).setMsg("app invalid").setAbt(o.getAbt());
            return response(resp);
//            lr.setStatus(0, "app invalid").writeToLog(logService);
        }

        if (StringUtils.isEmpty(o.getCountry())) {
            resp.setCode(CODE_COUNTRY_NOT_FOUND).setMsg("country not found").setAbt(o.getAbt());
            return response(resp);
//            lr.setStatus(0, "country not found").writeToLog(logService);
        }

        List<Integer> ins = getIns(devDevicePubId, o, placement, resp);
        resp.setIns(ins);
        return response(resp);
//        lr.setStatus(1, null).writeToLog(logService);
    }

    private List<Integer> getIns(Integer devDevicePubId, WaterfallRequest o, Placement p,
                                 WaterfallResponse resp) {
        //dev mode
        List<Instance> devIns = matchDev(devDevicePubId, p, cacheService);
        if (devIns != null && !devIns.isEmpty()) {
            List<Integer> hbList = new ArrayList<>();
            for (Instance ins : devIns) {
                hbList.add(ins.getId());
            }
            if (hbList.size() > 1) {// Randomly return one in dev mode
                return Collections.singletonList(hbList.get(ThreadLocalRandom.current().nextInt(hbList.size())));
            }
            return hbList;
        }
        //placement target filter
        if (!matchPlacement(o, p)) {
            return null;
        }

        List<InstanceRule> rules = cacheService.getCountryRules(p.getId(), o.getCountry());
        InstanceRule matchedRule = getMatchedRule(rules, o);

        if (resp.isDebugEnabled()) {
            if (matchedRule != null) {
                resp.addDebug("hit rule: %d", matchedRule.getId());
            } else {
                resp.addDebug("miss rule");
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
        List<Integer> hbList = null;
        for (Instance ins : pmInstances) {
            if (blockAdnIds.contains(ins.getAdnId()) || !ins.isHeadBidding() || !ins.matchInstance(o)
                    || (matchedRule != null && !matchedRule.getInstanceList().contains(ins.getId()))) {
                continue;
            }
            if (hbList == null) {
                hbList = new ArrayList<>();
            }
            hbList.add(ins.getId());
        }
        return hbList;
    }
}
