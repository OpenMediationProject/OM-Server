// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.web;

import com.adtiming.om.pb.CommonPB;
import com.adtiming.om.server.dto.Instance;
import com.adtiming.om.server.dto.LrRequest;
import com.adtiming.om.server.dto.Placement;
import com.adtiming.om.server.service.AppConfig;
import com.adtiming.om.server.service.CacheService;
import com.adtiming.om.server.service.GeoService;
import com.adtiming.om.server.service.LogService;
import com.adtiming.om.server.util.Compressor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.adtiming.om.server.dto.LrRequest.*;

@Controller
public class LoadReadyController extends BaseController {

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

    @PostMapping(value = "/lr", params = "v=1")
    public void lr(HttpServletRequest req, HttpServletResponse res,
                   @RequestParam("v") int version,    // api version
                   @RequestParam("plat") int plat,    // platform (0:iOS,1:Android)
                   @RequestParam("sdkv") String sdkv, // sdk version
                   @RequestBody byte[] data) {
        LrRequest o;
        try {
            o = objectMapper.readValue(Compressor.gunzip2s(data), LrRequest.class);
            o.setApiv(version);
            o.setPlat(plat);
            o.setSdkv(sdkv);
            o.setGeo(geoService.getGeoData(req, o));
            o.setAppConfig(cfg);
        } catch (Exception e) {
            LOG.warn("lr v{} decode fail", version, e);
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (o.getType() < TYPE_WATERFALL_FILLED) {
            LOG.warn("lr v{}, type {} not allowed", version, o.getType());
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Placement placement = cacheService.getPlacement(o.getPid());
        if (placement == null) {
            res.setStatus(HttpServletResponse.SC_NO_CONTENT);
            o.setStatus(0, "placement invalid");
            return;
        }

        if (o.getType() == TYPE_INSTANCE_REQUEST || o.getType() == TYPE_INSTANCE_FILLED) {
            Instance ins = cacheService.getInstanceById(o.getIid());
            if (ins != null && ins.isHeadBidding()) {
                // SDK 误上报了 payload 请求, server 强制过滤
                res.setStatus(HttpServletResponse.SC_NO_CONTENT);
                o.setStatus(0, "ignore bid payload");
                return;
            }
        }

        res.setStatus(HttpServletResponse.SC_OK);
        o.setPlacement(placement);
        o.setAbt(CommonPB.ABTest.None_VALUE);
        o.setStatus(1, null);


        o.writeToLog(logService);
    }

}
