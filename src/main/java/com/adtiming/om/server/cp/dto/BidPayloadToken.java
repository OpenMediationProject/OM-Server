// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.cp.dto;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class BidPayloadToken {

    private static final byte[] SIGN_SECRET = {22, 42, -54, 13, -32, -82, -63, 75, 90, -93, 58, -56, 119, 71, -18, 97};

    private long ts;        // request unix timestamp
    private Integer test;   // is test, 0 or 1
    private String bidid;
    private String reqid;
    private long cid;       // campaignID
    private long crid;      // creativeID
    private float price;    // bid price

    private Long icon;      // icon material id
    private List<Long> img; // image material id list
    private Long vd;        // video material id
    private Integer vdt;    // videoTemplate
    private Integer ect;    // endcardTemplate

    private String sign;    // sign

    public String toTokenString() {
        this.sign = buildSign();
        byte[] json = JSON.toJSONBytes(this);
        return "V1_" + Base64.encodeBase64String(json);
    }

    public static BidPayloadToken parseFromTokenString(String token) {
        return JSON.parseObject(Base64.decodeBase64(token.substring(3)), BidPayloadToken.class);
    }

    public String buildSign() {
        byte[] bididBytes = bidid.getBytes(UTF_8);
        byte[] priceBytes = new BigDecimal(price).setScale(2, RoundingMode.DOWN).toString().getBytes(UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(bididBytes.length + priceBytes.length + 24 + SIGN_SECRET.length);
        buf.put(bididBytes).putLong(cid).putLong(crid).put(priceBytes).putLong(ts).put(SIGN_SECRET);
        return DigestUtils.md5Hex(buf.array());
    }

    @JSONField(serialize = false, deserialize = false)
    public boolean isSignError() {
        return !buildSign().equals(sign);
    }

    public Integer getTest() {
        return test;
    }

    public BidPayloadToken setTest(Integer test) {
        this.test = test;
        return this;
    }

    public long getTs() {
        return ts;
    }

    public BidPayloadToken setTs(long ts) {
        this.ts = ts;
        return this;
    }

    public String getBidid() {
        return bidid;
    }

    public BidPayloadToken setBidid(String bidid) {
        this.bidid = bidid;
        return this;
    }

    public String getReqid() {
        return reqid;
    }

    public BidPayloadToken setReqid(String reqid) {
        this.reqid = reqid;
        return this;
    }

    public float getPrice() {
        return price;
    }

    public BidPayloadToken setPrice(float price) {
        this.price = price;
        return this;
    }

    public long getCid() {
        return cid;
    }

    public BidPayloadToken setCid(long cid) {
        this.cid = cid;
        return this;
    }

    public long getCrid() {
        return crid;
    }

    public BidPayloadToken setCrid(long crid) {
        this.crid = crid;
        return this;
    }

    public Long getIcon() {
        return icon;
    }

    public BidPayloadToken setIcon(Long icon) {
        this.icon = icon;
        return this;
    }

    public List<Long> getImg() {
        return img;
    }

    public BidPayloadToken setImg(List<Long> img) {
        this.img = img;
        return this;
    }

    public BidPayloadToken addImg(Long img) {
        if (this.img == null) {
            this.img = new ArrayList<>(5);
        }
        this.img.add(img);
        return this;
    }

    public Long getVd() {
        return vd;
    }

    public BidPayloadToken setVd(Long vd) {
        this.vd = vd;
        return this;
    }

    public Integer getVdt() {
        return vdt;
    }

    public BidPayloadToken setVdt(Integer vdt) {
        this.vdt = vdt;
        return this;
    }

    public Integer getEct() {
        return ect;
    }

    public BidPayloadToken setEct(Integer ect) {
        this.ect = ect;
        return this;
    }

    public String getSign() {
        return sign;
    }

    public BidPayloadToken setSign(String sign) {
        this.sign = sign;
        return this;
    }
}
