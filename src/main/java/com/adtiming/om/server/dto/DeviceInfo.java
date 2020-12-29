// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

import java.util.Map;

public interface DeviceInfo {

    /**
     * Client first install App time, in seconds
     */
    int getFit();

    /**
     * The first time the client opens the App, in seconds
     */
    int getFlt();

    int getPlat();

    String getDid();

    int getDtype();

    String getUid();

    String getSession();

    String getLang();

    String getLangname();

    int getJb();

    String getBundle();

    String getMake();

    String getBrand();

    String getModel();

    String getOsv();

    String getAppv();

    int getContype();

    String getCarrier();

    String getMccmnc();

    int getAbt();

    default String getIp() {
        return null;
    }

    default String getCountry() {
        return null;
    }

    default String getRegion() {
        return null;
    }

    default String getCity() {
        return null;
    }

    default String getCnl() {
        return null;
    }

    /**
     * Model Type, { 0b001: Phone, 0b010: Pad, 0b100: TV }
     */
    default int getMtype() {
        return 0;
    }

    /**
     * any legal, governmental, or industry regulations
     */
    default Regs getRegs() {
        return null;
    }

    default Integer getAge() {
        return null;
    }

    /**
     * gender
     *
     * @return {0: unknown, 1: male, 2: female}
     */
    default Integer getGender() {
        return null;
    }

    /**
     * Custom DeviceID
     */
    String getCdid();

    /**
     * App defined user tags
     */
    Map<String, Object> getTags();

}
