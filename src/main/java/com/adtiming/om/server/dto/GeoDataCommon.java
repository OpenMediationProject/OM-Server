// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

public class GeoDataCommon implements GeoData {

    private String ip;
    private String country, region, city;

    public GeoDataCommon(String ip) {
        this.ip = ip;
    }

    public GeoDataCommon(String ip, String country, String region, String city) {
        this.ip = ip;
        this.country = country;
        this.region = region;
        this.city = city;
    }

    public String getIp() {
        return ip;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
