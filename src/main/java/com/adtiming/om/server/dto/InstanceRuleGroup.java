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
    private int abTest;
    private Map<Integer, Map<Integer, Integer>> abtInsPriority = Collections.emptyMap();

    public InstanceRuleGroup(AdNetworkPB.InstanceRuleGroup group) {
        this.ruleId = group.getRuleId();
        this.groupId = group.getGroupId();
        this.groupLevel = group.getGroupLevel();
        this.autoSwitch = group.getAutoSwitch();
        this.abTest = group.getAbTest();
        if (group.getInstanceWeightCount() > 0) {
            abtInsPriority = new HashMap<>(group.getInstanceWeightCount());
            group.getInstanceWeightList().forEach(iw-> abtInsPriority.computeIfAbsent(iw.getAbTest(), k -> new HashMap<>()).put(iw.getInstanceId(), iw.getPriority()));
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

    public Map<Integer, Map<Integer, Integer>> getAbtInsPriority() {
        return abtInsPriority;
    }

    public Map<Integer, Integer> getInsPriority(int abt) {
        return abtInsPriority.getOrDefault(abt, abtInsPriority.get(0));
    }

    public int getAbTest() {
        return abTest;
    }

    public Set<Integer> getInsList(int abt) {
        if (abtInsPriority.isEmpty()) return Collections.emptySet();
        Map<Integer, Integer> insPri = abtInsPriority.getOrDefault(abt, Collections.emptyMap());
        return insPri.keySet();
    }
}
