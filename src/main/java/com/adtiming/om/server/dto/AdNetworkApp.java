// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

import com.adtiming.om.pb.AdNetworkPB;
import com.adtiming.om.server.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AdNetworkApp
 */
public class AdNetworkApp {
    public AdNetworkPB.AdNetworkApp m;
    private List<AppBlockRule> blockRule = Collections.emptyList();

    public AdNetworkApp(AdNetworkPB.AdNetworkApp m) {
        this.m = m;
        if (m.getBlockRulesCount() > 0) {
            blockRule = new ArrayList<>(m.getBlockRulesCount());
            for (AdNetworkPB.AdNetworkAppBlockRule rule : m.getBlockRulesList()) {
                blockRule.add(new AdNetworkAppBlockRule(rule));
            }
        }
    }

    public int getId() {
        return m.getId();
    }

    public int getAdnId() {
        return m.getAdnId();
    }

    public int getPubAppId() {
        return m.getPubAppId();
    }

    public String getAppKey() {
        return m.getAppKey();
    }

    public boolean isBlock(CommonRequest o, String device) {
        if (blockRule.isEmpty()) {
            return false;
        }
        return Util.matchBlockRule(blockRule, o.getSdkv(), o.getAppv(), o.getOsv(), o.getMake(), device, o.getBrand(), o.getModel());
    }
}
