// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.web;

import com.adtiming.om.server.dto.ConversionDataRequest;
import com.adtiming.om.server.dto.PublisherApp;
import com.adtiming.om.server.service.CacheService;
import com.adtiming.om.server.service.GeoService;
import com.adtiming.om.server.service.LogService;
import com.adtiming.om.server.util.Compressor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
public class ConversionDataController extends BaseController {

    private static final Logger LOG = LogManager.getLogger();

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private CacheService cs;

    @Resource
    private GeoService geoService;

    @Resource
    private LogService logService;

    /**
     * receive conversion data from sdk
     * example: appsflyer conversion data
     */
    @PostMapping("/cd")
    public ResponseEntity<?> cd(HttpServletRequest req,
                                @RequestParam("v") int apiv,       // apiv
                                @RequestParam("plat") int plat,    // platform (0:iOS,1:Android)
                                @RequestParam("sdkv") String sdkv, // sdkv
                                @RequestParam("k") String appKey,  // publisher_app.app_key
                                @RequestBody byte[] data) {
        ConversionDataRequest o;
        try {
            o = objectMapper.readValue(Compressor.gunzip2s(data), ConversionDataRequest.class);
            o.setApiv(apiv);
            o.setSdkv(sdkv);
            o.setPlat(plat);
            o.setGeo(geoService.getGeoData(req, o));
        } catch (Exception e) {
            LOG.warn("cd v{} decode fail", apiv, e);
            return ResponseEntity.badRequest().body("decode fail");
        }

        PublisherApp app = cs.getPublisherApp(appKey);
        if (app == null) {
            LOG.warn("appKey {} invaild", appKey);
        } else {
            o.setPubAppId(app.getId());
            o.setPublisherId(app.getPublisherId());
        }

        o.writeToLog(logService);
        return ResponseEntity.ok().build();
    }

}
