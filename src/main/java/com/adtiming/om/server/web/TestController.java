// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.web;

import com.adtiming.om.server.service.GeoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
public class TestController {

    @Resource
    private GeoService geoService;

    /**
     * Test IP & Headers
     */
    @RequestMapping("/ip")
    public Object ip(HttpServletRequest req, String hs) {
        Map<String, Object> v = new HashMap<>(2);
        v.put("geo", geoService.getGeoData(req, null));
        if (hs != null) {
            List<String> headers = new ArrayList<>();
            Enumeration<String> names = req.getHeaderNames();
            while (names.hasMoreElements()) {
                String n = names.nextElement();
                Enumeration<String> vs = req.getHeaders(n);
                while (vs.hasMoreElements())
                    headers.add(String.format("%s: %s", n, vs.nextElement()));
            }
            v.put("headers", headers);
        }
        return v;
    }

}
