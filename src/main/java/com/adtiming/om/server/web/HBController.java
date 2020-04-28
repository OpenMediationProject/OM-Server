package com.adtiming.om.server.web;

import com.adtiming.om.pb.CommonPB;
import com.adtiming.om.server.dto.*;
import com.adtiming.om.server.service.AppConfig;
import com.adtiming.om.server.service.CacheService;
import com.adtiming.om.server.service.GeoService;
import com.adtiming.om.server.service.LogService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static com.adtiming.om.server.dto.WaterfallResponse.*;

@Controller
public class HBController extends WaterfallBase {

    @Resource
    private AppConfig cfg;

    @Resource
    private GeoService geoService;

    @Resource
    private CacheService cacheService;

    @Resource
    private LogService logService;

    /**
     * Get the instance collection with header bidding turned on
     * Return the instances where you need to request a header bidding
     */
    @PostMapping(value = "/hb", params = "v=1")
    public void getHBInstances(HttpServletRequest req, HttpServletResponse res,
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
            return;
        }

        final boolean DEBUG = debug != null;
        final List<CharSequence> dmsg = DEBUG ? new WaterfallController.DebugMsgs() : null;

        LrRequest lr = o.copyTo(new LrRequest());
        lr.setType(LrRequest.TYPE_HB_REQUEST);

        Placement placement = cacheService.getPlacement(o.getPid());
        if (placement == null) {
            response(res, new WaterfallResponse(CODE_PLACEMENT_INVALID, "placement invalid", CommonPB.ABTest.None_VALUE, dmsg));
            lr.setStatus(0, "placement invalid").writeToLog(logService);
            return;
        }
        o.setPlacement(placement);
        lr.setPlacement(placement);

        o.setAdType(placement.getAdTypeValue());
        o.setAbt(cacheService.getAbTestMode(placement.getId(), o));

        if (DEBUG) {
            dmsg.add(String.format("request ip:%s, country:%s", o.getIp(), o.getCountry()));
            if (o.getAbt() > 0) {
                dmsg.add("Placement ABTest status: On");
                dmsg.add("Placement Device ABTest Mode:" + CommonPB.ABTest.forNumber(o.getAbt()));
            }
        }
        Integer devDevicePubId = cacheService.getDevDevicePub(o.getDid());
        // dev device uses the configured abTest mode
        Integer devAbMode = null;
        if (devDevicePubId != null) {
            if (DEBUG) {
                Integer devAppAdnId = cacheService.getDevAppAdnId(placement.getPubAppId());
                dmsg.add("Is Dev Device,dev publisher:" + devDevicePubId);
                dmsg.add("Is Dev model,Dev Mediation:" + devAppAdnId);
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
            return;
        }

        if (StringUtils.isEmpty(o.getCountry())) {
            response(res, new WaterfallResponse(CODE_COUNTRY_NOT_FOUND, "country not found", o.getAbt(), dmsg));
            lr.setStatus(0, "country not found").writeToLog(logService);
            return;
        }

        List<Integer> ins = getIns(devDevicePubId, o, placement, dmsg, DEBUG);

        WaterfallResponse resp = new WaterfallResponse(0, null, o.getAbt(), dmsg);
        resp.setIns(ins);
        response(res, resp);
        lr.setStatus(1, null).writeToLog(logService);
    }

    private List<Integer> getIns(Integer devDevicePubId, WaterfallRequest o, Placement p,
                                 List<CharSequence> dmsg, boolean DEBUG) {
        //dev mode
        List<Integer> devIns = matchDev(devDevicePubId, p, cacheService);
        if (devIns != null && !devIns.isEmpty()) {
            List<Integer> hbList = null;
            for (int iid : devIns) {
                Instance ins = cacheService.getInstanceById(iid);
                if (ins == null) continue;
                if (hbList == null)
                    hbList = new ArrayList<>();
                hbList.add(iid);
            }
            if (hbList != null && hbList.size() > 1) {// Randomly return one in dev mode
                return Collections.singletonList(hbList.get(ThreadLocalRandom.current().nextInt(hbList.size())));
            }
            return hbList;
        }
        //placement target filter
        if (!matchPlacement(o, p)) {
            return null;
        }

        List<InstanceRule> rules = cacheService.getCountryRules(p.getId(), o.getCountry());
        InstanceRule matchedRule = getMatchedRule(cacheService, rules, o, DEBUG, dmsg);

        int segmentId = matchedRule != null ? matchedRule.getSegmentId() : 0;
        if (DEBUG) {
            if (matchedRule != null) {
                dmsg.add("hit rule:" + matchedRule.getId());
            } else {
                dmsg.add("miss rule");
            }
            dmsg.add("hit segment:" + segmentId);
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
