// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

import java.math.BigDecimal;

public class InstanceEcpm {
    public int instanceId;
    public Float ecpm;
    public String dataLevel;
    public Float cov;  //离散系数 coefficient of variation
    public BigDecimal smoothParams; //平滑系数
    public BigDecimal[] ecpmDatas;  // 7天Ecpm数据
    public InstanceEcpm(int instanceId, Float ecpm, String dataLevel) {
        this.instanceId = instanceId;
        this.ecpm = ecpm;
        this.dataLevel = dataLevel;
    }

    public InstanceEcpm(int instanceId, Float ecpm, String dataLevel, Float cov,
                        BigDecimal smoothParams, BigDecimal[] ecpmDatas) {
        this.instanceId = instanceId;
        this.ecpm = ecpm;
        this.dataLevel = dataLevel;
        this.cov = cov;
        this.smoothParams = smoothParams;
        this.ecpmDatas = ecpmDatas;
    }
}
