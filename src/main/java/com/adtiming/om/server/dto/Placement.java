// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

import com.adtiming.om.pb.CommonPB;
import com.adtiming.om.pb.PlacementPB;
import com.adtiming.om.server.util.Util;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Placement {

    private PlacementPB.Placement p;
    private Set<String> sdkVersionBlacklist;
    private Set<String> makeWhitelist, makeBlacklist, brandWhitelist, brandBlacklist, modelWhitelist, modelBlacklist;
    private Set<String> didBlacklist;
    private Set<String> osvBlacklist, osvWhitelist;
    private Version osvMax, osvMin;

    public Placement(PlacementPB.Placement p) {
        this.p = p;

        if (p.getSdkvBlacklistCount() > 0)
            this.sdkVersionBlacklist = new HashSet<>(p.getSdkvBlacklistList());

        if (p.getMakeWhitelistCount() > 0)
            this.makeWhitelist = new HashSet<>(p.getMakeWhitelistList());
        if (p.getMakeBlacklistCount() > 0)
            this.makeBlacklist = new HashSet<>(p.getMakeBlacklistList());

        if (p.getBrandWhitelistCount() > 0)
            this.brandWhitelist = new HashSet<>(p.getBrandWhitelistList());
        if (p.getBrandBlacklistCount() > 0)
            this.brandBlacklist = new HashSet<>(p.getBrandBlacklistList());

        if (p.getModelWhitelistCount() > 0)
            this.modelWhitelist = new HashSet<>(p.getModelWhitelistList());
        if (p.getModelBlacklistCount() > 0)
            this.modelBlacklist = new HashSet<>(p.getModelBlacklistList());

        if (p.getDidBlacklistCount() > 0)
            this.didBlacklist = new HashSet<>(p.getDidBlacklistList());

        if (StringUtils.isNoneBlank(p.getOsvMax()))
            osvMax = Version.of(p.getOsvMax());
        if (StringUtils.isNoneBlank(p.getOsvMin()))
            osvMin = Version.of(p.getOsvMin());

        if (p.getOsvWhitelistCount() > 0)
            this.osvWhitelist = new HashSet<>(p.getOsvWhitelistList());
        if (p.getOsvBlacklistCount() > 0)
            this.osvBlacklist = new HashSet<>(p.getOsvBlacklistList());
    }

    public int getId() {
        return p.getId();
    }

    public int getPublisherId() {
        return p.getPublisherId();
    }

    public int getPubAppId() {
        return p.getPubAppId();
    }

    public int getAdTypeValue() {
        return p.getAdTypeValue();
    }

    public CommonPB.AdType getAdType() {
        return p.getAdType();
    }

    public boolean isMainPlacement() {
        return p.getMainPlacement();
    }

    public boolean getFanOut() {
        return p.getFanOut();
    }

    public int getBatchSize() {
        return p.getBatchSize();
    }

    public int getPreloadTimeout() {
        return p.getPreloadTimeout();
    }

    public String getIcUrl() {
        return p.getIcUrl();
    }

//    public boolean isAllowHb() {
//        return p.getAllowHb();
//    }

    public boolean isBlockSdkVersion(String sdkv) {
        return sdkVersionBlacklist != null && sdkVersionBlacklist.contains(sdkv);
    }

    public boolean isAllowOsv(String osv) {
        return Util.isAllowOsv(osv, osvMax, osvMin)
                && (osvWhitelist == null || osvWhitelist.contains(osv))
                && (osvBlacklist == null || !osvBlacklist.contains(osv));
    }

    public boolean isAllowMake(String make) {
        return (makeWhitelist == null || makeWhitelist.contains(make)) &&
                (makeBlacklist == null || !makeBlacklist.contains(make));
    }

    public boolean isAllowBrand(String brand) {
        return (brandWhitelist == null || brandWhitelist.contains(brand)) &&
                (brandBlacklist == null || !brandBlacklist.contains(brand));
    }

    public boolean isAllowModel(String model) {
        return (modelWhitelist == null || modelWhitelist.contains(model)) &&
                (modelBlacklist == null || !modelBlacklist.contains(model));
    }

    public boolean isBlockDeviceId(String device_id) {
        return didBlacklist != null && didBlacklist.contains(device_id);
    }

    public int getFrequencyCap() {
        return p.getFrequencyCap();
    }

    public int getFrequencyUnit() {
        return p.getFrequencyUnit();
    }

    public int getFrequencyInterval() {
        return p.getFrequencyInterval();
    }

    public boolean isAllowPeriod(String country) {
        PlacementPB.Placement.CountrySettings cs = p.getCountrySettingsOrDefault(country, null);
        if (cs == null || cs.getPeriodCount() == 0) {
            cs = p.getCountrySettingsOrDefault("AA", null);
            if (cs == null || cs.getPeriodCount() == 0)
                return true;
        }
        return Util.isAllowPeriod(cs.getPeriodMap());
    }

    /*public boolean isAbTestOn() {
        return p.getAbTest();
    }*/

    public List<PlacementPB.Scene> getScenes() {
        return p.getScenesList();
    }

    public int getScenesCount() {
        return p.getScenesCount();
    }

    public float[] getFloorAndMaxPrice(String country) {
        float floorPrice = -1F;
        float maxPrice = -1F;
        PlacementPB.Placement.CountrySettings cs = p.getCountrySettingsOrDefault(country, null);
        if (cs != null) {
            if (cs.getFloorPrice() > -1F) floorPrice = cs.getFloorPrice();
            if (cs.getMaxPrice() > -1F) maxPrice = cs.getMaxPrice();
        }
        //未配置country维度取placement配置
        if (floorPrice < 0F && p.getFloorPrice() > -1F)
            floorPrice = p.getFloorPrice();
//        if (maxPrice < 0F && p.getMaxPrice() > -1F)
//            maxPrice = p.getMaxPrice();
        return new float[]{floorPrice, maxPrice};
    }

    public int getInventoryCount() {
        return p.getInventoryCount();
    }

    public int getInventoryInterval() {
        return p.getInventoryInterval();
    }

    public Map<Integer, Integer> getInventoryIntervalStepMap() {
        return p.getInventoryIntervalStepMap();
    }

    public int getReloadInterval() {
        return p.getReloadInterval();
    }
}
