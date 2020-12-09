// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.cp.dto;

import com.adtiming.om.pb.CrossPromotionPB;
import com.adtiming.om.server.dto.VersionRange;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CampaignTargeting {
    private final CrossPromotionPB.CpCampaignTargeting cpct;
    public Set<String> mccmncWhite = Collections.emptySet();
    public Set<String> mccmncBlack = Collections.emptySet();
    public Set<String> countryWhite = Collections.emptySet();
    public Set<String> countryBlack = Collections.emptySet();
    public Set<Integer> pubappWhite = Collections.emptySet();
    public Set<Integer> pubappBlack = Collections.emptySet();
    public Set<Integer> placementWhite = Collections.emptySet();
    public Set<Integer> placementBlack = Collections.emptySet();
    public Set<Integer> devicetypeWhite = Collections.emptySet();
    public Set<Integer> devicetypeBlack = Collections.emptySet();
    public Set<String> makeWhite = Collections.emptySet();
    public Set<String> makeBlack = Collections.emptySet();
    public Set<String> brandWhite = Collections.emptySet();
    public Set<String> brandBlack = Collections.emptySet();
    public Set<String> modelWhite = Collections.emptySet();
    public Set<String> modelBlack = Collections.emptySet();
    public Set<String> osvWhite = Collections.emptySet();
    public Set<String> osvBlack = Collections.emptySet();
    public List<VersionRange> osvWhiteRange = Collections.emptyList();
    public List<VersionRange> osvBlackRange = Collections.emptyList();

    public CampaignTargeting(CrossPromotionPB.CpCampaignTargeting cpct) {
        this.cpct = cpct;
        if (cpct.getMccmncWhiteCount() > 0) {
            mccmncWhite = new HashSet<>(cpct.getMccmncWhiteList());
        }
        if (cpct.getMccmncBlackCount() > 0) {
            mccmncBlack = new HashSet<>(cpct.getMccmncBlackList());
        }
        if (cpct.getCountryWhiteCount() > 0) {
            countryWhite = new HashSet<>(cpct.getCountryWhiteList());
        }
        if (cpct.getCountryBlackCount() > 0) {
            countryBlack = new HashSet<>(cpct.getCountryBlackList());
        }
        if (cpct.getPubappWhiteCount() > 0) {
            pubappWhite = new HashSet<>(cpct.getPubappWhiteList());
        }
        if (cpct.getPubappBlackCount() > 0) {
            pubappBlack = new HashSet<>(cpct.getPubappBlackList());
        }
        if (cpct.getPlacementWhiteCount() > 0) {
            placementWhite = new HashSet<>(cpct.getPlacementWhiteList());
        }
        if (cpct.getPlacementBlackCount() > 0) {
            placementBlack = new HashSet<>(cpct.getPlacementBlackList());
        }
        if (cpct.getDevicetypeWhiteCount() > 0) {
            devicetypeWhite = new HashSet<>(cpct.getDevicetypeWhiteList());
        }
        if (cpct.getDevicetypeBlackCount() > 0) {
            devicetypeBlack = new HashSet<>(cpct.getDevicetypeBlackList());
        }
        if (cpct.getMakeWhiteCount() > 0) {
            makeWhite = new HashSet<>(cpct.getMakeWhiteList());
        }
        if (cpct.getMakeBlackCount() > 0) {
            makeBlack = new HashSet<>(cpct.getMakeBlackList());
        }
        if (cpct.getBrandWhiteCount() > 0) {
            brandWhite = new HashSet<>(cpct.getBrandWhiteList());
        }
        if (cpct.getBrandBlackCount() > 0) {
            brandBlack = new HashSet<>(cpct.getBrandBlackList());
        }
        if (cpct.getModelWhiteCount() > 0) {
            modelWhite = new HashSet<>(cpct.getModelWhiteList());
        }
        if (cpct.getModelBlackCount() > 0) {
            modelBlack = new HashSet<>(cpct.getModelBlackList());
        }
        if (cpct.getOsvWhiteCount() > 0) {
            osvWhite = new HashSet<>(cpct.getOsvWhiteList());
        }
        if (cpct.getOsvBlackCount() > 0) {
            osvBlack = new HashSet<>(cpct.getOsvBlackList());
        }
        if (cpct.getOsvWhiteRangeCount() > 0) {
            osvWhiteRange = cpct.getOsvWhiteRangeList().stream().map(VersionRange::new).collect(Collectors.toList());
        }
        if (cpct.getOsvBlackRangeCount() > 0) {
            osvBlackRange = cpct.getOsvBlackRangeList().stream().map(VersionRange::new).collect(Collectors.toList());
        }
    }

    public int getContype() {
        return cpct.getContype();
    }

    public boolean acceptCarrier(String mccmnc) {
        return mccmncWhite.isEmpty() || mccmncWhite.contains(mccmnc);
    }

    public boolean acceptCountry(String country) {
        return countryBlack.isEmpty() || !countryBlack.contains(country);
    }

    public boolean acceptMake(String make) {
        if (!makeWhite.isEmpty() && !makeWhite.contains(make)) {
            return false;
        }
        return makeBlack.isEmpty() || !makeBlack.contains(make);
    }

    public boolean acceptBrand(String brand) {
        if (!brandWhite.isEmpty() && !brandWhite.contains(brand)) {
            return false;
        }
        return brandBlack.isEmpty() || !brandBlack.contains(brand);
    }

    public boolean acceptModel(String model) {
        if (!modelWhite.isEmpty() && !modelWhite.contains(model)) {
            return false;
        }
        return modelBlack.isEmpty() || !modelBlack.contains(model);
    }

    public boolean acceptPublisherApp(int pubAppId) {
        if (!pubappWhite.isEmpty() && !pubappWhite.contains(pubAppId)) {
            return false;
        }
        return pubappBlack.isEmpty() || !pubappBlack.contains(pubAppId);
    }

    public boolean acceptPlacement(int placementId) {
        if (!placementWhite.isEmpty() && !placementWhite.contains(placementId)) {
            return false;
        }
        return placementBlack.isEmpty() || !placementBlack.contains(placementId);
    }

    public boolean acceptDeviceType(int deviceType) {
        if (!devicetypeWhite.isEmpty() && !devicetypeWhite.contains(deviceType)) {
            return false;
        }
        return devicetypeBlack.isEmpty() || !devicetypeBlack.contains(deviceType);
    }

    public boolean acceptOsv(String osv) {
        if (!osvWhite.isEmpty() && !osvWhite.contains(osv)) {
            return false;
        }
        if (!osvBlack.isEmpty() && osvBlack.contains(osv)) {
            return false;
        }
        boolean matchWhiteRange = osvWhiteRange.isEmpty();
        for (VersionRange range : osvWhiteRange) {
            if (range.isInRange(osv)) {
                matchWhiteRange = true;
                break;
            }
        }
        boolean matchBlackRange = !osvBlackRange.isEmpty();
        for (VersionRange range : osvBlackRange) {
            if (range.isInRange(osv)) {
                matchBlackRange = true;
                break;
            }
        }
        return matchWhiteRange && !matchBlackRange;
    }
}
