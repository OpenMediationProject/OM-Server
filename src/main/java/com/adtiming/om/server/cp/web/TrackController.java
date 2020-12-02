package com.adtiming.om.server.cp.web;

import com.adtiming.om.server.cp.dto.Campaign;
import com.adtiming.om.server.cp.dto.TrackRequest;
import com.adtiming.om.server.cp.service.CpCacheService;
import com.adtiming.om.server.dto.GeoData;
import com.adtiming.om.server.dto.Placement;
import com.adtiming.om.server.service.AppConfig;
import com.adtiming.om.server.service.CacheService;
import com.adtiming.om.server.service.GeoService;
import com.adtiming.om.server.service.LogService;
import com.adtiming.om.server.web.BaseController;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

@RestController
public class TrackController extends BaseController {

    private static final Logger LOG = LogManager.getLogger();
    private static final byte[] RES_CONTENT = "OK".getBytes();

    @Resource
    private CpCacheService cs;

    @Resource
    private CacheService cacheService;

    @Resource
    private GeoService geoService;

    @Resource
    private LogService logService;

    @Resource
    private AppConfig cfg;

    @RequestMapping({"/cp/click", "/cp/impr"})
    public ResponseEntity<?> tk(HttpServletRequest req,
                                @RequestHeader(value = "Referer", defaultValue = "") String ref,
                                @RequestHeader(value = "User-Agent", defaultValue = "") String ua,
                                @ModelAttribute TrackRequest o) {
        if (o.isExpired()) {
            return ResponseEntity.noContent().build();
        }

        if (!o.isSignOK()) {// 验签失败
            LOG.warn("The parameters are tampered, {}", req.getQueryString());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        o.setUa(ua);
        o.setRef(ref);
        o.setSnode(cfg.getSnode());
        o.setDcenter(cfg.getDcenter());

        GeoData geo = geoService.getGeoData(req, o.getCy());
        o.setIp(geo.getIp());
        o.setCy(geo.getCountry());

        Campaign c = cs.getCampaign(o.getCid());
        if (c == null) {
            LOG.warn("campaign not available {}", o.getCid());
            return ResponseEntity.status(HttpStatus.GONE).body("campaign not available");
        }
        o.setAdPubId(c.getPublisherId());
        o.setPlat(c.getPlatform());
        o.setAppId(c.getAppId());
        o.setBillingType(c.getBillingType());

        Placement place = cacheService.getPlacement(o.getPid());
        if (place != null) {
            o.setAdType(place.getAdTypeValue());
            o.setPublisherId(place.getPublisherId());
            o.setPubAppId(place.getPubAppId());
        }

        final boolean isImpr = req.getRequestURI().equals("/cp/impr");
        if (isImpr) {
            o.setImpr(1);
            if (StringUtils.isNotEmpty(o.getPrice()) && !cs.isTestCampaign(o.getCid())) {
                o.setCost(new BigDecimal(o.getPrice()));
            }
        } else {
            o.setClick(1);
        }

        o.writeToLog(logService);
        return ResponseEntity.ok()
                .header("Expires", "-1")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .body(RES_CONTENT);
    }


}
