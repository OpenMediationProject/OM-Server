// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.web;

import com.adtiming.om.server.dto.IapRequest;
import com.adtiming.om.server.dto.PublisherApp;
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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * IapController
 */
@Controller
public class IapController extends BaseController {

    private static final Logger LOG = LogManager.getLogger();

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private CacheService cacheService;

    @Resource
    private GeoService geoService;

    @Resource
    private LogService logService;

    @PostMapping(value = "/iap", params = "v=1")
    public void iap(HttpServletRequest req, HttpServletResponse res,
                    @RequestParam("v") int apiv,
                    @RequestParam("plat") int plat,
                    @RequestParam("sdkv") String sdkv,
                    @RequestParam("k") String appKey,
                    @RequestBody byte[] data) {
        IapRequest o;
        try {
            o = objectMapper.readValue(Compressor.gunzip2s(data), IapRequest.class);
            o.setGeo(geoService.getGeoData(req, o));
            o.setApiv(apiv);
            o.setSdkv(sdkv);
            o.setPlat(plat);
            o.setIapUsd(cacheService.getUsdMoney(o.getCur(), o.getIap()));
        } catch (java.util.zip.ZipException e) {
            LOG.warn("iap decode fail {}, {}", req.getQueryString(), e.toString());
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        } catch (Exception e) {
            LOG.error("iap decode fail {}", req.getQueryString(), e);
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        o.setIapUsd(cacheService.getUsdMoney(o.getCur(), o.getIap()));
        PublisherApp pubApp = cacheService.getPublisherApp(appKey);

        if (pubApp != null) {
            o.setPubApp(pubApp);

            Map<String, Object> result = new HashMap<>();
            result.put("iapUsd", o.getIapUsd() + o.getIapt());
            try {
                response(res, result);
                res.setStatus(HttpServletResponse.SC_OK);
            } catch (IOException e) {
                LOG.warn("write resp error", e);
            }
        } else {
            res.setStatus(HttpServletResponse.SC_GONE);
            LOG.info("appKey {} invaild", appKey);
        }

        o.writeToLog(logService);
    }

}
