// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.service;

import com.adtiming.om.server.dto.CommonRequest;
import com.adtiming.om.server.dto.GeoData;
import com.adtiming.om.server.dto.GeoDataCommon;
import com.adtiming.om.server.util.CountryCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

/**
 * Obtain geo info
 * The default implementation is to get country from CommonRequest
 * Modify this implementation to use your own IP Service
 */
@Service
public class GeoService {

    private static final Logger LOG = LogManager.getLogger();

    public GeoData getGeoData(HttpServletRequest req, CommonRequest o) {
        String ip = getClientIP(req);
        GeoDataCommon geo = new GeoDataCommon(ip);
        if (o != null) {
            String country = CountryCode.convertToA2(o.getLcountry());
            geo.setCountry(country);
        }
        return geo;
    }

    private String getClientIP(HttpServletRequest req) {
        String clientIP = req.getHeader("X-Real-IP");
        if (clientIP == null) {
            clientIP = req.getRemoteAddr();
        }
        return clientIP;
//        String xff = req.getHeader("X-Forwarded-For");
//        if (StringUtils.isNotBlank(xff)) {
//            return StringUtils.trim(xff.split(",")[0]);
//        } else
//            return remote_ip;
    }

}
