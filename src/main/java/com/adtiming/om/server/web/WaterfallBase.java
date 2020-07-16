package com.adtiming.om.server.web;

import com.adtiming.om.server.dto.*;
import com.adtiming.om.server.service.AppConfig;
import com.adtiming.om.server.service.CacheService;
import com.adtiming.om.server.service.GeoService;
import com.adtiming.om.server.util.Compressor;
import com.adtiming.om.server.util.Util;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class WaterfallBase extends BaseController {

    private static final Logger LOG = LogManager.getLogger();

    @Resource
    private ObjectMapper objectMapper;

    public static class DebugMsgs extends ArrayList<CharSequence> {
        static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

        @Override
        public boolean add(CharSequence o) {
            return super.add(LocalDateTime.now().format(fmt) + " - " + o);
        }
    }

    WaterfallRequest fillRequestParams(byte[] data, int apiv, String sdkv, int plat, String ua, String reqHost, HttpServletRequest req, GeoService geoService, AppConfig cfg, CacheService cacheService) {
        WaterfallRequest o;
        try {
            o = objectMapper.readValue(Compressor.gunzip2s(data), WaterfallRequest.class);
            o.setApiv(apiv);
            o.setSdkv(sdkv);
            o.setPlat(plat);
            o.setUa(ua);
            o.setReqHost(reqHost);
            o.setGeo(geoService.getGeoData(req, o));
            o.setAppConfig(cfg);
            o.processBidPrices(cacheService);
            o.setMtype(Util.getModelType(plat, o.getModel(), ua));
        } catch (Exception e) {
            LOG.warn("wf decode fail v{}", apiv, e);
            return null;
        }
        return o;
    }

    Set<Integer> getBlockAdnIds(List<AdNetworkApp> adnApps, WaterfallRequest o) {
        Set<Integer> removeAdnIds = Collections.emptySet();
        for (AdNetworkApp app : adnApps) {
            if (app.isBlock(o, "*")) {
                if (removeAdnIds.isEmpty())
                    removeAdnIds = new HashSet<>();
                removeAdnIds.add(app.getAdnId());
            }
        }
        return removeAdnIds;
    }

    List<Instance> matchDev(Integer devDevicePubId, Placement p, CacheService cacheService) {
        if (devDevicePubId != null) {
            // When devDevicePubId is 0, all apps are remediated in test mode
            if (devDevicePubId == 0 || devDevicePubId == p.getPublisherId()) {
                Integer adnId = cacheService.getDevAppAdnId(p.getPubAppId());
                if (adnId != null) {
                    return cacheService.getPlacementAdnInstances(p.getId(), adnId);
                }
            }
        }
        return null;
    }

    boolean matchPlacement(WaterfallRequest o, Placement p) {
        if (p.isBlockSdkVersion(o.getSdkv())) {
            return false;
        }

        if (!p.isAllowOsv(o.getOsv())) {
            return false;
        }

        if (!p.isAllowMake(o.getMake())) {
            return false;
        }

        if (!p.isAllowBrand(o.getBrand())) {
            return false;
        }

        if (!p.isAllowModel(o.getModel())) {
            return false;
        }

        if (p.isBlockDeviceId(o.getDid())) {
            return false;
        }

        if (!p.isAllowPeriod(o.getCountry())) {
            return false;
        }

        return true;
    }

    InstanceRule getMatchedRule(List<InstanceRule> rules, WaterfallRequest o) {
        if (rules == null || rules.isEmpty())
            return null;
        for (InstanceRule rule : rules) {
            if (rule.isMatched(o)) {
                return rule;
            }
        }
        return null;
    }
}
