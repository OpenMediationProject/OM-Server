// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

import com.adtiming.om.pb.AdNetworkPB;
import com.adtiming.om.server.util.Util;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Segment
 */
public class Segment {

    private AdNetworkPB.Segment s;
    private Set<String> brandWhitelist = Collections.emptySet();
    private Set<String> brandBlacklist = Collections.emptySet();
    private Set<String> modelWhitelist = Collections.emptySet();
    private Set<String> modelBlacklist = Collections.emptySet();

    public Segment(AdNetworkPB.Segment s) {
        this.s = s;
        if (s.getBrandWhitelistCount() > 0) {
            brandWhitelist = new HashSet<>(s.getBrandWhitelistList());
        }
        if (s.getBrandBlacklistCount() > 0) {
            brandBlacklist = new HashSet<>(s.getBrandBlacklistList());
        }

        if (s.getModelWhitelistCount() > 0) {
            modelWhitelist = new HashSet<>(s.getModelWhitelistList());
        }
        if (s.getModelBlacklistCount() > 0) {
            modelBlacklist = new HashSet<>(s.getModelBlacklistList());
        }
    }

    public int getId() {
        return s.getId();
    }

    public int getPlacementId() {
        return s.getPlacementId();
    }

    public String getCountry() {
        return s.getCountry();
    }

    public int getFrequency() {
        return s.getFrequency();
    }

    public int getConType() {
        return s.getConType();
    }

    public float getIapMin() {
        return s.getIapMin();
    }

    public float getIapMax() {
        return s.getIapMax();
    }

    public boolean isMatched(String country, int conType, String brand, String model, float iap, int pic) {
        if (!Util.COUNTRY_ALL.equals(s.getCountry()) && !country.equals(s.getCountry())) {
            return false;
        }
        if (s.getConType() > 0 && (s.getConType() & conType) == 0)
            return false;

        if (!brandWhitelist.isEmpty() && (StringUtils.isBlank(brand) || !brandWhitelist.contains(brand.toLowerCase())))
            return false;

        if (!brandBlacklist.isEmpty() && brandBlacklist.contains(model.toLowerCase()))
            return false;

        if (!modelWhitelist.isEmpty() && (StringUtils.isBlank(brand) || !modelWhitelist.contains(brand.toLowerCase())))
            return false;

        if (!modelBlacklist.isEmpty() && modelBlacklist.contains(model.toLowerCase()))
            return false;

        if ((s.getIapMax() > 0 && iap > s.getIapMax()) || (s.getIapMin() > 0 && iap < s.getIapMin()))
            return false;

        if (s.getFrequency() > 0 && pic < s.getFrequency())
            return false;

        return true;
    }

}
