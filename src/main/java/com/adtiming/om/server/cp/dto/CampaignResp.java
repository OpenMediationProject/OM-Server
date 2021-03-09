// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.cp.dto;

import com.adtiming.om.pb.CrossPromotionPB.App;
import com.adtiming.om.pb.CrossPromotionPB.CpMaterial;
import com.adtiming.om.server.cp.service.CpCacheService;
import com.adtiming.om.server.cp.util.ECDSAUtil;
import com.adtiming.om.server.cp.util.MacroReplaceUtil;
import com.adtiming.om.server.cp.util.ParamsBuilder;
import com.adtiming.om.server.cp.util.Util;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.adtiming.om.pb.CommonPB.AdType.CrossPromotion;
import static com.adtiming.om.pb.CommonPB.AdType.Interstitial;

public class CampaignResp {

    public static final Logger log = LogManager.getLogger();

    private static final JSONObject VES_DEFAULT = new JSONObject(2)
            .fluentPut("start", Collections.emptyList())
            .fluentPut("complete", Collections.emptyList());

    private static final Object CP_MK_VALUE = new JSONObject(2)
            .fluentPut("logo", "")
            .fluentPut("link", "");

    private final MatchedCampaign mc;
    private final MatchedCreative mcr;
    private final Campaign c;
    private final CpCacheService ccs;

    private final AdRequest req;
    private VideoRes videoRes;
    private AppRes appRes;
    private SkAdNetwork ska;
    private final String cdnOrigin;
    private final String urlQuery;

    public CampaignResp(AdRequest req, MatchedCampaign mc, CpCacheService ccs) {
        this.ccs = ccs;
        this.cdnOrigin = ccs.getCdnOrigin();
        this.mc = mc;
        this.c = mc.getCampaign();
        this.mcr = mc.getCreative();
        this.req = req;
        if (mcr.materialVideo != null) {
            this.videoRes = new VideoRes();
            this.videoRes.url = Util.buildMaterialUrl(cdnOrigin, mcr.materialVideo);
            this.videoRes.dur = mcr.materialVideo.getVideoDuration();
            this.videoRes.ves = VES_DEFAULT;
            if (req.getPlacement().getAdType() == Interstitial) {
                this.videoRes.skip = 1;
            }
        }

        App app = mc.getApp();
        if (app != null) {
            this.appRes = new AppRes(app, mcr.materialIcon, cdnOrigin);
        }

        String tkid = UUID.randomUUID().toString().replace("-", "");
        String price = String.valueOf(mc.getFinalBidPrice());
        String sign = Util.buildSign(req.getReqId(), c.getId(), price, req.getServerTs());
        ParamsBuilder ps = newParamsBuilder(req, tkid)
                .p("cid", getCid())
                .p("crid", getCrid())
                .p("price", price)
                .p("sign", sign);
        this.urlQuery = ps.format();

        if (req.isRequireSkAdNetwork() && ccs.getSkPrivateKey() != null) {
            try {
                ska = new SkAdNetwork();
                ska.adNetworkPayloadVersion = ccs.getSkAdNetworkVersion();
                ska.adNetworkId = ccs.getSkAdNetworkId();
                ska.adNetworkCampaignId = c.getSkaCampaignId();
                ska.adNetworkNonce = UUID.randomUUID().toString().toLowerCase();
                ska.adNetworkSourceAppStoreIdentifier = NumberUtils.toLong(req.getPubApp().getAppId());
                ska.adNetworkImpressionTimestamp = System.currentTimeMillis();
                String data = String.format("%s\u2063%s\u2063%d\u2063%s\u2063%s\u2063%d\u2063%d",
                        ska.adNetworkPayloadVersion, ska.adNetworkId, ska.adNetworkCampaignId, c.getAppId(),
                        ska.adNetworkNonce, ska.adNetworkSourceAppStoreIdentifier, ska.adNetworkImpressionTimestamp);
                ska.adNetworkAttributionSignature = ECDSAUtil.encodeSHA256WithECDSA(data, ccs.getSkPrivateKey());
            } catch (Exception e) {
                log.error("encodeSHA256WithECDSA error", e);
                ska = null;
            }
        }
    }

    @JsonIgnore
    public MatchedCampaign getMatchedCampaign() {
        return mc;
    }

    // CampaignID
    public String getCid() {
        return String.valueOf(c.getId());
    }

    // CreativeID
    public String getCrid() {
        return String.valueOf(mcr.creative.getId());
    }

    // Title
    public String getTitle() {
        return mcr.creative.getTitle();
    }

    // Description
    public String getDescn() {
        return mcr.creative.getDescn();
    }

    // Image List
    public List<String> getImgs() {
        if (mcr.materialImgs == null) {
            return null;
        }
        int size = mcr.materialImgs.size();
        if (size == 1) {
            return Collections.singletonList(Util.buildMaterialUrl(cdnOrigin, mcr.materialImgs.get(0)));
        }
        List<String> list = new ArrayList<>(size);
        for (CpMaterial img : mcr.materialImgs) {
            list.add(Util.buildMaterialUrl(cdnOrigin, img));
        }
        return list;
    }

    // Video Object
    public VideoRes getVideo() {
        return videoRes;
    }

    // 广告位类型
    public int getAdtype() {
        return req.getPlacement().getAdTypeValue();
    }

    // 广告点击跳转地址
    public String getLink() {
        return MacroReplaceUtil.replaceURL(c.getClickUrl(), req, c, mcr.creative);
    }

    // 是否使用 webview 打开 link 地址
    public Integer getIswv() {
        return c.getOpenType() == 1 || c.getBillingType() == 4 ? 1 : null;
    }

    // 点击跟踪地址列表
    public List<String> getClktks() {
        String url = String.format("https://%s/cp/click?%s", req.getReqHost(), urlQuery);
        if (CollectionUtils.isEmpty(c.getClickTkUrlsList())) {
            return Collections.singletonList(url);
        }
        List<String> clickList = new ArrayList<>(c.getClickTkUrlsList().size() + 1);
        clickList.add(url);
        for (String clickUrl : c.getClickTkUrlsList()) {
            clickList.add(MacroReplaceUtil.replaceURL(clickUrl, req, c, mcr.creative));
        }
        return clickList;
    }

    // 展现跟踪地址列表
    public List<String> getImptks() {
        String url = String.format("https://%s/cp/impr?%s", req.getReqHost(), urlQuery);
        if (CollectionUtils.isEmpty(c.getImprTkUrlsList())) {
            return Collections.singletonList(url);
        }
        List<String> imprList = new ArrayList<>(c.getImprTkUrlsList().size() + 1);
        imprList.add(url);
        for (String imprUrl : c.getImprTkUrlsList()) {
            imprList.add(MacroReplaceUtil.replaceURL(imprUrl, req, c, mcr.creative));
        }
        return imprList;
    }

    // 广告App信息
    public AppRes getApp() {
        return appRes;
    }

    // 资源或模板地址
    public List<String> getResources() {
        switch (req.getPlacement().getAdType()) {
            case Interstitial:// 插屏
            case RewardVideo:// 视频
                List<String> resources = new ArrayList<>(2);
                if (mcr.template == null)
                    mcr.template = ccs.getH5TemplateByType(0);
                if (mcr.template != null)
                    resources.add(mcr.template.getUrl());
                boolean hasPlayUrl = StringUtils.isNotEmpty(mcr.creative.getPlayUrl());
                if (hasPlayUrl) {
                    resources.add(mcr.creative.getPlayUrl());
                } else if (mcr.endcardTemplate != null) {
                    resources.add(mcr.endcardTemplate.getUrl());
                }
                return resources;
            case Banner:
                if (mcr.template == null)
                    mcr.template = ccs.getH5TemplateByType(2);
                return mcr.template == null ? null : Collections.singletonList(mcr.template.getUrl());
            case CrossPromotion:
                if (mcr.template == null)
                    mcr.template = ccs.getH5TemplateByType(4);
                return mcr.template == null ? null : Collections.singletonList(mcr.template.getUrl());
            default:
                return null;
        }
    }

    // 事件上报时的基本参数串, 用于拼接
    public String getPs() {
        return null;
    }

    // AdMark
    public Object getMk() {
        if (req.getPlacement().getAdType() == CrossPromotion) {
            return CP_MK_VALUE;
        }
        return null;
    }

    // 广告多少秒后过期
    public Integer getExpire() {
        return 3600 * 6;
    }

    // Retargeting 标识, 有值表示是Retargeting
    public Integer getRt() {
        return null;
    }


    public static ParamsBuilder newParamsBuilder(AdRequest req, String tkid) {
        ParamsBuilder b = new ParamsBuilder(25)
                .p("cy", req.getCountry())
                .p("make", req.getMake())
                .p("brand", req.getBrand())
                .p("model", req.getModel())
                .p("osv", req.getOsv())
                .p("appv", req.getAppv())
                .p("sdkv", req.getSdkv())
                .p("reqid", req.getReqId())
                .p("cts", req.getTs())
                .p("sts", req.getServerTs())
                .p("did", req.getDid())
                .p("uid", req.getUid())
                .p("pid", req.getPid())
                .p("size", String.format("%dX%d", req.getWidth(), req.getHeight()))
                .p("bundle", req.getBundle())
                .p("cont", req.getContype())
                .p("mccmnc", req.getMccmnc())
                .p("cr", req.getCarrier())
                .p("lang", req.getLang())
                .p("tkid", tkid);
        if (StringUtils.isNotEmpty(req.getBidid())) {
            b.p("bidid", req.getBidid());
        }
        return b;
    }

    public SkAdNetwork getSka() {
        return ska;
    }

    /**
     * 是否测试模式, null:否, 1:是
     */
    public Integer getTest() {
        return req.isTest() ? 1 : null;
    }

    public Integer getOpenType() {
        return c.getOpenType();
    }

    public float getR() {
        return this.mc != null ? this.mc.getFinalBidPrice() : 0;
    }

    public int getRp() {//Revenue Precision, 0:undisclosed,1:exact,2:estimated,3:defined
        return 3;
    }

    public static class SkAdNetwork {
        public String adNetworkPayloadVersion;          // AdNetworkVersion
        public String adNetworkId;                      // AdNetworkIdentifier
        public int adNetworkCampaignId;                 // AdNetworkCampaignIdentifier: [1-100]
        public String adNetworkNonce;                   // AdNetworkNonce: 随机值,小写uuid
        public long adNetworkSourceAppStoreIdentifier;  // AdNetworkSourceAppStoreIdentifier：媒体app id
        public long adNetworkImpressionTimestamp;       // AdNetworkTimestamp: 时间戳,毫秒
        public String adNetworkAttributionSignature;    // AdNetworkAttributionSignature：签名
    }

    public static class VideoRes {
        public String url; // Video URL
        public int dur; // Video Duration
        public int skip; // Indicates if the player will allow the video to be skipped, where 0 = no, 1 = yes.
        public Object ves; // Object of VideoEvents
    }

    public static class AppRes {
        public String id;    // AppID
        public String name;  // AppName
        public String icon;  // AppIcon URL
        public float rating; // AppRating, 评分

        public AppRes(App app, CpMaterial icon, String cdnOrigin) {
            this.id = app.getAppId();
            this.name = app.getName();
            String url;
            if (icon != null) {
                url = icon.getUrl();
            } else {
                url = app.getIcon();
            }
            this.icon = Util.buildMaterialUrl(cdnOrigin, url);
            this.rating = Math.min(4.5F, app.getRatingValue());
        }
    }
}
