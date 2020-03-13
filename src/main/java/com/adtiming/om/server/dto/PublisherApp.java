// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

import com.adtiming.om.pb.PubAppPB;
import com.adtiming.om.server.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PublisherApp {

    private PubAppPB.PublisherApp p;

    private List<AppBlockRule> blockRule = Collections.emptyList();

    public PublisherApp(PubAppPB.PublisherApp p) {
        this.p = p;
        if (p.getBlockRulesCount() > 0) {
            blockRule = new ArrayList<>(p.getBlockRulesCount());
            for (PubAppPB.PublisherAppBlockRule rule : p.getBlockRulesList()) {
                blockRule.add(new PublisherAppBlockRule(rule));
            }
        }
    }

    public int getId() {
        return p.getId();
    }

    public int getPublisherId() {
        return p.getPublisherId();
    }

    public int getPlat() {
        return p.getPlat();
    }

    public String getAppId() {
        return p.getAppId();
    }

    public String getAppName() {
        return p.getAppName();
    }

    public String getAppKey() {
        return p.getAppKey();
    }

    public String getBundleId() {
        return p.getBundleId();
    }

    public boolean isBlock(InitRequest o) {
        if (blockRule.isEmpty())
            return false;
        String device = null;
        if (o.getPlat() == 1) {
            device = o.getAndroid().device;
        }
        return Util.matchBlockRule(blockRule, o.getSdkv(), o.getAppv(), o.getOsv(), o.getMake(), device, o.getBrand(), o.getModel());
    }

    public List<Integer> getEventIds() {
        return p.getEventIdsList();
    }
}
