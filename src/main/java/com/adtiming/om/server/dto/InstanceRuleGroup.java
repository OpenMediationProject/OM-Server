package com.adtiming.om.server.dto;

import com.adtiming.om.pb.AdNetworkPB;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class InstanceRuleGroup {
    private int ruleId;
    private int groupId;
    private int groupLevel;
    private int autoSwitch;
    private Map<Integer, Integer> insPriority = Collections.emptyMap();

    public InstanceRuleGroup(AdNetworkPB.InstanceRuleGroup group) {
        this.ruleId = group.getRuleId();
        this.groupId = group.getGroupId();
        this.groupLevel = group.getGroupLevel();
        this.autoSwitch = group.getAutoSwitch();
        if (group.getInstanceWeightCount() > 0) {
            insPriority = new HashMap<>(group.getInstanceWeightCount());
            group.getInstanceWeightList().forEach(iw-> insPriority.put(iw.getInstanceId(), iw.getPriority()));
        }
    }

    public int getRuleId() {
        return ruleId;
    }

    public InstanceRuleGroup setRuleId(int ruleId) {
        this.ruleId = ruleId;
        return this;
    }

    public int getGroupId() {
        return groupId;
    }


    public int getGroupLevel() {
        return groupLevel;
    }

    public int getAutoSwitch() {
        return autoSwitch;
    }

    public Map<Integer, Integer> getInsPriority() {
        return insPriority;
    }

    public Set<Integer> getInsList() {
        if (insPriority.isEmpty()) return Collections.emptySet();
        return insPriority.keySet();
    }
}
