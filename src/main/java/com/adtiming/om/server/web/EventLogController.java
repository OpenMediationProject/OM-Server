// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.web;

import com.adtiming.om.pb.CommonPB;
import com.adtiming.om.server.dto.*;
import com.adtiming.om.server.service.AppConfig;
import com.adtiming.om.server.service.CacheService;
import com.adtiming.om.server.service.GeoService;
import com.adtiming.om.server.service.LogService;
import com.adtiming.om.server.util.Compressor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import static com.adtiming.om.server.dto.EventLogRequest.*;

@RestController
public class EventLogController extends BaseController {

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

    /**
     * for SDK Event Log
     */
    @PostMapping(value = "/log", params = "v=1")
    public ResponseEntity<?> log(HttpServletRequest req,
                                 @RequestParam("v") int version, // api version
                                 @RequestParam("plat") int plat, // platform (0:iOS,1:Android)
                                 @RequestParam("sdkv") String sdkv,
                                 @RequestParam("k") String appKey,
                                 @RequestBody byte[] data) {
        EventLogRequest o;
        try {
            o = objectMapper.readValue(Compressor.gunzip2s(data), EventLogRequest.class);
            o.setApiv(version);
            o.setSdkv(sdkv);
            o.setPlat(plat);
            o.setGeo(geoService.getGeoData(req, o));
            o.setAppConfig(cfg);
        } catch (Exception e) {
            LOG.warn("log decode fail {}", req.getQueryString(), e);
            return ResponseEntity.badRequest().body("bad data");
        }

        o.setPubApp(cacheService.getPublisherApp(appKey));

        long tsSc = o.getServerTs() - o.getTs();// client less than server ts
        // Tile all events
        for (EventLogRequest.Event event : o.events) {
            long clientTs = event.ts;
            event.serverTs = clientTs + tsSc;
            if (event.msg != null) {
                event.msg = StringUtils.replace(event.msg, "\n", "<br>");
            }

            Placement placement = cacheService.getPlacement(event.pid);
            if (placement != null) {
                event.adType = placement.getAdTypeValue();
                event.abt = CommonPB.ABTest.None_VALUE; //cacheService.getAbTestMode(placement, o.getDid());
            }

            if (event.price > 0F) {
                event.price = cacheService.getUsdMoney(event.cur, event.price);
            }

            // add REQUIRED_EVENT_IDS event to lr log
            if (REQUIRED_EVENT_IDS.contains(event.eid)) {
                LrRequest lr = o.copyTo(new LrRequest());
                lr.setTs(event.ts);
                lr.setServerTs(event.serverTs);
                lr.setType(event.eid);
                lr.setBid(event.bid);
                switch (event.eid) {
                    case INSTANCE_BID_REQUEST:
                        lr.setBidReq(1);
                        lr.setBid(1);
                        break;
                    case INSTANCE_BID_RESPONSE:
                        lr.setBidRes(1);
                        lr.setBid(1);
                        if (event.price > 0F) {
                            lr.setBidResPrice(event.price);
                        }
                        break;
                    case INSTANCE_BID_WIN:
                        lr.setBidWin(1);
                        lr.setBid(1);
                        if (event.price > 0F) {
                            lr.setBidWinPrice(event.price);
                        }
                        break;
                    case CALLED_SHOW:
                        lr.setCalledShow(1);
                        break;
                    case CALLED_IS_READY_TRUE:
                        lr.setReadyTrue(1);
                        break;
                    case CALLED_IS_READY_FALSE:
                        lr.setReadyFalse(1);
                        break;
                    case INSTANCE_PAYLOAD_REQUEST:
                        lr.setBid(1);
                        lr.setPlReq(1);
                        break;
                    case INSTANCE_PAYLOAD_SUCCESS:
                        lr.setBid(1);
                        lr.setPlSuccess(1);
                        break;
                    case INSTANCE_PAYLOAD_FAIL:
                        lr.setBid(1);
                        lr.setPlFail(1);
                        break;
                    default:
                        continue;
                }
                lr.setMid(event.mid);
                lr.setPid(event.pid);
                if (placement != null) {
                    lr.setPlacement(placement);
                    lr.setAdType(event.adType);
                }
                lr.setIid(event.iid);
                lr.setScene(event.scene);
                lr.setAbt(event.abt);
                lr.setReqId(event.reqId);
                lr.setRuleId(event.ruleId);
                lr.setRevenue(event.revenue);
                if (event.ruleId > 0) {
                    InstanceRule rule = cacheService.getInstanceRule(event.ruleId);
                    if (rule != null) {
                        lr.setRuleType(rule.isAutoOpt() ? 1 : 0);
                        lr.setRp(rule.getPriority());
                        lr.setIi(rule.getInstancePriority(event.iid, lr.getAbt()));
                        if (rule.getAbTestSwitch() == 1) {
                            lr.setAbt(rule.getRuleAbt(o.getDid()));
                            lr.setAbtId(rule.getRuleAbtId());
                        }
                    }
                }
                if (event.iid > 0) {
                    Instance instance = cacheService.getInstanceById(event.iid);
                    if (instance != null) {
                        lr.setAdnPk(instance.getPlacementKey());
                    }
                }
//                if (event.price > 0F) {
//                    lr.setPrice(event.price);
//                }
                lr.writeToLog(logService);
            }
        }

        o.writeToLog(logService);
        return ResponseEntity.ok().build();
    }

}
