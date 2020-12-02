// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.cp.dto;

import com.adtiming.om.server.dto.CommonRequest;
import com.adtiming.om.server.dto.Placement;
import com.adtiming.om.server.dto.PublisherApp;

public class BidPayloadRequest extends CommonRequest implements AdRequest {

    private int pid;        //  placement ID
    private String token;   //  payload token

    // from payload token
    private BidPayloadToken payload;

    // not from json, server added
    // load from cache
    private Placement placement;
    private PublisherApp pubApp;
    private boolean requireSkAdNetwork;

    @Override
    public String getBidid() {
        return payload.getBidid();
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
        this.payload = BidPayloadToken.parseFromTokenString(token);
    }

    public BidPayloadToken getPayload() {
        return payload;
    }

    @Override
    public void setPlacement(Placement placement) {
        this.placement = placement;
    }

    @Override
    public Placement getPlacement() {
        return placement;
    }

    @Override
    public void setPubApp(PublisherApp pubApp) {
        this.pubApp = pubApp;
    }

    @Override
    public PublisherApp getPubApp() {
        return pubApp;
    }

    @Override
    public boolean isTest() {
        return payload.getTest() != null && payload.getTest() == 1;
    }

    @Override
    public boolean isRequireSkAdNetwork() {
        return requireSkAdNetwork;
    }

    public void setRequireSkAdNetwork(boolean requireSkAdNetwork) {
        this.requireSkAdNetwork = requireSkAdNetwork;
    }
}
