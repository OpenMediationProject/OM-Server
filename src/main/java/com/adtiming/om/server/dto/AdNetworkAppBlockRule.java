// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

import com.adtiming.om.pb.AdNetworkPB;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class AdNetworkAppBlockRule implements AppBlockRule {

    private AdNetworkPB.AdNetworkAppBlockRule rule;
    private Version osv_max, osv_min;

    public AdNetworkAppBlockRule(AdNetworkPB.AdNetworkAppBlockRule rule) {
        this.rule = rule;
        if (StringUtils.isNotBlank(rule.getOsvMax()))
            osv_max = Version.of(rule.getOsvMax());
        if (StringUtils.isNotBlank(rule.getOsvMin()))
            osv_min = Version.of(rule.getOsvMin());
    }

    @Override
    public int getId() {
        return rule.getId();
    }

    public int getPubAppId() {
        return rule.getPubAppId();
    }

    public int getAdnId() {
        return rule.getAdnId();
    }

    @Override
    public String getSdkVersion() {
        return rule.getSdkVersion();
    }

    @Override
    public String getAppVersion() {
        return rule.getAppVersion();
    }

    @Override
    public Version getOsvMax() {
        return osv_max;
    }

    @Override
    public Version getOsvMin() {
        return osv_min;
    }

    @Override
    public List<String> getMakeDeviceBlacklistList() {
        return rule.getMakeDeviceBlacklistList();
    }

    @Override
    public int getMakeDeviceBlacklistCount() {
        return rule.getMakeDeviceBlacklistCount();
    }

    @Override
    public List<String> getBrandModelBlacklistList() {
        return rule.getBrandModelBlacklistList();
    }

    @Override
    public int getBrandModelBlacklistCount() {
        return rule.getBrandModelBlacklistCount();
    }
}
