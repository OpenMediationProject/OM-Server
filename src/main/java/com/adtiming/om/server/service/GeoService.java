// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.service;

import com.adtiming.om.server.dto.CommonRequest;
import com.adtiming.om.server.dto.GeoData;
import com.adtiming.om.server.dto.GeoDataCommon;
import com.adtiming.om.server.util.CountryCode;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.model.AbstractCountryResponse;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.model.CountryResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.zip.GZIPInputStream;

/**
 * Obtain geo info
 * The default implementation is to get country from CommonRequest
 * Modify this implementation to use your own IP Service
 * This product includes GeoLite2 data created by MaxMind, available from
 * <a href="https://www.maxmind.com">https://www.maxmind.com</a>.
 */
@Service
public class GeoService {

    private static final Logger LOG = LogManager.getLogger();

    private DatabaseReader dbReader;
    private DBType dbType = DBType.Country;
    private final long[] fileLTS = {0, 0};

    private enum DBType {
        Country, City;
    }

    @Scheduled(cron = "0 5 13 * * ?")
    synchronized void init() {
        // A File object pointing to your GeoIP2 or GeoLite2 database
        File database = new File("cache/GeoIP2-City.mmdb.gz");
        if (!database.exists()) {
            database = new File("cache/GeoIP2-Country.mmdb.gz");
            if (!database.exists()) {
                return;
            } else {
                dbType = DBType.Country;
            }
        } else {
            dbType = DBType.City;
        }
        if (database.lastModified() <= fileLTS[dbType.ordinal()]) {
            LOG.debug("{} not modified since {}, skip", database, new Date(fileLTS[dbType.ordinal()]));
            return;
        }
        try (InputStream in = new GZIPInputStream(new FileInputStream(database))) {
            // This creates the DatabaseReader object. To improve performance, reuse
            // the object across lookups. The object is thread-safe.
            DatabaseReader oldIpReader = dbReader;
            dbReader = new DatabaseReader.Builder(in).build();
            fileLTS[dbType.ordinal()] = database.lastModified();
            if (oldIpReader != null) {
                oldIpReader.close();
            }
            LOG.info("init GeoIP2, {}", dbReader.getMetadata());
        } catch (IOException e) {
            LOG.error("init GeoIP2 error", e);
        }
    }

    public GeoData getGeoData(HttpServletRequest req, CommonRequest o) {
        return getGeoData(getClientIP(req), o == null ? null : o.getLcountry());
    }

    public GeoData getGeoData(HttpServletRequest req, String lcountry) {
        return getGeoData(getClientIP(req), lcountry);
    }

    public GeoData getGeoData(String ip, String lcountry) {
        if (lcountry != null && lcountry.length() != 2) {
            lcountry = CountryCode.convertToA2(lcountry);
        }
        GeoDataCommon geo = new GeoDataCommon(ip);
        if (dbReader == null) {
            geo.setCountry(lcountry);
            return geo;
        }
        try {
            InetAddress addr = InetAddress.getByName(ip);
            if (dbType == DBType.Country) {
                CountryResponse res = dbReader.country(addr);
                geo.setCountry(getCountry(res));
            } else {
                CityResponse res = dbReader.city(addr);
                if (res != null) {
                    geo.setCountry(getCountry(res));
                    geo.setRegion(res.getLeastSpecificSubdivision().getIsoCode());
                    geo.setCity(res.getCity().getName());
                }
            }
            if (geo.getCountry() == null && lcountry != null) {
                geo.setCountry(lcountry);
            }
        } catch (UnknownHostException e) {
            LOG.error("get InetAddress error {}", e.toString());
            geo.setCountry(lcountry);
        } catch (AddressNotFoundException e) {
            LOG.debug(e.toString());
            geo.setCountry(lcountry);
        } catch (Exception e) {
            LOG.error("get geo error", e);
            geo.setCountry(lcountry);
        }
        return geo;
    }

    public static String getClientIP(HttpServletRequest req) {
        String remoteIp = req.getHeader("X-Real-IP");
        return remoteIp == null ? req.getRemoteAddr() : remoteIp;
//        String xff = req.getHeader("X-Forwarded-For");
//        if (StringUtils.isNotBlank(xff)) {
//            return StringUtils.trim(xff.split(",")[0]);
//        } else
//            return remote_ip;
    }

    private String getCountry(AbstractCountryResponse res) {
        if (res == null)
            return null;
        String code = res.getCountry().getIsoCode();
        if (StringUtils.isEmpty(code))
            code = res.getRegisteredCountry().getIsoCode();
        if (StringUtils.isEmpty(code))
            return null;
        return code.toUpperCase();
    }

}
