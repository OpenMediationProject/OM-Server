// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

import com.adtiming.om.pb.AdNetworkPB;
import com.adtiming.om.pb.CommonPB;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class InstanceRule {
    private AdNetworkPB.InstanceRule rule;

    public InstanceRule(AdNetworkPB.InstanceRule rule) {
        this.rule = rule;
    }

    public int getId() {
        return rule.getId();
    }

    public int getPublisherId() {
        return rule.getPublisherId();
    }

    public int getPubAppId() {
        return rule.getPubAppId();
    }

    public int getPlacementId() {
        return rule.getPlacementId();
    }

    public String getCountry() {
        return rule.getCountry();
    }

    public int getSortType() {
        return rule.getSortType();
    }

    public CommonPB.ABTest getAbt() {
        return rule.getAbt();
    }

    public int getAbtValue() {
        return rule.getAbtValue();
    }

    public int getSegmentId() {
        return rule.getSegmentId();
    }

    public boolean isAutoOpt() {
        return rule.getAutoSwitch() == 1;
    }

    public Map<Integer, Integer> getInstanceWeightMap() {
        return rule.getInstanceWeightMap();
    }

    public int getInstanceCount() {
        return rule.getInstanceWeightCount();
    }

    public int getPriority() {
        return rule.getPriority();
    }

    public Set<Integer> getInstanceList() {
        if (rule.getInstanceWeightCount() > 0) {
            return rule.getInstanceWeightMap().keySet();
        }
        return Collections.emptySet();
    }
}
