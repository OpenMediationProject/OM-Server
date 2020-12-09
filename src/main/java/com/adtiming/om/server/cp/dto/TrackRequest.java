package com.adtiming.om.server.cp.dto;

import com.adtiming.om.server.cp.util.Util;
import com.adtiming.om.server.service.LogService;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

public class TrackRequest {

    /////////////////////////////////////////////////////////
    //////// receive from request query string //////////////
    /////////////////////////////////////////////////////////
    private String cy;                     // Country, cy
    private String make;                   // 设备产商
    private String brand;                  // 设备品牌
    private String model;                  // 设备型号
    private String osv;                    // OS Version
    private String appv;                   // app version
    private String sdkv;                   // sdk version
    private String reqid;                  // 请求ID
    private long cts;                      // client 时间戳
    private long sts;                      // server 时间戳
    private String did;                    // devicdId
    private String uid;                    // SDK 生成用户唯一标识
    private int pid;                       // Placement ID
    private long cid;                      // 活动ID
    private long crid;                     // 创意ID
    private String size;                   // ad size
    private String bundle;                 // bundle
    private int cont;                      // connection type
    private String mccmnc;                 // mccmnc
    private String carrier;                // carrier, cr
    private String lang;                   // language code
    private String bidid;                  // bid id
    private String tkid;                   // TrackID
    private String price;                  // Price
    private String sign;                   // sign, reqid+cid+price+time+secret

    /////////////////////////////////////////////////////////
    //////// server appended fields            //////////////
    /////////////////////////////////////////////////////////
    private int click;                     // click
    private int impr;                      // impression
    private long ts = System.currentTimeMillis(); // 当前时间
    private int snode;                     // Server Node ID
    private int dcenter;                   // Server Dcenter ID
    private String ip;                     // client IP
    private String ref;                    // Referrer 请求来源url
    private String ua;                     // UserAgent
    private int adPubId;                   // 活动所属 PublisherID
    private int plat;                      // 0:iOS,1:Android
    private String appId;                  // Campaign.appId
    private int publisherId;               // Placement.publisherId
    private int pubAppId;                  // Placement.pubAppId
    private int adType;                    // Placement.AdType
    private int billingType;               // Campaign.billingType
    private BigDecimal cost = BigDecimal.ZERO; // 根据 price 计算 cost

    public TrackRequest() {
    }

    @JsonIgnore
    public boolean isExpired() {
        // 6 hours
        return ts - sts > (3600 * 6 * 1000L);
    }

    @JsonIgnore
    public boolean isSignOK() {
        return StringUtils.equals(Util.buildSign(reqid, cid, price, sts), sign);
    }

    public String getCy() {
        return cy;
    }

    public void setCy(String cy) {
        this.cy = cy;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getOsv() {
        return osv;
    }

    public void setOsv(String osv) {
        this.osv = osv;
    }

    public String getAppv() {
        return appv;
    }

    public void setAppv(String appv) {
        this.appv = appv;
    }

    public String getSdkv() {
        return sdkv;
    }

    public void setSdkv(String sdkv) {
        this.sdkv = sdkv;
    }

    public String getReqid() {
        return reqid;
    }

    public void setReqid(String reqid) {
        this.reqid = reqid;
    }

    public long getCts() {
        return cts;
    }

    public void setCts(long cts) {
        this.cts = cts;
    }

    public long getSts() {
        return sts;
    }

    public void setSts(long sts) {
        this.sts = sts;
    }

    public String getDid() {
        return did;
    }

    public void setDid(String did) {
        this.did = did;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public long getCid() {
        return cid;
    }

    public void setCid(long cid) {
        this.cid = cid;
    }

    public long getCrid() {
        return crid;
    }

    public void setCrid(long crid) {
        this.crid = crid;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getBundle() {
        return bundle;
    }

    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    public int getCont() {
        return cont;
    }

    public void setCont(int cont) {
        this.cont = cont;
    }

    public String getMccmnc() {
        return mccmnc;
    }

    public void setMccmnc(String mccmnc) {
        this.mccmnc = mccmnc;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public void setCr(String carrier) {
        this.carrier = carrier;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getBidid() {
        return bidid;
    }

    public void setBidid(String bidid) {
        this.bidid = bidid;
    }

    public String getTkid() {
        return tkid;
    }

    public void setTkid(String tkid) {
        this.tkid = tkid;
    }

    @JsonIgnore
    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    @JsonIgnore
    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public int getClick() {
        return click;
    }

    public void setClick(int click) {
        this.click = click;
    }

    public int getImpr() {
        return impr;
    }

    public void setImpr(int impr) {
        this.impr = impr;
    }

    public long getTs() {
        return ts;
    }

    public void setTs(long ts) {
        this.ts = ts;
    }

    public int getSnode() {
        return snode;
    }

    public void setSnode(int snode) {
        this.snode = snode;
    }

    public int getDcenter() {
        return dcenter;
    }

    public void setDcenter(int dcenter) {
        this.dcenter = dcenter;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    @JsonIgnore
    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getUa() {
        return ua;
    }

    public void setUa(String ua) {
        this.ua = ua;
    }

    public int getAdPubId() {
        return adPubId;
    }

    public void setAdPubId(int adPubId) {
        this.adPubId = adPubId;
    }

    public int getPlat() {
        return plat;
    }

    public void setPlat(int plat) {
        this.plat = plat;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public int getPublisherId() {
        return publisherId;
    }

    public void setPublisherId(int publisherId) {
        this.publisherId = publisherId;
    }

    public int getPubAppId() {
        return pubAppId;
    }

    public void setPubAppId(int pubAppId) {
        this.pubAppId = pubAppId;
    }

    public int getAdType() {
        return adType;
    }

    public void setAdType(int adType) {
        this.adType = adType;
    }

    public int getBillingType() {
        return billingType;
    }

    public void setBillingType(int billingType) {
        this.billingType = billingType;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public void writeToLog(LogService logService) {
        logService.write("om.cptk", this);
    }
}
