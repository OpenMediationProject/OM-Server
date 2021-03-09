// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

import com.adtiming.om.pb.AdNetworkPB;
import com.adtiming.om.pb.CommonPB;
import com.adtiming.om.server.util.Util;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class InstanceRule {

    private final AdNetworkPB.InstanceRule rule;
    private Set<String> brandWhitelist, brandBlacklist, modelWhitelist, modelBlacklist;
    private Set<String> channelList;
    private Set<String> osvWhite;
    private Set<String> sdkvWhite;
    private Set<String> appvWhite;
    private List<VersionRange> osvRanges;
    private List<VersionRange> sdkvRanges;
    private List<VersionRange> appvRanges;
    private Map<Integer, Integer> instancePriority = Collections.emptyMap();

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

        if (rule.getOsvWhiteCount() > 0) {
            osvWhite = new HashSet<>(rule.getOsvWhiteList());
        }

        if (rule.getSdkvWhiteCount() > 0) {
            sdkvWhite = new HashSet<>(rule.getSdkvWhiteList());
        }

        if (rule.getAppvWhiteCount() > 0) {
            appvWhite = new HashSet<>(rule.getAppvWhiteList());
        }

        if (rule.getOsvRangeCount() > 0) {
            osvRanges = rule.getOsvRangeList().stream().map(VersionRange::new).collect(Collectors.toList());
        }

        if (rule.getSdkvRangeCount() > 0) {
            sdkvRanges = rule.getSdkvRangeList().stream().map(VersionRange::new).collect(Collectors.toList());
        }

        if (rule.getAppvRangeCount() > 0) {
            appvRanges = rule.getAppvRangeList().stream().map(VersionRange::new).collect(Collectors.toList());
        }
        if (rule.getInstanceWeightCount() > 0) {
            instancePriority = new HashMap<>(rule.getInstanceWeightCount());
            rule.getInstanceWeightMap().forEach((iid, priority) -> {
                if (isAutoOpt()) {
                    instancePriority.put(iid, 0);
                } else {
                    instancePriority.put(iid, priority);
                }
            });
            /*int index = 1;
            List<Integer> instanceList = Util.sortByPriority(rule.getInstanceWeightMap());
            for (int iid : instanceList) {
                if (isAutoOpt()) {
                    instancePriority.put(iid, 0);
                } else {
                    instancePriority.put(iid, index);
                }
                index++;
            }*/
        }
    }

    public int getId() {
        return rule.getId();
    }

    public String getName() {
        return rule.getName();
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

    public Integer getInstancePriority(int instanceId) {
        return instancePriority.getOrDefault(instanceId, 0);
    }

    /**
     * determin if match rule
     *
     * @param o waterfall request
     * @return matched true, otherwise false
     */
    public boolean isMatched(WaterfallRequest o) {
        if (!isHardMatched(o.getBrand(), o.getModel(), o.getCnl(), o.getMtype(), o.getOsv(), o.getSdkv(), o.getAppv(), o.getDid())) {
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

        if (rule.getCustomTagsCount() > 0) {
            Map<String, Object> tags = o.getTags();
            if (tags == null || tags.isEmpty()) {
                return false;
            }
            AtomicBoolean tagMatched = new AtomicBoolean(true);
            rule.getCustomTagsMap().forEach((k, v) -> {
                Object tagVal = tags.get(k);
                if (!matchedTag(v, tagVal)) {
                    tagMatched.set(false);
                }
            });
            return tagMatched.get();
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
    public boolean isHardMatched(String brand, String model, String channel, int modelType, String osv, String sdkv,
                                 String appv, String did) {
        if (!isAllowBrand(brand))
            return false;

        if (!isAllowModel(model))
            return false;

        if (!isAllowChannel(channel))
            return false;

        // 二进制不包含
        if (rule.getModelType() > 0 && (rule.getModelType() & modelType) == 0)
            return false;

        if (!Util.matchVersion(osvRanges, osv)) {
            return false;
        }

        if (!Util.matchVersion(sdkvRanges, sdkv)) {
            return false;
        }

        // appv获取不到的情况不过滤
        if (StringUtils.isNotBlank(appv) && !Util.matchVersion(appvRanges, appv)) {
            return false;
        }

        if (!CollectionUtils.isEmpty(osvWhite) &&  !osvWhite.contains(osv)) {
            return false;
        }

        if (!CollectionUtils.isEmpty(sdkvWhite) &&  !sdkvWhite.contains(sdkv)) {
            return false;
        }

        if (!CollectionUtils.isEmpty(appvWhite) &&  !appvWhite.contains(appv)) {
            return false;
        }

        //Rule中配置Did为必须且无did情况
        return rule.getRequireDid() != 1 || !Util.isEmptyDid(did);
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

    private boolean matchedTag(AdNetworkPB.CustomTag tag, Object value) {
        if (tag.getConditionsCount() > 0 ) {
            if (ObjectUtils.isEmpty(value)) {
                return false;
            }
            for (AdNetworkPB.TagCondition tc : tag.getConditionsList()) {
                if (!matchedTagCondition(tag.getType(), tc, value)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean matchedTagCondition(int type, AdNetworkPB.TagCondition tc, Object value) {
        switch (tc.getOperator()) {
            case ">":
                if (type == 0) {
                    if (value instanceof Collection) {
                        Collection<?> list = (Collection<?>) value;
                        for (Object val : list) {
                            if (val.toString().compareTo(tc.getValue()) > 0) {
                                return true;
                            }
                        }
                    } else {
                        return value.toString().compareTo(tc.getValue()) > 0;
                    }
                } else if (type == 1) {
                    if (value instanceof Collection) {
                        Collection<?> list = (Collection<?>) value;
                        for (Object val : list) {
                            if (NumberUtils.toInt(val.toString()) > NumberUtils.toInt(tc.getValue())) {
                                return true;
                            }
                        }
                    } else {
                        return NumberUtils.toInt(value.toString()) > NumberUtils.toInt(tc.getValue());
                    }
                } else {
                    if (value instanceof Collection) {
                        Collection<?> list = (Collection<?>) value;
                        for (Object val : list) {
                            if (NumberUtils.toFloat(val.toString()) > NumberUtils.toFloat(tc.getValue())) {
                                return true;
                            }
                        }
                    } else {
                        return NumberUtils.toFloat(value.toString()) > NumberUtils.toFloat(tc.getValue());
                    }
                }
                break;
            case ">=":
                if (type == 0) {
                    if (value instanceof Collection) {
                        Collection<?> list = (Collection<?>) value;
                        for (Object val : list) {
                            if (val.toString().compareTo(tc.getValue()) >= 0) {
                                return true;
                            }
                        }
                    } else {
                        return value.toString().compareTo(tc.getValue()) >= 0;
                    }
                } else if (type == 1) {
                    if (value instanceof Collection) {
                        Collection<?> list = (Collection<?>) value;
                        for (Object val : list) {
                            if (NumberUtils.toInt(val.toString()) >= NumberUtils.toInt(tc.getValue())) {
                                return true;
                            }
                        }
                    } else {
                        return NumberUtils.toInt(value.toString()) >= NumberUtils.toInt(tc.getValue());
                    }
                } else {
                    if (value instanceof Collection) {
                        Collection<?> list = (Collection<?>) value;
                        for (Object val : list) {
                            if (NumberUtils.toFloat(val.toString()) >= NumberUtils.toFloat(tc.getValue())) {
                                return true;
                            }
                        }
                    } else {
                        return NumberUtils.toFloat(value.toString()) >= NumberUtils.toFloat(tc.getValue());
                    }
                }
                break;
            case "<":
                if (type == 0) {
                    if (value instanceof Collection) {
                        Collection<?> list = (Collection<?>) value;
                        for (Object val : list) {
                            if (val.toString().compareTo(tc.getValue()) < 0) {
                                return true;
                            }
                        }
                    } else {
                        return value.toString().compareTo(tc.getValue()) < 0;
                    }
                } else if (type == 1) {
                    if (value instanceof Collection) {
                        Collection<?> list = (Collection<?>) value;
                        for (Object val : list) {
                            if (NumberUtils.toInt(val.toString()) < NumberUtils.toInt(tc.getValue())) {
                                return true;
                            }
                        }
                    } else {
                        return NumberUtils.toInt(value.toString()) < NumberUtils.toInt(tc.getValue());
                    }
                } else {
                    if (value instanceof Collection) {
                        Collection<?> list = (Collection<?>) value;
                        for (Object val : list) {
                            if (NumberUtils.toFloat(val.toString()) < NumberUtils.toFloat(tc.getValue())) {
                                return true;
                            }
                        }
                    } else {
                        return NumberUtils.toFloat(value.toString()) < NumberUtils.toFloat(tc.getValue());
                    }
                }
                break;
            case "<=":
                if (type == 0) {
                    if (value instanceof Collection) {
                        Collection<?> list = (Collection<?>) value;
                        for (Object val : list) {
                            if (val.toString().compareTo(tc.getValue()) <= 0) {
                                return true;
                            }
                        }
                    } else {
                        return value.toString().compareTo(tc.getValue()) <= 0;
                    }
                } else if (type == 1) {
                    if (value instanceof Collection) {
                        Collection<?> list = (Collection<?>) value;
                        for (Object val : list) {
                            if (NumberUtils.toInt(val.toString()) <= NumberUtils.toInt(tc.getValue())) {
                                return true;
                            }
                        }
                    } else {
                        return NumberUtils.toInt(value.toString()) <= NumberUtils.toInt(tc.getValue());
                    }
                } else {
                    if (value instanceof Collection) {
                        Collection<?> list = (Collection<?>) value;
                        for (Object val : list) {
                            if (NumberUtils.toFloat(val.toString()) <= NumberUtils.toFloat(tc.getValue())) {
                                return true;
                            }
                        }
                    } else {
                        return NumberUtils.toFloat(value.toString()) <= NumberUtils.toFloat(tc.getValue());
                    }
                }
                break;
            case "=":
                if (type == 0) {
                    if (value instanceof Collection) {
                        Collection<?> list = (Collection<?>) value;
                        for (Object val : list) {
                            if (val.toString().compareTo(tc.getValue()) == 0) {
                                return true;
                            }
                        }
                    } else {
                        return value.toString().compareTo(tc.getValue()) == 0;
                    }
                } else if (type == 1) {
                    if (value instanceof Collection) {
                        Collection<?> list = (Collection<?>) value;
                        for (Object val : list) {
                            if (NumberUtils.toInt(val.toString()) == NumberUtils.toInt(tc.getValue())) {
                                return true;
                            }
                        }
                    } else {
                        return NumberUtils.toInt(value.toString()) == NumberUtils.toInt(tc.getValue());
                    }
                } else {
                    if (value instanceof Collection) {
                        Collection<?> list = (Collection<?>) value;
                        for (Object val : list) {
                            if (NumberUtils.toFloat(val.toString()) == NumberUtils.toFloat(tc.getValue())) {
                                return true;
                            }
                        }
                    } else {
                        return NumberUtils.toFloat(value.toString()) == NumberUtils.toFloat(tc.getValue());
                    }
                }
                break;
            case "!=":
                if (type == 0) {
                    if (value instanceof Collection) {
                        Collection<?> list = (Collection<?>) value;
                        for (Object val : list) {
                            if (val.toString().compareTo(tc.getValue()) != 0) {
                                return true;
                            }
                        }
                    } else {
                        return value.toString().compareTo(tc.getValue()) != 0;
                    }
                } else if (type == 1) {
                    if (value instanceof Collection) {
                        Collection<?> list = (Collection<?>) value;
                        for (Object val : list) {
                            if (NumberUtils.toInt(val.toString()) == NumberUtils.toInt(tc.getValue())) {
                                return false;
                            }
                        }
                    } else {
                        return NumberUtils.toInt(value.toString()) != NumberUtils.toInt(tc.getValue());
                    }
                } else {
                    if (value instanceof Collection) {
                        Collection<?> list = (Collection<?>) value;
                        for (Object val : list) {
                            if (NumberUtils.toFloat(val.toString()) != NumberUtils.toFloat(tc.getValue())) {
                                return true;
                            }
                        }
                    } else {
                        return NumberUtils.toFloat(value.toString()) != NumberUtils.toFloat(tc.getValue());
                    }
                }
                break;
            case "in":
                if (type == 0) {
                    Set<String> tcVals = new HashSet<>(tc.getValuesList());
                    if (value instanceof Collection) {
                        Collection<?> list = (Collection<?>) value;
                        tcVals.retainAll(list.stream().map(Object::toString).collect(Collectors.toSet()));
                        return !tcVals.isEmpty();
                    } else {
                        return tcVals.contains(value.toString());
                    }
                } else if (type == 1) {
                    Set<Integer> tcVals = tc.getValuesList().stream().map(NumberUtils::toInt).collect(Collectors.toSet());
                    if (value instanceof Collection) {
                        Collection<?> list = (Collection<?>) value;
                        tcVals.retainAll(list.stream().map(o->NumberUtils.toInt(o.toString())).collect(Collectors.toSet()));
                        return !tcVals.isEmpty();
                    } else {
                        return tcVals.contains(NumberUtils.toInt(value.toString()));
                    }
                } else {
                    Set<Float> tcVals = tc.getValuesList().stream().map(NumberUtils::toFloat).collect(Collectors.toSet());
                    if (value instanceof Collection) {
                        Collection<?> list = (Collection<?>) value;
                        tcVals.retainAll(list.stream().map(o->NumberUtils.toFloat(o.toString())).collect(Collectors.toSet()));
                        return !tcVals.isEmpty();
                    } else {
                        return tcVals.contains(NumberUtils.toFloat(value.toString()));
                    }
                }
            case "notin":
                if (type == 0) {
                    Set<String> tcVals = new HashSet<>(tc.getValuesList());
                    if (value instanceof Collection) {
                        Collection<?> list = (Collection<?>) value;
                        tcVals.retainAll(list.stream().map(Object::toString).collect(Collectors.toSet()));
                        return tcVals.isEmpty();
                    } else {
                        return !tcVals.contains(value.toString());
                    }
                } else if (type == 1) {
                    Set<Integer> tcVals = tc.getValuesList().stream().map(NumberUtils::toInt).collect(Collectors.toSet());
                    if (value instanceof Collection) {
                        Collection<?> list = (Collection<?>) value;
                        tcVals.retainAll(list.stream().map(o->NumberUtils.toInt(o.toString())).collect(Collectors.toSet()));
                        return tcVals.isEmpty();
                    } else {
                        return !tcVals.contains(NumberUtils.toInt(value.toString()));
                    }
                } else {
                    Set<Float> tcVals = tc.getValuesList().stream().map(NumberUtils::toFloat).collect(Collectors.toSet());
                    if (value instanceof Collection) {
                        Collection<?> list = (Collection<?>) value;
                        tcVals.retainAll(list.stream().map(o->NumberUtils.toFloat(o.toString())).collect(Collectors.toSet()));
                        return tcVals.isEmpty();
                    } else {
                        return !tcVals.contains(NumberUtils.toFloat(value.toString()));
                    }
                }
        }
        return true;
    }
}
