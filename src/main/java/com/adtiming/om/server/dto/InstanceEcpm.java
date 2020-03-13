// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

public class InstanceEcpm {
    public int instanceId;
    public Float ecpm;
    public String dataLevel;

    public InstanceEcpm(int instanceId, Float ecpm, String dataLevel) {
        this.instanceId = instanceId;
        this.ecpm = ecpm;
        this.dataLevel = dataLevel;
    }
}
