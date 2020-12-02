// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

import com.adtiming.om.server.service.LogService;

import java.util.Map;

public class ConversionDataRequest extends CommonRequest {

    // [0: install, 1:retargeting]
    private int type;
    // conversion data
    private Map<String, Object> cd;
    private int pubAppId;
    private int publisherId;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Map<String, Object> getCd() {
        return cd;
    }

    public void setCd(Map<String, Object> cd) {
        this.cd = cd;
    }

    public int getPubAppId() {
        return pubAppId;
    }

    public void setPubAppId(int pubAppId) {
        this.pubAppId = pubAppId;
    }

    public int getPublisherId() {
        return publisherId;
    }

    public void setPublisherId(int publisherId) {
        this.publisherId = publisherId;
    }

    public void writeToLog(LogService logService) {
        logService.write("om.cd", this);
    }
}
