// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.service;

import com.adtiming.om.server.dto.CommonRequest;
import com.adtiming.om.server.dto.GeoData;
import com.adtiming.om.server.dto.GeoDataCommon;
import com.adtiming.om.server.util.CountryCode;
import com.adtiming.om.server.util.RequestParamsUtil;

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
        String ip = RequestParamsUtil.getClientIp(req);;
        GeoDataCommon geo = new GeoDataCommon(ip);
        if (o != null) {
            String country = CountryCode.convertToA2(o.getLcountry());
            geo.setCountry(country);
        }
        return geo;
    }
}
