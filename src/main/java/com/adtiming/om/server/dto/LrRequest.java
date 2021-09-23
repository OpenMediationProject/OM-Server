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
//    public static final int TYPE_HB_REQUEST = 10; // hb interface request

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
//    private Float price;  // for bid response & win
    private int adType;

    // 2020-07-20
    private int bid;       // if data is bidding releated: [0,1]

    // 2020-12-01,  @since 2.0, add metrics besides types
    private Integer init;
    private Integer wfReq;
    private Integer wfFil;
    private Integer insReq;
    private Integer insFil;
    private Integer vdStart;
    private Integer vdEnd;
    private Integer calledShow;
    private Integer readyTrue;
    private Integer readyFalse;
    private Integer click;
    private Integer impr;
    private Integer bidReq;
    private Integer bidRes;
    private Float bidResPrice;
    private Integer bidWin;
    private Float bidWinPrice;

    // 2021-03-04
    private int ruleId;    // Mediation Rule ID for instance req|fill|impr|click
    private float revenue; // Instance Impression Revenue
    private int rp;        // Revenue Precision0:undisclosed,1:exact,2:estimated,3:defined
    private int ii;        // Instance Priority
    private int ruleType = 1; // mediation rule的优化类型，0:Manual 1:Auto
    private String adnPk;  // Adn Placement Key

    private int plReq;               // PayloadRequest
    private int plSuccess;           // PayloadSuccess
    private int plFail;              // PayloadFail

    //2021-09-10
    private int abtId;               // A/B Test ID

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

//    public Float getPrice() {
//        return price;
//    }
//
//    public void setPrice(Float price) {
//        this.price = price;
//    }

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

    public Integer getInit() {
        return init;
    }

    public void setInit(Integer init) {
        this.init = init;
    }

    public Integer getWfReq() {
        return wfReq;
    }

    public void setWfReq(Integer wfReq) {
        this.wfReq = wfReq;
    }

    public Integer getWfFil() {
        return wfFil;
    }

    public void setWfFil(Integer wfFil) {
        this.wfFil = wfFil;
    }

    public Integer getInsReq() {
        return insReq;
    }

    public void setInsReq(Integer insReq) {
        this.insReq = insReq;
    }

    public Integer getInsFil() {
        return insFil;
    }

    public void setInsFil(Integer insFil) {
        this.insFil = insFil;
    }

    public Integer getVdStart() {
        return vdStart;
    }

    public void setVdStart(Integer vdStart) {
        this.vdStart = vdStart;
    }

    public Integer getVdEnd() {
        return vdEnd;
    }

    public void setVdEnd(Integer vdEnd) {
        this.vdEnd = vdEnd;
    }

    public Integer getCalledShow() {
        return calledShow;
    }

    public void setCalledShow(Integer calledShow) {
        this.calledShow = calledShow;
    }

    public Integer getReadyTrue() {
        return readyTrue;
    }

    public void setReadyTrue(Integer readyTrue) {
        this.readyTrue = readyTrue;
    }

    public Integer getReadyFalse() {
        return readyFalse;
    }

    public void setReadyFalse(Integer readyFalse) {
        this.readyFalse = readyFalse;
    }

    public Integer getClick() {
        return click;
    }

    public void setClick(Integer click) {
        this.click = click;
    }

    public Integer getImpr() {
        return impr;
    }

    public void setImpr(Integer impr) {
        this.impr = impr;
    }

    public Integer getBidReq() {
        return bidReq;
    }

    public void setBidReq(Integer bidReq) {
        this.bidReq = bidReq;
    }

    public Integer getBidRes() {
        return bidRes;
    }

    public void setBidRes(Integer bidRes) {
        this.bidRes = bidRes;
    }

    public Float getBidResPrice() {
        return bidResPrice;
    }

    public void setBidResPrice(Float bidResPrice) {
        this.bidResPrice = bidResPrice;
    }

    public Integer getBidWin() {
        return bidWin;
    }

    public void setBidWin(Integer bidWin) {
        this.bidWin = bidWin;
    }

    public Float getBidWinPrice() {
        return bidWinPrice;
    }

    public void setBidWinPrice(Float bidWinPrice) {
        this.bidWinPrice = bidWinPrice;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getRuleId() {
        return ruleId;
    }

    public void setRuleId(int ruleId) {
        this.ruleId = ruleId;
    }

    public float getRevenue() {
        return revenue;
    }

    public void setRevenue(float revenue) {
        this.revenue = revenue;
    }

    public int getRp() {
        return rp;
    }

    public void setRp(int rp) {
        this.rp = rp;
    }

    public int getIi() {
        return ii;
    }

    public void setIi(int ii) {
        this.ii = ii;
    }

    public int getRuleType() {
        return ruleType;
    }

    public void setRuleType(int ruleType) {
        this.ruleType = ruleType;
    }

    public String getAdnPk() {
        return adnPk;
    }

    public void setAdnPk(String adnPk) {
        this.adnPk = adnPk;
    }

    public int getPlReq() {
        return plReq;
    }

    public void setPlReq(int plReq) {
        this.plReq = plReq;
    }

    public int getPlSuccess() {
        return plSuccess;
    }

    public void setPlSuccess(int plSuccess) {
        this.plSuccess = plSuccess;
    }

    public int getPlFail() {
        return plFail;
    }

    public void setPlFail(int plFail) {
        this.plFail = plFail;
    }

    public int getAbtId() {
        return abtId;
    }

    public LrRequest setAbtId(int abtId) {
        this.abtId = abtId;
        return this;
    }

    public void writeToLog(LogService logService) {
        logService.write("om.lr", this);
    }
}
