// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.web;

import com.adtiming.om.server.dto.IncentivizedRequest;
import com.adtiming.om.server.dto.Placement;
import com.adtiming.om.server.service.AppConfig;
import com.adtiming.om.server.service.CacheService;
import com.adtiming.om.server.service.GeoService;
import com.adtiming.om.server.service.LogService;
import com.adtiming.om.server.util.Compressor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLEncoder;

@Controller
public class IncentivizedController extends BaseController {

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

    @Resource
    private HttpAsyncClient httpAsyncClient;

    private final RequestConfig cbReqCfg = RequestConfig.custom()
            .setRedirectsEnabled(false)
            .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
            .setConnectionRequestTimeout(3000)
            .setConnectTimeout(3000)
            .setSocketTimeout(5000)
            .build();

    @PostMapping(value = "/ic", params = "v=1")
    @ResponseBody
    public ResponseEntity<?> ic(HttpServletRequest req,
                                @RequestHeader("Host") String reqHost,
                                @RequestParam("v") int apiv,
                                @RequestParam("sdkv") String sdkv,
                                @RequestParam("plat") int plat,
                                @RequestBody byte[] body) {
        IncentivizedRequest o;
        try {
            String json = Compressor.gunzip2s(body);
            o = objectMapper.readValue(json, IncentivizedRequest.class);
            o.setApiv(apiv);
            o.setPlat(plat);
            o.setSdkv(sdkv);
            o.setGeo(geoService.getGeoData(req, o));
            o.setReqHost(reqHost);
            o.setAppConfig(cfg);
        } catch (Exception e) {
            LOG.warn("/ic decrypt fail {}, {}", sdkv, e.toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("parse json error");
        }

        try {
            Placement placement = cacheService.getPlacement(o.getPid());
            if (placement == null) {
                o.setStatus(0, "placement invalid").writeToLog(logService);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("placement invalid");
            }

            o.setPlacement(placement);

            if (StringUtils.isBlank(placement.getIcUrl())) {
                o.setStatus(1, "empty icURL").writeToLog(logService);
                return ResponseEntity.ok("OK");
            }

            String cv = URLEncoder.encode(o.getContent(), "UTF-8");
            String url = StringUtils.replace(placement.getIcUrl(), "{content}", cv);
            HttpGet cbReq = new HttpGet(url);
            cbReq.setConfig(cbReqCfg);
            httpAsyncClient.execute(cbReq, new FutureCallback<HttpResponse>() {
                @Override
                public void completed(HttpResponse result) {
                    StatusLine sl = result.getStatusLine();
                    o.setHttpStatus(sl);
                    LOG.debug("placement: {}, sl: {}, icURL: {}", o.getPid(), sl, url);
                    EntityUtils.consumeQuietly(result.getEntity());
                    o.setStatus(1, null).writeToLog(logService);
                }

                @Override
                public void failed(Exception ex) {
                    if (ex instanceof IOException)
                        LOG.debug("icReq error, placement: {}, url: {}, {}", o.getPid(), url, ex.toString());
                    else
                        LOG.error("icReq error, placement: {}, url: {}", o.getPid(), url, ex);
                    o.setStatus(0, ex.toString()).writeToLog(logService);
                }

                @Override
                public void cancelled() {
                    LOG.error("icReq cancelled, placement: {}, url: {}", o.getPid(), url);
                    o.setStatus(0, "cancelled").writeToLog(logService);
                }
            });

        } catch (Exception e) {
            LOG.error("/ic fail {}", sdkv, e);
            o.setStatus(0, e.toString()).writeToLog(logService);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("error");
        }

        return ResponseEntity.ok("OK");
    }

}
