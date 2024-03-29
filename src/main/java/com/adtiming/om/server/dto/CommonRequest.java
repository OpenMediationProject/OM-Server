// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

import com.adtiming.om.server.service.AppConfig;
import com.adtiming.om.server.util.Util;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.beans.BeanUtils;

import java.util.Map;
import java.util.UUID;

/**
 * BasicRequestFields
 */
public class CommonRequest implements DeviceInfo {

    private long ts;          //  Client time in milliseconds
    private int fit;          //  Client first install App time, in seconds
    private int flt;          //  The first time the client opens the App, in seconds (uid file creation time)
    private int zo;           //  Time zone offset in minutes. For example, Beijing time zone zo = 480
    private String session;   //  Session ID, UUID generated when the app is initialized
    private String uid;       //  User ID, the unique user ID generated by the SDK
    private String did;       //  Device ID, corresponding to dtype
    private int dtype;        //  Device ID type, 1:IDFA, 2:GAID, 3:FBID, 4:HUAWEIID
    private String lang;      //  language code
    private String langname;  //  language name
    private int jb;           //  jailbreak status, 0: normal, 1: jailbreak, no transmission during normal
    private String bundle;    //  The current app package name
    private String make;      //  device maker
    private String brand;     //  device brands
    private String model;     //  device model
    private String build;     //  build number, Android: ro.build.display.id
    private String osv;       //  os version
    private String appv;      //  app version
    private int width;        //  screen or placement width
    private int height;       //  screen or placement height
    private int contype;      //  ConnectionType
    private Carrier carrier;  //  NetworkOperatorName
    private String lip;       //  local ip
    private String lcountry;  //  [[NSLocale currentLocale]localeIdentifier], Locale.getCountry()
    private int fm;           //  Free hard disk space, unit M
    private int battery;      //  Remaining battery percentage
    private int btch;         //  Whether charging, 0: No, 1: Yes
    private int lowp;         //  Low battery mode, 0:No,1:Yes
    private int abt;
    private String cnl;       //  Channel
    private Regs regs;        //  Regs
    private Integer age;      //  age
    private Integer gender;   //  gender, {0: unknown, 1: male, 2: female}
    private String cdid;      //  Custom Device ID
    private Map<String, Object> tags; // User tags
    private Integer atts;            // The status value for app tracking authorization.0 = not determined,1 = restricted,2 = denied,3 = authorized
    private String gcy;       //  TelephonyManager.getNetworkCountryIso()

    // not from request params
    // set by api controller
    private GeoData geo;
    private String reqHost;
    private int plat;
    private String sdkv;
    private Version sdkVersion;
    private Version osVersion;
    private int apiv;
    private String reqId = UUID.randomUUID().toString();
    private long serverTs = System.currentTimeMillis();
    private String ua;
    private int dcenter;   // data center id
    private int snode;     // server node id
    private int publisherId;
    private int pubAppId;
    /**
     * @see DeviceInfo#getMtype()
     */
    private int mtype;     // model type
    private boolean emptyDid;

    public long getTs() {
        return ts;
    }

    public void setTs(long ts) {
        this.ts = ts;
    }

    public int getFit() {
        return fit;
    }

    public void setFit(int fit) {
        this.fit = fit;
    }

    public int getFlt() {
        return flt;
    }

    public void setFlt(int flt) {
        this.flt = flt;
    }

    public int getZo() {
        return zo;
    }

    public void setZo(int zo) {
        this.zo = zo;
    }

    @Override
    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    @Override
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    @Override
    public String getDid() {
        return did;
    }

    public void setDid(String did) {
        this.did = did;
        this.emptyDid = Util.isEmptyDid(did);
    }

    @JsonIgnore
    public boolean isEmptyDid() {
        return emptyDid;
    }

    @Override
    public int getDtype() {
        return dtype;
    }

    public void setDtype(int dtype) {
        this.dtype = dtype;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getLangname() {
        return langname;
    }

    public void setLangname(String langname) {
        this.langname = langname;
    }

    public int getJb() {
        return jb;
    }

    public void setJb(int jb) {
        this.jb = jb;
    }

    public String getBundle() {
        return bundle;
    }

    public void setBundle(String bundle) {
        this.bundle = bundle;
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

    public String getBuild() {
        return build;
    }

    public void setBuild(String build) {
        this.build = build;
    }

    public String getOsv() {
        return osv;
    }

    public void setOsv(String osv) {
        this.osv = osv;
        this.osVersion = Version.of(osv);
    }

    @JsonIgnore
    public Version getOsVersion() {
        return osVersion;
    }

    public String getAppv() {
        return appv;
    }

    public void setAppv(String appv) {
        this.appv = appv;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getContype() {
        return contype;
    }

    public void setContype(int contype) {
        this.contype = contype;
    }

    @Override
    public String getCarrier() {
        return carrier == null ? null : carrier.getName();
    }

    @Override
    public String getMccmnc() {
        return carrier == null ? null : carrier.getMccmnc();
    }

    public void setCarrier(String carrier) {
        this.carrier = Carrier.parseFrom(carrier);
    }

    public void setCarrier(Carrier carrier) {
        this.carrier = carrier;
    }

    public String getLip() {
        return lip;
    }

    public void setLip(String lip) {
        this.lip = lip;
    }

    public String getLcountry() {
        return lcountry;
    }

    public void setLcountry(String lcountry) {
        this.lcountry = lcountry;
    }

    public int getFm() {
        return fm;
    }

    public void setFm(int fm) {
        this.fm = fm;
    }

    public int getBattery() {
        return battery;
    }

    public void setBattery(int battery) {
        this.battery = battery;
    }

    public int getBtch() {
        return btch;
    }

    public void setBtch(int btch) {
        this.btch = btch;
    }

    public int getLowp() {
        return lowp;
    }

    public void setLowp(int lowp) {
        this.lowp = lowp;
    }

    @JsonIgnore
    public GeoData getGeo() {
        return geo;
    }

    public void setGeo(GeoData geo) {
        this.geo = geo;
    }

    @Override
    public String getIp() {
        return geo.getIp();
    }

    public String getCountry() {
        return geo.getCountry();
    }

    @Override
    public String getRegion() {
        return geo.getRegion();
    }

    @Override
    public String getCity() {
        return geo.getCity();
    }

    public int getPlat() {
        return plat;
    }

    public void setPlat(int plat) {
        this.plat = plat;
    }

    public String getSdkv() {
        return sdkv;
    }

    public void setSdkv(String sdkv) {
        this.sdkv = sdkv;
        this.sdkVersion = Version.of(sdkv);
    }

    @JsonIgnore
    public Version getSdkVersion() {
        return sdkVersion;
    }

    public void setSdkVersion(Version sdkVersion) {
        this.sdkVersion = sdkVersion;
    }

    public int getApiv() {
        return apiv;
    }

    public void setApiv(int apiVersion) {
        this.apiv = apiVersion;
    }

    public String getReqId() {
        return reqId;
    }

    public void setReqId(String reqId) {
        this.reqId = reqId;
    }

    public long getServerTs() {
        return serverTs;
    }

    public void setServerTs(long serverTs) {
        this.serverTs = serverTs;
    }

    public void setUa(String ua) {
        this.ua = ua;
    }

    public String getUa() {
        return ua;
    }

    public void setAbt(int abt) {
        this.abt = abt;
    }

    @JsonIgnore
    public String getReqHost() {
        return reqHost;
    }

    public void setReqHost(String reqHost) {
        this.reqHost = reqHost;
    }

    @Override
    public int getAbt() {
        return abt;
    }

    public int getDcenter() {
        return dcenter;
    }

    public void setDcenter(int dcenter) {
        this.dcenter = dcenter;
    }

    public int getSnode() {
        return snode;
    }

    public void setSnode(int snode) {
        this.snode = snode;
    }

    public String getCnl() {
        return cnl;
    }

    public void setCnl(String cnl) {
        this.cnl = cnl;
    }

    @Override
    public int getMtype() {
        return mtype;
    }

    public void setMtype(int mtype) {
        this.mtype = mtype;
    }

    @Override
    public Regs getRegs() {
        return regs;
    }

    public void setRegs(Regs regs) {
        this.regs = regs;
    }

    @Override
    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    @Override
    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    @Override
    public String getCdid() {
        return cdid;
    }

    public void setCdid(String cdid) {
        this.cdid = cdid;
    }

    @Override
    public Map<String, Object> getTags() {
        return tags;
    }

    public void setTags(Map<String, Object> tags) {
        this.tags = tags;
    }

    public Integer getAtts() {
        return atts;
    }

    public void setAtts(Integer atts) {
        this.atts = atts;
    }

    public String getGcy() {
        return gcy;
    }

    public void setGcy(String gcy) {
        this.gcy = gcy;
    }

    @JsonIgnore
    public void setAppConfig(AppConfig cfg) {
        this.dcenter = cfg.getDcenter();
        this.snode = cfg.getSnode();
    }

    @JsonIgnore
    public void setPubApp(PublisherApp pubApp) {
        if (pubApp == null)
            return;
        this.publisherId = pubApp.getPublisherId();
        this.pubAppId = pubApp.getId();
    }

    @JsonIgnore
    public void setPlacement(Placement placement) {
        if (placement == null)
            return;
        this.publisherId = placement.getPublisherId();
        this.pubAppId = placement.getPubAppId();
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

    public <T extends CommonRequest> T copyTo(T other) {
        BeanUtils.copyProperties(this, other, "carrier");
        other.setCarrier(this.carrier);
        return other;
    }

}
