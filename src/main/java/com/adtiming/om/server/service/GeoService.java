// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.service;

import com.adtiming.om.server.dto.CommonRequest;
import com.adtiming.om.server.dto.GeoData;
import com.adtiming.om.server.dto.GeoDataCommon;
import com.adtiming.om.server.util.CountryCode;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

/**
 * Obtain geo info
 * The default implementation is to get country from CommonRequest
 * Modify this implementation to use your own IP Service
 */
@Service
public class GeoService {

    public GeoData getGeoData(HttpServletRequest req, CommonRequest o) {
        return getGeoData(getClientIP(req), o == null ? null : o.getLcountry());
    }

    public GeoData getGeoData(HttpServletRequest req, String defaultCountry) {
        return getGeoData(getClientIP(req), defaultCountry);
    }

    public GeoData getGeoData(String ip, String defaultCountry) {
        GeoDataCommon geo = new GeoDataCommon(ip);
        if (defaultCountry != null) {
            String country = CountryCode.convertToA2(defaultCountry);
            geo.setCountry(country);
        }
        return geo;
    }

    public static String getClientIP(HttpServletRequest req) {
        return RequestParamsUtil.getClientIp(req);
    }

}
