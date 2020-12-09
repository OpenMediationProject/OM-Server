// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.cp.dto;

import com.adtiming.om.server.dto.Debugable;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

public class CLResponse extends Debugable<CLResponse> {

    private List<?> campaigns;
    private int code;
    private String lastReason;

    public CLResponse(boolean debug) {
        if (debug) {
            setDebug(new ArrayList<>());
        }
    }

    public List<?> getCampaigns() {
        if (campaigns != null && !campaigns.isEmpty()) {
            return campaigns;
        }
        return null;
    }

    public CLResponse setCampaigns(List<?> campaigns) {
        this.campaigns = campaigns;
        return this;
    }

    public CLResponse setCode(int code) {
        this.code = code;
        return this;
    }

    public Integer getCode() {
        return code > 0 ? code : null;
    }

    @JsonIgnore
    public String getLastReason() {
        return lastReason;
    }

    public CLResponse setLastReason(String lastReason) {
        this.lastReason = lastReason;
        return this;
    }
}
