// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Get mcc + mnc from carrierInfo uploaded by the client
 * mccmnc is the first 5 to 6 digits, with name after
 * Not getting returns null
 */
public class Carrier {

    private static final Pattern REG_MCC_MNC_CARRIER = Pattern.compile("^(\\d{5,6})(.*)$");

    private String carrier;
    private String mccmnc;
    private String name;

    private Carrier(String s) {
        this.carrier = StringUtils.trim(s);
        if (carrier == null)
            return;
        Matcher m = REG_MCC_MNC_CARRIER.matcher(carrier);
        if (m.find()) {
            mccmnc = m.group(1);
            name = m.group(2);
        }
    }

    public static Carrier parseFrom(String carrier) {
        return new Carrier(carrier);
    }

    public String getCarrier() {
        return carrier;
    }

    public String getMccmnc() {
        return mccmnc;
    }

    public String getName() {
        return name;
    }
}
