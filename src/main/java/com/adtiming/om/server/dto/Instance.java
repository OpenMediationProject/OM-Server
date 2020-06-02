// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

import com.adtiming.om.pb.AdNetworkPB;
import com.adtiming.om.server.util.Util;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Instance
 */
public class Instance {

    private AdNetworkPB.Instance ins;
    private Set<String> make_whitelist, make_blacklist, brand_whitelist, brand_blacklist, model_whitelist, model_blacklist;
    private Version osv_max, osv_min;

    public Instance(AdNetworkPB.Instance p) {
        this.ins = p;
        if (p.getMakeWhitelistCount() > 0)
            this.make_whitelist = new HashSet<>(p.getMakeWhitelistList());
        if (p.getMakeBlacklistCount() > 0)
            this.make_blacklist = new HashSet<>(p.getMakeBlacklistList());

        if (p.getBrandWhitelistCount() > 0)
            this.brand_whitelist = new HashSet<>(p.getBrandWhitelistList());
        if (p.getBrandBlacklistCount() > 0)
            this.brand_blacklist = new HashSet<>(p.getBrandBlacklistList());

        if (p.getModelWhitelistCount() > 0)
            this.model_whitelist = new HashSet<>(p.getModelWhitelistList());
        if (p.getModelBlacklistCount() > 0)
            this.model_blacklist = new HashSet<>(p.getModelBlacklistList());
        if (StringUtils.isNoneBlank(p.getOsvMax()))
            osv_max = Version.of(p.getOsvMax());
        if (StringUtils.isNoneBlank(p.getOsvMin()))
            osv_min = Version.of(p.getOsvMin());
    }

    public AdNetworkPB.Instance getIns() {
        return ins;
    }

    public int getId() {
        return ins.getId();
    }

    public int getPubAppId() {
        return ins.getPubAppId();
    }

    public int getPlacementId() {
        return ins.getPlacementId();
    }

    public int getAdnId() {
        return ins.getAdnId();
    }

    public String getPlacementKey() {
        return ins.getPlacementKey();
    }

//    public Map<String, SdkPB.SegmentWeight> getContrySegmentWeight() {
//        return ins.getCountrySegmentWeightMap();
//    }

    public int getFrequencyCap() {
        return ins.getFrequencyCap();
    }

    public int getFrequencyUnit() {
        return ins.getFrequencyUnit();
    }

    public int getFrequencyInterval() {
        return ins.getFrequencyInterval();
    }

    public boolean isHeadBidding() {
        return ins.getHbStatus();
    }

    public boolean isAllowOsv(String osv) {
        return Util.isAllowOsv(osv, osv_max, osv_min);
    }

    public boolean isAllowMake(String make) {
        return (make_whitelist == null || make_whitelist.contains(make)) &&
                (make_blacklist == null || !make_blacklist.contains(make));
    }

    public boolean isAllowBrand(String brand) {
        return (brand_whitelist == null || brand_whitelist.contains(brand)) &&
                (brand_blacklist == null || !brand_blacklist.contains(brand));
    }

    public boolean isAllowModel(String model) {
        return (model_whitelist == null || model_whitelist.contains(model)) &&
                (model_blacklist == null || !model_blacklist.contains(model));
    }

    public boolean isAllowPeriod(String country) {
        AdNetworkPB.Instance.CountrySettings cp = ins.getCountrySettingsOrDefault(country, null);
        if (cp == null || cp.getPeriodCount() == 0) {
            cp = ins.getCountrySettingsOrDefault("AA", null);
            if (cp == null || cp.getPeriodCount() == 0)
                return true;
        }
        return Util.isAllowPeriod(cp.getPeriodMap());
    }

    public boolean matchInstance(DeviceInfo d) {
        // match_filter_osv
        if (!isAllowOsv(d.getOsv())) {
            return false;
        }

        // match_filter_make
        if (!isAllowMake(d.getMake())) {
            return false;
        }

        // match_filter_brand
        if (!isAllowBrand(d.getBrand())) {
            return false;
        }

        // match_filter_model
        if (!isAllowModel(d.getModel())) {
            return false;
        }

        //时间过滤
        if (!isAllowPeriod(d.getCountry())) {
            return false;
        }

        return true;
    }
}
