package com.adtiming.om.server.cp.dto;

import com.adtiming.om.server.dto.DeviceInfo;
import com.adtiming.om.server.dto.Placement;
import com.adtiming.om.server.dto.PublisherApp;
import com.fasterxml.jackson.annotation.JsonIgnore;

public interface AdRequest extends DeviceInfo {

    String getReqId();

    long getTs();

    long getServerTs();

    String getBidid();

    String getReqHost();

    int getPid();

    String getSdkv();

    String getUa();

    @JsonIgnore
    Placement getPlacement();

    @JsonIgnore
    PublisherApp getPubApp();

    int getPublisherId();

    int getPubAppId();

    @JsonIgnore
    boolean isEmptyDid();

    @JsonIgnore
    boolean isRequireSkAdNetwork();

    @JsonIgnore
    boolean isTest();

    int getWidth();

    int getHeight();
}
