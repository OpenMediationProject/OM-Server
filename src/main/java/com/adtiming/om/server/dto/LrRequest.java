// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

import com.adtiming.om.server.service.LogService;

public class LrRequest extends CommonRequest {

    public static final int TYPE_INIT = 1;
    public static final int TYPE_WATERFALL_REQUEST = 2;  // waterfall REQUEST
    public static final int TYPE_WATERFALL_FILLED = 3; // waterfall FILLED
    public static final int TYPE_INSTANCE_REQUEST = 4;   // instance REQUEST
    public static final int TYPE_INSTANCE_FILLED = 5;  // instance FILLED
    public static final int TYPE_INSTANCE_IMPR = 6;   // instance impr
    public static final int TYPE_INSTANCE_CLICK = 7;  // instance click
    public static final int TYPE_VIDEO_START = 8;     // video start
    public static final int TYPE_VIDEO_COMPLETE = 9;  // video complete
    public static final int TYPE_HB_REQUEST = 10; // hb interface request

    private int type;      // type
    private int pid;       // Placement ID
    private int mid;       // AdNetwork ID
    private int iid;       // InstanceID
    private int act;       // Load request trigger type, [1:init,2:interval,3:adclose,4:manual]
    private int scene;     // Scene ID

    // not from json
    private int status;
    private String msg;

    // 2020-07-07
    private Float price;  // for bid response & win
    private int adType;

    // 2020-07-20
    private int bid;       // if data is bidding releated: [0,1]

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public int getMid() {
        return mid;
    }

    public void setMid(int mid) {
        this.mid = mid;
    }

    public int getIid() {
        return iid;
    }

    public void setIid(int iid) {
        this.iid = iid;
    }

    public int getAct() {
        return act;
    }

    public void setAct(int act) {
        this.act = act;
    }

    public int getScene() {
        return scene;
    }

    public void setScene(int scene) {
        this.scene = scene;
    }

    public int getStatus() {
        return status;
    }

    public LrRequest setStatus(int status, String msg) {
        this.status = status;
        this.msg = msg;
        return this;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Float getPrice() {
        return price;
    }

    public void setPrice(Float price) {
        this.price = price;
    }

    public int getAdType() {
        return adType;
    }

    public void setAdType(int adType) {
        this.adType = adType;
    }

    public int getBid() {
        return bid;
    }

    public void setBid(int bid) {
        this.bid = bid;
    }

    public void writeToLog(LogService logService) {
        logService.write("om.lr", this);
    }
}
