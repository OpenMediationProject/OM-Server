// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.cp.web;

import com.adtiming.om.pb.CommonPB;
import com.adtiming.om.pb.CrossPromotionPB;
import com.adtiming.om.server.cp.dto.*;
import com.adtiming.om.server.cp.service.CpCacheService;
import com.adtiming.om.server.dto.Placement;
import com.adtiming.om.server.dto.PublisherApp;
import com.adtiming.om.server.dto.Version;
import com.adtiming.om.server.service.AppConfig;
import com.adtiming.om.server.service.CacheService;
import com.adtiming.om.server.service.GeoService;
import com.adtiming.om.server.web.BaseController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;

@RestController
public class PayloadController extends BaseController {

    private static final Logger LOG = LogManager.getLogger();

    @Resource
    private AppConfig appConfig;

    @Resource
    private CacheService cacheService;

    @Resource
    private CpCacheService cpCacheService;

    @Resource
    private GeoService geoService;

    /**
     * Payload Request
     */
    @PostMapping(value = "/cp/pl", params = "v=1")
    public ResponseEntity<?> payload(
            HttpServletRequest req,
            @RequestHeader("Host") String reqHost,
            @RequestParam("v") int apiv,
            @RequestParam("plat") int plat, // platform
            @RequestParam("sdkv") String sdkv,
            @RequestHeader(value = "User-Agent", required = false) String ua) throws IOException {
        BidPayloadRequest o = parseBody(req, BidPayloadRequest.class);
        o.setApiv(apiv);
        o.setSdkv(sdkv);
        o.setPlat(plat);
        o.setReqHost(reqHost);
        o.setUa(ua);
        o.setAppConfig(appConfig);
        o.setGeo(geoService.getGeoData(req, o));

        BidPayloadToken pl = o.getPayload();
        if (pl.isSignError()) {
            LOG.warn("/cp/pl toekn sign error");
            return ResponseEntity.badRequest().body("toekn sign error");
        }

        Placement placement = cacheService.getPlacement(o.getPid());
        if (placement == null) {
            LOG.warn("/cp/pl placement not found");
            return ResponseEntity.badRequest().body("placement not found");
        }
        o.setPlacement(placement);

        PublisherApp pubApp = cacheService.getPublisherApp(placement.getPubAppId());
        if (pubApp == null) {
            LOG.warn("/cp/pl pubApp not found");
            return ResponseEntity.badRequest().body("pubApp not found");
        }
        o.setPubApp(pubApp);

        Campaign c = cpCacheService.getCampaign(pl.getCid());
        MatchedCreative mcr = new MatchedCreative();
        mcr.creative = cpCacheService.getCreative(pl.getCrid());
        if (pl.getIcon() != null) {
            mcr.materialIcon = cpCacheService.getMaterial(pl.getIcon());
        }
        if (!CollectionUtils.isEmpty(pl.getImg())) {
            for (Long id : pl.getImg()) {
                mcr.addImg(cpCacheService.getMaterial(id));
            }
        }
        if (pl.getVd() != null) {
            mcr.materialVideo = cpCacheService.getMaterial(pl.getVd());
        }
        if (pl.getVdt() != null) {
            mcr.template = cpCacheService.getH5Template(pl.getVdt());
        }
        if (pl.getEct() != null) {
            mcr.endcardTemplate = cpCacheService.getH5Template(pl.getEct());
        }
        final CrossPromotionPB.App capp = cpCacheService.getApp(c.getAppId());

        o.setRequireSkAdNetwork(o.getPlat() == CommonPB.Plat.iOS_VALUE
                && o.isEmptyDid()
                && o.getOsVersion().ge(Version.OSV_IOS_14_0_0));
        MatchedCampaign mc = new MatchedCampaign(c, capp, mcr, pl.getPrice());
        CampaignResp campaignResp = new CampaignResp(o, mc, cpCacheService);
        CLResponse resp = new CLResponse(false)
                .setCampaigns(Collections.singletonList(campaignResp));

        return response(resp);
    }


}
