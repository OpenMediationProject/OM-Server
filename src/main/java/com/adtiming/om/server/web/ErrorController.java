// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.web;

import com.adtiming.om.server.dto.ErrorRequest;
import com.adtiming.om.server.service.AppConfig;
import com.adtiming.om.server.service.CacheService;
import com.adtiming.om.server.service.GeoService;
import com.adtiming.om.server.service.LogService;
import com.adtiming.om.server.util.Compressor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Controller
public class ErrorController extends BaseController {

    private static final Logger LOG = LogManager.getLogger();

    @Resource
    private AppConfig cfg;

    @Resource
    private GeoService geoService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private CacheService cacheService;

    @Resource
    private LogService logService;

    @RequestMapping("/status")
    @ResponseBody
    public Object status() {
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/err", params = "v=1")
    @ResponseBody
    public ResponseEntity<?> error(HttpServletRequest req,
                                   @RequestParam("v") int version, // api version
                                   @RequestParam("plat") int plat, // platform (0:iOS,1:Android)
                                   @RequestParam("sdkv") String sdkv,
                                   @RequestParam("k") String appKey,
                                   @RequestBody byte[] data) {
        ErrorRequest o;
        try {
            o = objectMapper.readValue(Compressor.gunzip2s(data), ErrorRequest.class);
            o.setApiv(version);
            o.setSdkv(sdkv);
            o.setPlat(plat);
            o.setGeo(geoService.getGeoData(req, o));
            o.setAppConfig(cfg);
        } catch (java.util.zip.ZipException | JsonProcessingException e) {
            LOG.warn("err decode fail {}, {}", req.getQueryString(), e.toString());
            return ResponseEntity.badRequest().body("bad data");
        } catch (Exception e) {
            LOG.error("err decode fail {}", req.getQueryString(), e);
            return ResponseEntity.badRequest().body("bad data 2");
        }

        o.setPubApp(cacheService.getPublisherApp(appKey));
        o.writeToLog(logService);
        return ResponseEntity.ok().build();
    }

}
