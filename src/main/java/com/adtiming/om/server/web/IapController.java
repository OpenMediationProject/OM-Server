// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.web;

import com.adtiming.om.server.dto.IapRequest;
import com.adtiming.om.server.dto.PublisherApp;
import com.adtiming.om.server.service.AppConfig;
import com.adtiming.om.server.service.CacheService;
import com.adtiming.om.server.service.GeoService;
import com.adtiming.om.server.service.LogService;
import com.adtiming.om.server.util.Compressor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class IapController extends BaseController {

    private static final Logger LOG = LogManager.getLogger();

    @Resource
    private AppConfig appConfig;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private CacheService cacheService;

    @Resource
    private GeoService geoService;

    @Resource
    private LogService logService;

    @PostMapping(value = "/iap", params = "v=1")
    public ResponseEntity<?> iap(HttpServletRequest req,
                                 @RequestParam("v") int apiv,
                                 @RequestParam("plat") int plat,
                                 @RequestParam("sdkv") String sdkv,
                                 @RequestParam("k") String appKey,
                                 @RequestBody byte[] data) throws IOException {
        IapRequest o;
        try {
            o = objectMapper.readValue(Compressor.gunzip2s(data), IapRequest.class);
            o.setGeo(geoService.getGeoData(req, o));
            o.setApiv(apiv);
            o.setSdkv(sdkv);
            o.setPlat(plat);
            o.setAppConfig(appConfig);
            o.setIapUsd(cacheService.getUsdMoney(o.getCur(), o.getIap()));
        } catch (Exception e) {
            LOG.warn("iap decode fail {}", req.getQueryString(), e);
            return ResponseEntity.badRequest().body("decode fail");
        }
        o.setIapUsd(cacheService.getUsdMoney(o.getCur(), o.getIap()));
        PublisherApp pubApp = cacheService.getPublisherApp(appKey);

        o.writeToLog(logService);

        if (pubApp != null) {
            o.setPubApp(pubApp);

            Map<String, Object> result = new HashMap<>();
            result.put("iapUsd", o.getIapUsd() + o.getIapt());
            return response(result);
        } else {
            LOG.info("appKey {} invaild", appKey);
            return ResponseEntity.status(HttpStatus.GONE).body("app lost");
        }

    }

}
