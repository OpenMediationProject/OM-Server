// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

import com.adtiming.om.server.service.LogService;

/**
 * IapRequest
 */
public class IapRequest extends CommonRequest {

    private String cur; // currency
    private float iap;  // iap price
    private float iapt; // iap total price

    // not from json
    // set by api controller
    private float iapUsd;
    private int status;
    private String msg;

    public String getCur() {
        return cur;
    }

    public void setCur(String cur) {
        this.cur = cur;
    }

    public float getIap() {
        return iap;
    }

    public void setIap(float iap) {
        this.iap = iap;
    }

    public float getIapt() {
        return iapt;
    }

    public void setIapt(float iapt) {
        this.iapt = iapt;
    }

    public float getIapUsd() {
        return iapUsd;
    }

    public void setIapUsd(float iapUsd) {
        this.iapUsd = iapUsd;
    }

    public void writeToLog(LogService logService) {
        logService.write("om.iap", this);
    }


}
