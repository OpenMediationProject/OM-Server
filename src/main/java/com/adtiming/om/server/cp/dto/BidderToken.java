package com.adtiming.om.server.cp.dto;

public class BidderToken {

    public String sdkv;
    public int fit;
    public int flt;
    public float iap;
    public String session;
    public String uid;
    public String did;
    public int dtype; // 设备ID类型, 1:IDFA, 2:GAID, 3:FBID, 4:OAID
    public String afid; // appsflyer User ID
    public int ng;    // No GooglePlay
    public int zo;    // 时区偏移量,单位分钟. 比如北京时区 zo=480
    public int jb;    // jailbreak 状态, 0:正常,1:越狱, 正常时不传
    public String brand; // 手机品牌
    public int fm;     // 剩余硬盘空间, 单位M
    public int battery;// 剩余电量百分比
    public int btch;   // 是否充电中, 0:否,1:是
    public int lowp;   // 是否低电量模式, 0:否,1:是
    public String lcy; // local country

}
