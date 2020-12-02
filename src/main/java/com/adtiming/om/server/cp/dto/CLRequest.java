package com.adtiming.om.server.cp.dto;

import com.adtiming.om.server.dto.CommonRequest;
import com.adtiming.om.server.dto.Placement;
import com.adtiming.om.server.dto.PublisherApp;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class CLRequest extends CommonRequest implements AdRequest {

    private int pid;       //  广告位ID
    private float iap;     //  IAP, inAppPurchase
    private int imprTimes; //  placementImprTimes 用户当天该广告位展示次数
    private int ng;        //  noGooglePlay标识,<br>0或不传表示已安装GP,<br>1:未安装GP.<br>当ng=1时只投CPL的活动
    private int act;       //  加载请求触发类型, [1:init,2:interval,3:adclose,4:manual]

    private String country;

    // load from cache
    private Placement placement;
    private PublisherApp pubApp;
    private boolean test;
    private boolean requireSkAdNetwork;

    // for response
    private int iid;
    private int abt;           // abTest
    private String bidid;

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public float getIap() {
        return iap;
    }

    public void setIap(float iap) {
        this.iap = iap;
    }

    public int getImprTimes() {
        return imprTimes;
    }

    public void setImprTimes(int imprTimes) {
        this.imprTimes = imprTimes;
    }

    public int getNg() {
        return ng;
    }

    public void setNg(int ng) {
        this.ng = ng;
    }

    public int getAct() {
        return act;
    }

    public void setAct(int act) {
        this.act = act;
    }

    public String getCountry() {
        return country == null ? super.getCountry() : country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setIid(int iid) {
        this.iid = iid;
    }

    public int getIid() {
        return iid;
    }

    public Placement getPlacement() {
        return placement;
    }

    @JsonIgnore
    public void setPlacement(Placement placement) {
        this.placement = placement;
        super.setPlacement(placement);
    }

    public PublisherApp getPubApp() {
        return pubApp;
    }

    @JsonIgnore
    public void setPubApp(PublisherApp pubApp) {
        this.pubApp = pubApp;
        super.setPubApp(pubApp);
    }

    public int getAbt() {
        return abt;
    }

    public void setAbt(int abt) {
        this.abt = abt;
    }

    public String getBidid() {
        return bidid;
    }

    public void setBidid(String bidid) {
        this.bidid = bidid;
    }

    @Override
    public boolean isTest() {
        return test;
    }

    public void setTest(boolean test) {
        this.test = test;
    }

    @Override
    public boolean isRequireSkAdNetwork() {
        return requireSkAdNetwork;
    }

    public void setRequireSkAdNetwork(boolean requireSkAdNetwork) {
        this.requireSkAdNetwork = requireSkAdNetwork;
    }
}
