// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

import com.adtiming.om.pb.AdNetworkPB;
import com.adtiming.om.pb.CommonPB;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InstanceRule {

    private final AdNetworkPB.InstanceRule rule;
    private Set<String> brandWhitelist, brandBlacklist, modelWhitelist, modelBlacklist;
    private Set<String> channelList;

    public InstanceRule(AdNetworkPB.InstanceRule rule) {
        this.rule = rule;

        if (rule.getBrandWhitelistCount() > 0) {
            brandWhitelist = new HashSet<>(rule.getBrandWhitelistList());
        }
        if (rule.getBrandBlacklistCount() > 0) {
            brandBlacklist = new HashSet<>(rule.getBrandBlacklistList());
        }

        if (rule.getModelWhitelistCount() > 0) {
            modelWhitelist = new HashSet<>(rule.getModelWhitelistList());
        }
        if (rule.getModelBlacklistCount() > 0) {
            modelBlacklist = new HashSet<>(rule.getModelBlacklistList());
        }

        if (rule.getChannelCount() > 0) {
            this.channelList = new HashSet<>(rule.getChannelList());
        }
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

    public int getSortType() {
        return rule.getSortType();
    }

    public CommonPB.ABTest getAbt() {
        return rule.getAbt();
    }

    public int getAbtValue() {
        return rule.getAbtValue();
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

    /**
     * determin if match rule
     *
     * @param o waterfall request
     * @return matched true, otherwise false
     */
    public boolean isMatched(WaterfallRequest o) {
        if (!isHardMatched(o.getBrand(), o.getModel(), o.getCnl(), o.getMtype())) {
            return false;
        }

        if (rule.getConType() > 0 && (rule.getConType() & o.getContype()) == 0) {
            return false;
        }

        if ((rule.getIapMax() > 0 && o.getIap() > rule.getIapMax())
                || (rule.getIapMin() > 0 && o.getIap() < rule.getIapMin())) {
            return false;
        }

        if (rule.getFrequency() > 0 && o.getImprTimes() < rule.getFrequency()) {
            return false;
        }

        if (rule.getGender() > 0) {
            Integer gender = o.getGender();
            if (gender == null || (rule.getGender() & gender) == 0) {
                return false;
            }
        }

        if (rule.getAgeMax() > 0 || rule.getAgeMin() > 0) {
            Integer age = o.getAge();
            if (age == null) {
                return false;
            }
            if ((rule.getAgeMax() > 0 && age > rule.getAgeMax())
                    || (rule.getAgeMin() > 0 && age < rule.getAgeMin())) {
                return false;
            }
        }

        return true;
    }

    /**
     * 主要用于初始化, 硬控过滤规则
     *
     * @param brand     brand
     * @param model     model
     * @param channel   国内 Android channel
     * @param modelType model type, {0:Phone,1:Pad,2:TV}
     * @return matched true, otherwise false
     */
    public boolean isHardMatched(String brand, String model, String channel, int modelType) {
        if (!isAllowBrand(brand))
            return false;

        if (!isAllowModel(model))
            return false;

        if (!isAllowChannel(channel))
            return false;

        // 二进制不包含
        if (rule.getModelType() > 0 && (rule.getModelType() & modelType) == 0)
            return false;

        return true;
    }

    private boolean isAllowBrand(String brand) {
        return (brandWhitelist == null || brandWhitelist.contains(brand)) &&
                (brandBlacklist == null || !brandBlacklist.contains(brand));
    }

    private boolean isAllowModel(String model) {
        return (modelWhitelist == null || modelWhitelist.contains(model)) &&
                (modelBlacklist == null || !modelBlacklist.contains(model));
    }

    private boolean isAllowChannel(String channel) {
        if (channelList == null)
            return true;
        boolean contains = channelList.contains(channel);
        if (rule.getChannelBow()) {
            return contains; // 白名单,包含
        } else {
            return !contains;// 黑名单,不包含
        }
    }
}
