package com.adtiming.om.server.cp.web;

import com.adtiming.om.pb.CommonPB;
import com.adtiming.om.server.cp.dto.*;
import com.adtiming.om.server.dto.Placement;
import com.adtiming.om.server.dto.PublisherApp;
import com.adtiming.om.server.dto.Regs;
import com.adtiming.om.server.dto.Version;
import com.adtiming.om.server.service.AppConfig;
import com.adtiming.om.server.service.GeoService;
import com.adtiming.om.server.util.Util;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.client.entity.DeflateInputStream;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

@RestController
public class BidController extends AdBaseController {

    @Resource
    private AppConfig appConfig;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private GeoService geoService;

    /**
     * Cross Promotion OpenRTB BidRequest
     */
    @PostMapping("/cp/bid/v1")
    public ResponseEntity<?> bid(
            HttpServletRequest req,
            @RequestHeader("Host") String reqHost,
            @RequestHeader(value = "x-om-debug", required = false) String debugMark) throws IOException {
        BidRequest bid = parseBody(req, BidRequest.class);
        CLRequest o;
        try {
            o = new CLRequest();
            o.setReqHost(reqHost);
            o.setAppConfig(appConfig);
            o.setApiv(1);
            o.setAppv(bid.app.ver);
            o.setBundle(bid.app.bundle);
            o.setTest(bid.test == 1);
            if (bid.regs != null) {
                Regs regs = new Regs();
                regs.setCoppa(bid.regs.coppa);
                if (bid.regs.ext != null) {
                    regs.setGdpr(bid.regs.ext.gdpr);
                    regs.setCcpa(bid.regs.ext.ccpa);
                }
                o.setRegs(regs);
            }

            BidRequest.Device d = bid.device;
            o.setPlat(StringUtils.equalsAnyIgnoreCase(d.os, "iOS") ? 0 : 1);
            o.setUa(d.ua);
            o.setLang(d.language);
            o.setDid(d.ifa);
            o.setMake(d.make);
            o.setModel(d.model);
            o.setOsv(d.osv);
            o.setContype(d.connectiontype);
            o.setCarrier(d.carrier);
            o.setMtype(Util.getModelType(o.getPlat(), o.getModel(), d.ua));

            BidderToken bidToken = objectMapper.readValue(new InputStreamReader(
                    new DeflateInputStream(new ByteArrayInputStream(Base64.decodeBase64(bid.user.id))),
                    UTF_8), BidderToken.class);
            if (!StringUtils.equals(d.ifa, bidToken.did)) {
                // check did ifa
                return ResponseEntity.badRequest().body(new BidResponse(bid, NoBidReason.UNMATCHED_USER, null));
            }
            o.setUid(bidToken.uid);
            o.setSession(bidToken.session);
            o.setSdkv(bidToken.sdkv);
            o.setFit(bidToken.fit);
            o.setFlt(bidToken.flt);
            o.setIap(bidToken.iap);
            o.setDtype(bidToken.dtype);
            o.setZo(bidToken.zo);
            o.setNg(bidToken.ng);
            o.setJb(bidToken.jb);
            o.setBrand(bidToken.brand);
            o.setFm(bidToken.fm);
            o.setBattery(bidToken.battery);
            o.setBtch(bidToken.btch);
            o.setLowp(bidToken.lowp);
            o.setLcountry(bidToken.lcy);

            BidRequest.Imp imp = bid.imp[0];
            o.setPid(NumberUtils.toInt(imp.tagid));
            BidRequest.Imp.AdType adType = imp.banner;
            if (adType == null) adType = imp.$native;
            if (adType == null) adType = imp.video;
            if (adType != null) {
                o.setWidth(adType.w);
                o.setHeight(adType.h);
            }

        } catch (Exception e) {
            LOG.warn("/cp/bid/v1 decode fail", e);
            return ResponseEntity.badRequest().body(e.toString());
        }

        String ip = StringUtils.defaultIfBlank(bid.device.ip, bid.device.ipv6);
        if (StringUtils.isBlank(ip)) {
            ip = GeoService.getClientIP(req);
        }
        o.setGeo(geoService.getGeoData(ip, o.getLcountry()));

        final boolean DEBUG = debugMark != null;
        CLResponse clResp = new CLResponse(DEBUG);
        BidResponse bidResp = new BidResponse(bid, clResp.getDebug());
        o.setBidid(bidResp.bidid);

        if (DEBUG || bid.test == 1) {
            if (bid.device.geo != null && StringUtils.isNoneBlank(bid.device.geo.country)) {
                o.setCountry(bid.device.geo.country);// Test Country
            }
        }

        Placement placement = cacheService.getPlacement(o.getPid());
        PublisherApp pubApp = cacheService.getPublisherApp(bid.app.id);
        if (pubApp == null || placement == null || placement.getPubAppId() != pubApp.getId()) {
            return ResponseEntity.badRequest().body(bidResp.setNbr(NoBidReason.BLOCKED_PUBLISHER));
        }
        o.setPubApp(pubApp);
        o.setPlacement(placement);

        clResp.addDebug("request ip:%s, country:%s, ng:%s", o.getIp(), o.getCountry(), o.getNg());

        // regs deny
        if (bid.test == 0 && o.getRegs() != null && o.getRegs().hasAny()) {
            return ResponseEntity.noContent().build();
        }

        o.setRequireSkAdNetwork(o.getPlat() == CommonPB.Plat.iOS_VALUE
                && o.isEmptyDid()
                && o.getOsVersion().ge(Version.OSV_IOS_14_0_0));

        List<MatchedCampaign> matchedCampaigns = matchCampaigns(o, clResp);
        final boolean hasCampaign = matchedCampaigns != null && !matchedCampaigns.isEmpty();
        MatchedCampaign mc = null;
        if (hasCampaign) {
            mc = matchedCampaigns.get(0);
        }
        if (mc == null) {
            if (DEBUG) {
                return ResponseEntity.ok(bidResp.setNbr(NoBidReason.UNKNOWN_ERROR));
            } else {
                return ResponseEntity.noContent().build();
            }

        }

        return ResponseEntity.ok(bidResp.setAdm(mc, null));
    }

}
