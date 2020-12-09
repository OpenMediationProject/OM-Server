// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.cp.web;

import com.adtiming.om.pb.CommonPB;
import com.adtiming.om.server.cp.dto.*;
import com.adtiming.om.server.dto.*;
import com.adtiming.om.server.service.AppConfig;
import com.adtiming.om.server.service.GeoService;
import com.adtiming.om.server.service.LogService;
import com.adtiming.om.server.util.Util;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.adtiming.om.pb.CommonPB.AdType.Banner;
import static com.adtiming.om.server.dto.AdNetwork.ADN_CROSSPROMOTION;

@RestController
public class CLController extends AdBaseController {

    @Resource
    private AppConfig appConfig;

    @Resource
    private GeoService geoService;

    @Resource
    private LogService logService;

    /**
     * get campaigns
     */
    @PostMapping(value = "/cp/cl", params = "v=1")
    public ResponseEntity<?> cl(
            HttpServletRequest req,
            @RequestHeader("Host") String reqHost,
            @RequestParam("v") int apiv,        // api version
            @RequestParam("plat") int plat,     // platform
            @RequestParam("sdkv") String sdkv,  // sdk version
            @RequestHeader(value = "User-Agent", required = false) String ua,
            @RequestHeader(value = "x-om-debug", required = false) String debugMark) throws IOException {
        CLRequest o = parseBody(req, CLRequest.class);
        o.setApiv(apiv);
        o.setSdkv(sdkv);
        o.setPlat(plat);
        o.setReqHost(reqHost);
        o.setUa(ua);
        o.setAppConfig(appConfig);
        o.setMtype(Util.getModelType(plat, o.getModel(), ua));
        o.setGeo(geoService.getGeoData(req, o));

        final boolean DEBUG = debugMark != null;
        CLResponse resp = new CLResponse(DEBUG);

        if (!DEBUG) {
            o.setCountry(o.getGeo().getCountry());
        }

        Placement placement = cacheService.getPlacement(o.getPid());
        if (placement == null) {
            resp.setCode(NoFillReason.PLACEMENT_INVALID).addDebug("placement invalid");
            return ResponseEntity.badRequest().body(resp);
        }
        o.setPlacement(placement);

        PublisherApp pubApp = cacheService.getPublisherApp(placement.getPubAppId());
        if (pubApp == null) {
            resp.setCode(NoFillReason.PUB_APP_INVALID).addDebug("pubApp invalid");
            return ResponseEntity.badRequest().body(resp);
        }
        o.setPubApp(pubApp);

        LrRequest lr = o.copyTo(new LrRequest());
        lr.setWfReq(1);
        lr.setInsReq(1);
        lr.setMid(ADN_CROSSPROMOTION);
        lr.setAdType(placement.getAdTypeValue());

        if (o.getIid() == 0) {
            List<Instance> cpInstances = cacheService.getPlacementAdnInstances(o.getPid(), ADN_CROSSPROMOTION);
            if (cpInstances != null && !cpInstances.isEmpty()) {
                int iid = cpInstances.get(0).getId();
                o.setIid(iid);
                lr.setIid(iid);
            }
        }

        resp.addDebug("request ip:%s, country:%s, ng:%s", o.getIp(), o.getCountry(), o.getNg());

        Integer devDevicePubId = cacheService.getDevDevicePub(o.getDid());
        if (devDevicePubId != null && (devDevicePubId == 0 || devDevicePubId == placement.getPublisherId())) {
            o.setTest(true);
        }

        if (!o.isTest() && o.getRegs() != null && o.getRegs().hasAny()) {
            lr.setStatus(0, "regs deny").writeToLog(logService);
            return ResponseEntity.ok(resp.setCode(NoFillReason.REGS_DENY).addDebug("regs deny"));
        }

        o.setRequireSkAdNetwork(o.getPlat() == CommonPB.Plat.iOS_VALUE
                && o.isEmptyDid()
                && o.getOsVersion().ge(Version.OSV_IOS_14_0_0));

        List<MatchedCampaign> matchedCampaigns = matchCampaigns(o, resp);
        boolean noCampaign = matchedCampaigns == null || matchedCampaigns.isEmpty();

        if (noCampaign) {
            lr.setStatus(0, "nofill").writeToLog(logService);
        } else {
            int cacheSize = placement.getAdType() == Banner ? 1 : placement.getInventoryCount();
            List<CampaignResp> campaigns = new ArrayList<>(cacheSize);
            for (int i = 0, j = Math.min(cacheSize, matchedCampaigns.size()); i < j; ++i) {
                campaigns.add(new CampaignResp(o, matchedCampaigns.get(i), cpCacheService));
            }
            resp.setCampaigns(campaigns);

            lr.setWfFil(1);
            lr.setInsReq(campaigns.size());
            lr.setInsFil(campaigns.size());
            lr.setStatus(1, null).writeToLog(logService);
        }

        return ResponseEntity.ok(resp);
    }

}
