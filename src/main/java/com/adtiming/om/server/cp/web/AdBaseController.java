// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.cp.web;

import com.adtiming.om.pb.CrossPromotionPB;
import com.adtiming.om.pb.CrossPromotionPB.CpCreative;
import com.adtiming.om.server.cp.dto.*;
import com.adtiming.om.server.cp.service.CpCacheService;
import com.adtiming.om.server.dto.Placement;
import com.adtiming.om.server.service.AppConfig;
import com.adtiming.om.server.service.CacheService;
import com.adtiming.om.server.util.RandomUtil;
import com.adtiming.om.server.web.BaseController;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static com.adtiming.om.pb.CommonPB.AdType.*;
import static com.adtiming.om.pb.CrossPromotionPB.CpMaterial.Type;

/**
 * Created by wuwei on 2020-11-16
 * AdBase for CrossPromotion
 */
public class AdBaseController extends BaseController {

    public static final Logger LOG = LogManager.getLogger();

    @Resource
    protected AppConfig appConfig;

    @Resource
    protected CacheService cacheService;

    @Resource
    protected CpCacheService cpCacheService;

    /**
     * 二进制, [descn,title,video,image,icon]
     */
    private static final int[] ADTYPE_MATCH_REQUIRE = {
            0b00010, // Banner
            0b11011, // Native
            0b11111, // Video
            0b11111, // Interstitial
            0b00010, // Splash
            0b01100  // CrossPromotion
    };

    /**
     * 活动匹配, no-fill 返回 null
     */
    List<MatchedCampaign> matchCampaigns(AdRequest req, CLResponse res) {
        Placement placement = req.getPlacement();
        if (placement.getAdType() != RewardVideo
                && placement.getAdType() != Interstitial
                && (req.getHeight() == 0 || req.getWidth() == 0)) {
            res.addDebug("size invalid").setCode(NoFillReason.SIZE_INVALID);
            return null;
        }

        res.addDebug("Is test: %s", req.isTest());

        if (!req.isTest()) {
            // block sdk_version
            if (placement.isBlockSdkVersion(req.getSdkv())) {
                res.setCode(NoFillReason.SDK_VERSION_DENIED).addDebug("sdkv denied");
                return null;
            }

            // match_filter_osv
            if (!placement.isAllowOsv(req.getOsv())) {
                res.setCode(NoFillReason.OSV_DENIED).addDebug("osv denied");
                return null;
            }

            // match_filter_make
            if (!placement.isAllowMake(req.getMake())) {
                res.setCode(NoFillReason.MAKE_DENIED).addDebug("make denied");
                return null;
            }

            // match_filter_brand
            if (!placement.isAllowBrand(req.getBrand())) {
                res.setCode(NoFillReason.BRAND_DENIED).addDebug("brand denied");
                return null;
            }

            // match_filter_model
            if (!placement.isAllowModel(req.getModel())) {
                res.setCode(NoFillReason.MODEL_DENIED).addDebug("model denied");
                return null;
            }

            // block black device_id
            if (placement.isBlockDeviceId(req.getDid())) {
                res.setCode(NoFillReason.DID_DENIED).addDebug("did denied");
                return null;
            }

            // match_filter_period
            if (!placement.isAllowPeriod(req.getCountry())) {
                res.setCode(NoFillReason.PERIOD_DENIED).addDebug("period denied");
                return null;
            }

        }

        List<Campaign> pcs;
        if (req.isTest()) {
            pcs = cpCacheService.getTestCampaigns(req.getPlat());
            if (CollectionUtils.isEmpty(pcs)) {
                res.setCode(NoFillReason.DEV_CAMPAIGN_LOST).addDebug("dev_campaign lost");
                LOG.error("dev_campaign lost {}", 692);
                return null;
            }
            res.addDebug("test mode, size: %d", pcs.size());
        } else {
            pcs = cpCacheService.getCampaigns(req.getPublisherId(), req.getCountry(), req.getPlat());
        }

        if (pcs == null || pcs.isEmpty()) {
            res.setCode(NoFillReason.NO_CAMPAIGN_PC).addDebug("no campaign available");
            return null;
        }

        if (res.isDebugEnabled()) {
            res.addDebug("campaigns before match: %s", pcs.stream().map(Campaign::getId).map(String::valueOf).collect(Collectors.joining(",")));
        }

        String mccmnc = req.getCarrier();
        int conType = req.getContype();
        double whRatioReq = req.getWidth() / (double) req.getHeight();
        // floorPrice & maxPrice
        final float floorPrice = 0.01F;
        final float maxPrice = 99F;

        final Map<Long, MatchedCampaign> campaignMatched = new HashMap<>();

        for (Campaign c : pcs) {
            final CrossPromotionPB.App capp = cpCacheService.getApp(c.getAppId());
            final CampaignTargeting cpct = cpCacheService.getCampaignTargeting(c.getId());
            final float bidPrice = c.getCountryBidPrice(req.getCountry());
            if (req.isRequireSkAdNetwork()) {
                if (c.getSkaCampaignId() < 1) {
                    res.addDebug("remove %d, SKAdnetworkCampaignId is not configuration", c.getId());
                    continue;
                }
            }
            if (!req.isTest()) {
                if (cpct != null) {
                    if (!cpct.acceptCountry(req.getCountry())) {
                        res.setLastReason("block country").addDebug(
                                "remove %d, block country: %s, blacklist:%s",
                                c.getId(), req.getCountry(), cpct.countryBlack);
                        continue; // 过滤 Campaign publisher app
                    }
                    if (!cpct.acceptPublisherApp(placement.getPubAppId())) {
                        res.setLastReason("campaign targeting publisher app").addDebug(
                                "remove %d, campaign targeting publisher app: %s, whitelist:%s,blacklist:%s",
                                c.getId(), placement.getPubAppId(), cpct.pubappWhite, cpct.pubappBlack);
                        continue; // 过滤 Campaign publisher app
                    }
                    if (!cpct.acceptPlacement(placement.getId())) {
                        res.setLastReason("campaign targeting placement").addDebug(
                                "remove %d, campaign targeting placement: %s, whitelist:%s,blacklist:%s",
                                c.getId(), placement.getId(), cpct.placementWhite, cpct.placementBlack);
                        continue; // 过滤 Campaign placemnt_whitelist
                    }

                    if (!cpct.acceptCarrier(mccmnc)) {
                        res.setLastReason("carrier mismatch").addDebug(
                                "remove %d, carrier mismatch: %s, whitelist:%s,blacklist:%s",
                                c.getId(), mccmnc, cpct.mccmncWhite, cpct.mccmncBlack);
                        continue; // 过滤 运营商
                    }
                    if (cpct.getContype() > 0 && (cpct.getContype() & conType) == 0) {
                        res.setLastReason("conType mismatch").addDebug(
                                "remove %d, con_type mismatch: %d, accept: %d",
                                c.getId(), conType, cpct.getContype());
                        continue; // 过滤 conType
                    }

                    if (!cpct.acceptMake(req.getMake())) {
                        res.setLastReason("make mismatch").addDebug(
                                "remove %d, make mismatch: %s, white: %s, black: %s",
                                c.getId(), req.getMake(), cpct.makeWhite, cpct.makeBlack);
                        continue; // make定向过滤
                    }

                    if (!cpct.acceptBrand(req.getBrand())) {
                        res.setLastReason("brand mismatch").addDebug(
                                "remove %d, brand mismatch: %s, white: %s, black: %s",
                                c.getId(), req.getBrand(), cpct.brandWhite, cpct.brandBlack);
                        continue; // brand定向过滤
                    }
                    if (!cpct.acceptModel(req.getModel())) {
                        res.setLastReason("model mismatch").addDebug(
                                "remove %d, model mismatch: %s, white: %s, black: %s",
                                c.getId(), req.getModel(), cpct.modelWhite, cpct.modelBlack);
                        continue; // model定向过滤
                    }
                    if (!cpct.acceptDeviceType(req.getMtype())) {
                        res.setLastReason("device type mismatch").addDebug(
                                "remove %d, device type mismatch: %s,whitelist:%s,black:%s",
                                c.getId(), req.getDtype(), cpct.devicetypeWhite, cpct.devicetypeBlack);
                        continue; // device type定向过滤
                    }
                    if (!cpct.acceptOsv(req.getOsv())) {
                        res.setLastReason("osv mismatch").addDebug(
                                "remove %d, osv mismatch: %s, whitelist:%s, blacklist:%s",
                                c.getId(), req.getOsv(), cpct.osvWhite, cpct.osvBlack);
                        continue; // osv定向过滤
                    }
                }
                if (StringUtils.equals(c.getAppId(), req.getPubApp().getAppId())) {
                    res.setLastReason("same appId").addDebug(
                            "remove %d, pub_app equals campaign app: %s", c.getId(), c.getAppId());
                    continue; // 过滤 publisher_app.app_id = campaign.app_id
                }

                // remove bidPrice < floorPrice
                if (bidPrice < floorPrice) {
                    res.setLastReason("low bidPrice").addDebug(
                            "remove %d, low bidPrice: %.4f, floorPrice: %.4f",
                            c.getId(), bidPrice, floorPrice);
                    continue;
                }
            }

            // 开始匹配创意 match creative
            List<CpCreative> candidateCreatives = cpCacheService.getCreatives(c.getId(), placement);
            List<MatchedCreative> matchedCreatives = null;
            int require = ADTYPE_MATCH_REQUIRE[placement.getAdTypeValue()];

            // loop_creative:
            for (CpCreative creative : candidateCreatives) {
                int matched = 0;
                if (capp != null && StringUtils.isNotEmpty(capp.getIcon()))
                    matched |= 1;
                MatchedCreative mc = new MatchedCreative();
                if (StringUtils.isNotEmpty(creative.getTitle()))
                    matched |= (1 << 3);
                if (StringUtils.isNotEmpty(creative.getDescn()))
                    matched |= (1 << 4);
                mc.template = cpCacheService.getH5Template(creative.getTemplate());//初始模版
                mc.endcardTemplate = cpCacheService.getH5Template(creative.getEndcardTemplate());//着陆页模版
                for (long materialId : creative.getMaterialIdsList()) {
                    CrossPromotionPB.CpMaterial material = cpCacheService.getMaterial(materialId);
                    if (material == null)
                        continue;
                    double whRatio = material.getWidth() / (double) material.getHeight();
                    Type mtype = material.getType();
                    if (mtype == Type.Icon) {
                        mc.materialIcon = material;
                        matched |= 1;

                    } else if (mtype == Type.Video) {
                        mc.materialVideo = material;
                        matched |= (1 << 2);

                    } else if (mtype == Type.Image) {
                        if (placement.getAdType() == Banner || placement.getAdType() == Native) {
                            // 广告位尺寸匹配，宽高比 < 0.1
                            if (whRatio == whRatioReq || Math.abs(whRatio - whRatioReq) < 0.1) {
                                mc.addImg(material);
                                matched |= (1 << 1);
                            }
                        } else {
                            // 模版尺寸匹配
                            if (mc.template != null && mc.template.getWidth() > 0 && mc.template.getHeight() > 0) {
                                double ecRatio = mc.template.getWidth() / (double) mc.template.getHeight();
                                //尺寸匹配，过滤宽高比 > 0.1 的素材
                                if ((Math.abs(ecRatio - whRatio) > 0.1)) {
                                    res.addDebug("material mismatch [template size filter], campaign: %d, creative: %d",
                                            c.getId(), creative.getId());
                                    continue;
                                }
                            }
                            matched |= (1 << 1);
                            mc.addImg(material);
                        }
                    }

                }

                if ((matched & require) == require) {
                    mc.creative = creative;
                    if (matchedCreatives == null)
                        matchedCreatives = new ArrayList<>(candidateCreatives.size());
                    matchedCreatives.add(mc);
                } else {
                    res.setLastReason("creative mismatch").addDebug(
                            "creative mismatch, campaign: %d, creative: %d, result: %d, require: %d",
                            c.getId(), creative.getId(), matched, require);
                }
            }

            if (matchedCreatives == null) {
                res.addDebug("remove %d, creative mismatch, accept: %d", c.getId(), require);
                continue;
            }

            MatchedCreative mc;
            if (matchedCreatives.size() > 1) {
                //插屏 优先出video
                if (placement.getAdType() == Interstitial) {
                    Map<MatchedCreative, Integer> videoCreative = new HashMap<>();
                    Map<MatchedCreative, Integer> otherCreative = new HashMap<>();
                    for (MatchedCreative m : matchedCreatives) {
                        if (m.creative.getType() == CpCreative.Type.Video) {
                            videoCreative.put(m, m.creative.getWeight());
                        } else {
                            otherCreative.put(m, m.creative.getWeight());
                        }
                    }

                    if (videoCreative.size() >= 1) {
                        mc = RandomUtil.randomByWeight(videoCreative);
                    } else {
                        mc = RandomUtil.randomByWeight(otherCreative);
                    }
                } else {
                    Map<MatchedCreative, Integer> cWeights = new HashMap<>(matchedCreatives.size());
                    for (MatchedCreative m : matchedCreatives) {
                        cWeights.put(m, m.creative.getWeight());
                    }
                    mc = RandomUtil.randomByWeight(cWeights);
                }
            } else {
                mc = matchedCreatives.get(0);
            }
            campaignMatched.put(c.getId(), new MatchedCampaign(c, capp, mc, bidPrice));
        }

        if (campaignMatched.isEmpty()) {
            res.setCode(NoFillReason.NO_CAMPAIGN_AVALIABLE);
            return null;
        }

        if (res.isDebugEnabled()) {
            res.addDebug("campaigns before sort: %s", campaignMatched.keySet());
        }

        if (req.isTest()) {
            // 测试模式下, 直接随机返回一个活动
            List<MatchedCampaign> mcs = new ArrayList<>(campaignMatched.values());
            MatchedCampaign c = mcs.get(ThreadLocalRandom.current().nextInt(mcs.size()));
            c.setFinalBidPrice(0F); // 强制抹掉 price, 不计费
            res.addDebug("devMode random sort, return %d", c.getCampaignId());
            return Collections.singletonList(c);
        }


        if (campaignMatched.size() == 1) {
            MatchedCampaign firstCampaign = campaignMatched.values().iterator().next();
            firstCampaign.setFinalBidPrice(Math.min(firstCampaign.getBidPrice(), maxPrice));
            if (res.isDebugEnabled()) {
                res.addDebug("return: %s, floorPrice: %.4f, bidPrice: %.4f, finalBidPrice: %.4f",
                        firstCampaign.getCampaign().getId(), floorPrice, firstCampaign.getBidPrice(), firstCampaign.getFinalBidPrice());
            }
            return Collections.singletonList(firstCampaign);
        } else {
            List<MatchedCampaign> list = campaignMatched.values().stream()
                    .map(o -> o.setFinalBidPrice(Math.min(o.getBidPrice(), maxPrice)))
                    .sorted((a, b) -> Float.compare(b.getBidPrice(), a.getBidPrice()))
                    .collect(Collectors.toList());
            if (res.isDebugEnabled()) {
                res.addDebug(formatMatchedCampaigns(list));
            }
            return list;
        }
    }

    private String formatMatchedCampaigns(List<MatchedCampaign> matchedCampaigns) {
        return matchedCampaigns.stream()
                .map(c -> String.format("%d:%.4f", c.getCampaign().getId(), c.getBidPrice()))
                .collect(Collectors.joining(", "));
    }

}
