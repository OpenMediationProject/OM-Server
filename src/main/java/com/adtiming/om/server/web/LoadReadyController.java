// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.web;

import com.adtiming.om.server.dto.Instance;
import com.adtiming.om.server.dto.InstanceRule;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import static com.adtiming.om.server.dto.LrRequest.*;

@RestController
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
    public ResponseEntity<?> lr(HttpServletRequest req,
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
            return ResponseEntity.badRequest().body("decode fail");
        }

        if (o.getType() < TYPE_WATERFALL_FILLED) {
            LOG.warn("lr v{}, type {} not allowed", version, o.getType());
            return ResponseEntity.badRequest().body("type denied");
        }

        Placement placement = cacheService.getPlacement(o.getPid());
        if (placement == null) {
            o.setStatus(0, "placement invalid");
            return ResponseEntity.status(HttpStatus.GONE).body("placement invalid");
        }

        if (o.getType() == TYPE_INSTANCE_REQUEST || o.getType() == TYPE_INSTANCE_FILLED) {
            Instance ins = cacheService.getInstanceById(o.getIid());
            if (ins != null && ins.isHeadBidding()) {
                // SDK 误上报了 payload 请求, server 强制过滤
                o.setStatus(0, "ignore bid payload");
                return ResponseEntity.status(HttpStatus.GONE).body("ignore payload");
            }
        }

        if (o.getRuleId() > 0) {
            InstanceRule rule = cacheService.getInstanceRule(o.getRuleId());
            if (rule != null) {
                o.setRuleType(rule.isAutoOpt() ? 1 : 0);
                o.setRp(rule.getPriority());
                if (rule.getAbTestSwitch() == 1) {
                    o.setAbt(rule.getRuleAbt(o.getDid()));
                    o.setAbtId(rule.getRuleAbtId());
                }
            }
        }
        if (o.getIid() > 0) {
            Instance instance = cacheService.getInstanceById(o.getIid());
            if (instance != null) {
                o.setAdnPk(instance.getPlacementKey());
            }
        }

        switch (o.getType()) {
//            case TYPE_WATERFALL_REQUEST:
//                o.setWfReq(1);
//                break;
            case TYPE_WATERFALL_FILLED:
                o.setWfFil(1);
                break;
            case TYPE_INSTANCE_REQUEST:
                o.setInsReq(1);
                break;
            case TYPE_INSTANCE_FILLED:
                o.setInsFil(1);
                break;
            case TYPE_INSTANCE_IMPR:
                o.setImpr(1);
                break;
            case TYPE_INSTANCE_CLICK:
                o.setClick(1);
                break;
            case TYPE_VIDEO_START:
                o.setVdStart(1);
                break;
            case TYPE_VIDEO_COMPLETE:
                o.setVdEnd(1);
                break;
            default:
                return ResponseEntity.badRequest().body("lr type not allowed");
        }

        o.setPlacement(placement);
        o.setStatus(1, null);
        o.writeToLog(logService);
        return ResponseEntity.ok().build();
    }

}
