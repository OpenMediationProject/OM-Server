// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.web;

import com.adtiming.om.server.dto.InitRequest;
import com.adtiming.om.server.dto.InitResponse;
import com.adtiming.om.server.dto.LrRequest;
import com.adtiming.om.server.dto.PublisherApp;
import com.adtiming.om.server.service.AppConfig;
import com.adtiming.om.server.service.CacheService;
import com.adtiming.om.server.service.GeoService;
import com.adtiming.om.server.service.LogService;
import com.adtiming.om.server.util.Compressor;
import com.adtiming.om.server.util.Util;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;


@Controller
public class InitController extends BaseController {

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

    @PostMapping(value = "/init", params = "v=1")
    @ResponseBody
    public ResponseEntity<?> init(HttpServletRequest req,
                                  @RequestParam("v") int version, // api version
                                  @RequestParam("plat") int plat, // platform
                                  @RequestParam("sdkv") String sdkv,
                                  @RequestParam("k") String appKey, // publisher_app.app_key
                                  @RequestHeader("Host") String reqHost,
                                  @RequestHeader(value = "User-Agent", required = false) String ua,
                                  @RequestBody byte[] data) {
        PublisherApp pubApp = cacheService.getPublisherApp(appKey);
        if (pubApp == null) {
            LOG.info("appKey {} invaild", appKey);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("appKey invaild");
        }

        InitRequest o;
        try {
            o = objectMapper.readValue(Compressor.gunzip2s(data), InitRequest.class);
            // fill fields that not in json
            o.setApiv(version);
            o.setPlat(plat);
            o.setSdkv(sdkv);
            o.setGeo(geoService.getGeoData(req, o));
            o.setUa(ua);
            o.setReqHost(reqHost);
            o.setAppConfig(cfg);
            o.setPubApp(pubApp);
            o.setMtype(Util.getModelType(plat, o.getModel(), ua));
        } catch (Exception e) {
            LOG.warn("init decode fail v{}", version, e);
            return ResponseEntity.badRequest().body("decode fail");
        }

        // set abtMode
        //o.setAbt(cacheService.getAbTestMode(pubApp.getId(), o.getDid()));

        // dev_mode
        Integer devDevicePublisherId = cacheService.getDevDevicePub(o.getDid());
        Integer devAdnId = null;
        if (devDevicePublisherId != null && (devDevicePublisherId == 0 || devDevicePublisherId == pubApp.getPublisherId())) {
            devAdnId = cacheService.getDevAppAdnId(pubApp.getId());
        }

        //dev设备使用配置的 ab_test_mode
        /*if (devDevicePublisherId != null) {
            Integer devAbtMode = cacheService.getDevDeviceAbtMode(o.getDid());
            if (devAbtMode != null) {
                o.setAbt(devAbtMode); // reset abtMode under dev mode
            }
        }*/

        LrRequest lr = o.copyTo(new LrRequest());
        lr.setType(LrRequest.TYPE_INIT);

        if (devAdnId == null && pubApp.isBlock(o)) {// Block by rule in non-test mode
            LOG.info("appBlockRule {}", appKey);
            lr.setStatus(0, "appBlockRule").writeToLog(logService);
            return ResponseEntity.status(HttpStatus.GONE).body("blocked");
        }

        try {
            InitResponse resp = new InitResponse(o, cacheService, pubApp, devDevicePublisherId, devAdnId);
            lr.setStatus(1, null).writeToLog(logService);
            return response(resp);
        } catch (Exception e) {
            LOG.error("init error, appKey: {}", appKey, e);
            lr.setStatus(0, e.toString()).writeToLog(logService);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("init error");
        }

    }

}
