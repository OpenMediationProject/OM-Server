// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.cp.service;

import com.adtiming.om.pb.CrossPromotionPB;
import com.adtiming.om.pb.CrossPromotionPB.App;
import com.adtiming.om.pb.CrossPromotionPB.CpCreative;
import com.adtiming.om.pb.CrossPromotionPB.CpMaterial;
import com.adtiming.om.pb.CrossPromotionPB.H5Template;
import com.adtiming.om.server.cp.dto.Campaign;
import com.adtiming.om.server.cp.dto.CampaignTargeting;
import com.adtiming.om.server.cp.util.ECDSAUtil;
import com.adtiming.om.server.dto.Placement;
import com.adtiming.om.server.service.DictManager;
import com.adtiming.om.server.service.KafkaService;
import com.adtiming.om.server.service.PBLoader;
import com.adtiming.om.server.service.RedisService;
import com.adtiming.om.server.util.CountryCode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.security.PrivateKey;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Cache PB Data in memory
 * rsync & load cp_*.gz files from cache directory minutely
 */
@Service
public class CpCacheService extends PBLoader {

    private static final Logger LOG = LogManager.getLogger();

    @Resource
    private DictManager dictManager;

    // key:publisher_id-plat-country
    private Map<Integer, Map<Integer, Map<String, List<Campaign>>>> pubPlatCountryCampaigns = Collections.emptyMap();
    private Map<Long, Campaign> campaignMap = Collections.emptyMap();
    private Map<Long, CampaignTargeting> campaignTargetingMap = Collections.emptyMap();
    // key:campaign_id, AdType
    private Map<Long, Map<Integer, List<CpCreative>>> campaignCreativeMap = Collections.emptyMap();
    private Map<Long, CpCreative> creativeMap = Collections.emptyMap();
    private Map<Long, CpMaterial> materialMap = Collections.emptyMap();
    private Map<Integer, List<Campaign>> platTestCampaigns = Collections.emptyMap();
    private Map<Long, Campaign> testCampaignMap = Collections.emptyMap();
    private Map<Integer, H5Template> h5Templates = Collections.emptyMap();
    private Map<Integer, H5Template> h5TypeTemplates = Collections.emptyMap();
    private Map<String, App> apps = Collections.emptyMap();

    private String cdnOrigin;
    private String skAdNetworkVersion;
    private String skAdNetworkId;
    private PrivateKey skPrivateKey;

    private static final String CP_IMPR_FREQ_PREFIX = "cp_impr_freq_";

    private static final DateTimeFormatter KEY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Resource
    private RedisService redisService;

    @Resource
    private KafkaService kafkaService;

    public void reloadCache() {
        LOG.info("reload cp cache start");
        long start = System.currentTimeMillis();

        cdnOrigin = "https://" + dictManager.val("/om/cp/cdn_domain", "om.com");
        skAdNetworkVersion = dictManager.val("/om/cp/skAdNetworkVersion", "2.0");
        skAdNetworkId = dictManager.val("/om/cp/skAdNetworkId", "");
        String strPrivateKey = dictManager.val("/om/cp/skPrivateKey", "");
        if (StringUtils.isNotEmpty(strPrivateKey)) {
            skPrivateKey = ECDSAUtil.buildSkPrivateKey(strPrivateKey);
        }

        loadCampaignTargeting();
        loadCampaign();
        loadCreative();
        loadMaterial();
        loadH5Template();
        loadApp();

        LOG.info("reload cp cache finished, cost: {} ms", System.currentTimeMillis() - start);
    }

    private void loadCampaignTargeting() {
        load("cp_campaign_targeting", in -> {
            Map<Long, CampaignTargeting> ctMap = newMap(this.campaignTargetingMap, 1000, 20);
            while (true) {
                CrossPromotionPB.CpCampaignTargeting campaignTargeting = CrossPromotionPB.CpCampaignTargeting.parseDelimitedFrom(in);
                if (campaignTargeting == null) break;
                ctMap.put(campaignTargeting.getCampaignId(), new CampaignTargeting(campaignTargeting));
            }
            this.campaignTargetingMap = ctMap;
        });
    }

    private void loadCampaign() {
        load("cp_campaign", in -> {
            Map<Integer, Map<Integer, Map<String, List<Campaign>>>> ppccMap = newMap(this.pubPlatCountryCampaigns, 50, 20);
            Map<Long, Campaign> m = newMap(this.campaignMap, 1000, 20);
            while (true) {
                CrossPromotionPB.CpCampaign campaign = CrossPromotionPB.CpCampaign.parseDelimitedFrom(in);
                if (campaign == null) break;
                Campaign cpCampaign = new Campaign(campaign);
                m.put(campaign.getId(), cpCampaign);
                CampaignTargeting ct = campaignTargetingMap.get(campaign.getId());
                if (ct == null || CollectionUtils.isEmpty(ct.countryWhite)) {
                    ppccMap.computeIfAbsent(campaign.getPublisherId(), k -> new HashMap<>())
                            .computeIfAbsent(campaign.getPlatform(), k -> new HashMap<>())
                            .computeIfAbsent("ALL", k -> new ArrayList<>())
                            .add(cpCampaign);
                } else {
                    for (String country : ct.countryWhite) {
                        ppccMap.computeIfAbsent(campaign.getPublisherId(), k -> new HashMap<>())
                                .computeIfAbsent(campaign.getPlatform(), k -> new HashMap<>())
                                .computeIfAbsent(country, k -> new ArrayList<>())
                                .add(cpCampaign);
                    }
                }

            }
            this.pubPlatCountryCampaigns = ppccMap;
            this.campaignMap = m;
        });

        Set<Long> testCampaignIds = dictManager.streamVal("/om/cp/test_cids", ",")
                .map(StringUtils::trim)
                .filter(NumberUtils::isDigits)
                .map(NumberUtils::toLong)
                .collect(Collectors.toSet());
        Map<Long, Campaign> testCampaignMap = new HashMap<>(testCampaignIds.size());
        Map<Integer, List<Campaign>> platTestCampaigns = new HashMap<>(2);
        for (Long id : testCampaignIds) {
            Campaign c = this.campaignMap.get(id);
            if (c == null) {
                continue;
            }
            testCampaignMap.put(c.getId(), c);
            platTestCampaigns.computeIfAbsent(c.getPlatform(), k -> new ArrayList<>()).add(c);
        }
        this.testCampaignMap = testCampaignMap;
        this.platTestCampaigns = platTestCampaigns;
    }

    private void loadCreative() {
        load("cp_creative", in -> {
            Map<Long, CpCreative> creativeMap = newMap(this.creativeMap, 1000, 20);
            Map<Long, Map<Integer, List<CpCreative>>> cc = newMap(this.campaignCreativeMap, 1000, 20);
            while (true) {
                CpCreative creative = CpCreative.parseDelimitedFrom(in);
                if (creative == null) break;
                creativeMap.put(creative.getId(), creative);
                cc.computeIfAbsent(creative.getCampaignId(), k -> new HashMap<>())
                        .computeIfAbsent(creative.getTypeValue(), k -> new ArrayList<>())
                        .add(creative);
            }
            this.creativeMap = creativeMap;
            this.campaignCreativeMap = cc;
        });
    }

    private void loadMaterial() {
        load("cp_material", in -> {
            Map<Long, CpMaterial> m = newMap(this.materialMap, 1000, 20);
            while (true) {
                CpMaterial o = CpMaterial.parseDelimitedFrom(in);
                if (o == null) break;
                m.put(o.getId(), o);
            }
            this.materialMap = m;
        });
    }

    private void loadH5Template() {
        load("cp_template", in -> {
            Map<Integer, H5Template> tm = newMap(this.h5TypeTemplates, 10, 2);
            Map<Integer, H5Template> m = newMap(this.h5Templates, 10, 2);
            while (true) {
                H5Template o = H5Template.parseDelimitedFrom(in);
                if (o == null) break;
                tm.put(o.getType(), o);
                m.put(o.getId(), o);
            }
            this.h5TypeTemplates = tm;
            this.h5Templates = m;
        });
    }

    private void loadApp() {
        load("cp_app", in -> {
            Map<String, App> apps = newMap(this.apps, 100, 10);
            while (true) {
                App o = App.parseDelimitedFrom(in);
                if (o == null) break;
                apps.put(o.getAppId(), o);
            }
            this.apps = apps;
        });
    }

    public List<Campaign> getCampaigns(int pubId, String country, int plat) {
        Map<String, List<Campaign>> countryCampaigns = pubPlatCountryCampaigns
                .getOrDefault(pubId, Collections.emptyMap())
                .getOrDefault(plat, Collections.emptyMap());

        List<Campaign> countryCpList = countryCampaigns.getOrDefault(country, Collections.emptyList());
        List<Campaign> allCpList = countryCampaigns.getOrDefault(CountryCode.COUNTRY_ALL, Collections.emptyList());

        if (countryCpList.isEmpty() && allCpList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Campaign> rv = new ArrayList<>(countryCpList.size() + allCpList.size());
        rv.addAll(countryCpList);
        rv.addAll(allCpList);
        return rv;
    }

    public Campaign getCampaign(long cid) {
        return campaignMap.get(cid);
    }

    public boolean isTestCampaign(long cid) {
        return testCampaignMap.containsKey(cid);
    }

    public List<Campaign> getTestCampaigns(int plat) {
        return platTestCampaigns.get(plat);
    }

    public CampaignTargeting getCampaignTargeting(long cid) {
        return campaignTargetingMap.get(cid);
    }

    public CpCreative getCreative(long crid) {
        return creativeMap.get(crid);
    }

    public List<CpCreative> getCreatives(long cid, Placement placement) {
        int creativeType = 0;
        switch (placement.getAdType()) {
            case Banner:
                creativeType = CpCreative.Type.Banner_VALUE;
                break;
            case Native:
            case Splash:
                creativeType = CpCreative.Type.Native_VALUE;
                break;
            case RewardVideo:
            case Interstitial:
                creativeType = CpCreative.Type.Video_VALUE;
                break;
            case CrossPromotion:
                creativeType = CpCreative.Type.CrossPromotion_VALUE;
                break;
            default:
                break;
        }
        return campaignCreativeMap
                .getOrDefault(cid, Collections.emptyMap())
                .getOrDefault(creativeType, Collections.emptyList());
    }

    public CpMaterial getMaterial(long id) {
        return materialMap.get(id);
    }

    public H5Template getH5Template(int tempId) {
        return h5Templates.get(tempId);
    }

    public H5Template getH5TemplateByType(int type) {
        return h5TypeTemplates.get(type);
    }

    public App getApp(String appId) {
        return apps.get(appId);
    }

    public String getCdnOrigin() {
        return cdnOrigin;
    }

    public String getSkAdNetworkVersion() {
        return skAdNetworkVersion;
    }

    public String getSkAdNetworkId() {
        return skAdNetworkId;
    }

    public PrivateKey getSkPrivateKey() {
        return skPrivateKey;
    }

    public Map<Long, Integer> getDeviceImpressionCount(String deviceId, List<String> campaignIds) {
        if (campaignIds.isEmpty())
            return Collections.emptyMap();
        try {
            int day = LocalDate.now().getDayOfMonth();
            String key = CP_IMPR_FREQ_PREFIX + day + "_" + deviceId;
            List<String> campaignCount = redisService.hmget(key, campaignIds.toArray(new String[0]));
            if (campaignCount.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<Long, Integer> map = new HashMap<>(campaignIds.size());
            int index = 0;
            for (String cid : campaignIds) {
                map.put(NumberUtils.toLong(cid), NumberUtils.toInt(campaignCount.get(index++)));
            }
            return map;
        } catch (Exception e) {
            LOG.error("getDeviceCpImprCount error", e);
        }
        return Collections.emptyMap();
    }

    public void setDeviceImpressionCount(String deviceId, long campaignId) {
        int day = LocalDate.now().getDayOfMonth();
        String key = CP_IMPR_FREQ_PREFIX + day + "_" + deviceId;
        try {
            redisService.hincrBy(key, String.valueOf(campaignId), 1);
            redisService.expire(key, 86400);
        } catch (Exception e) {
            LOG.error("setDeviceImprCount error, prefix:{}, device:{}, cid:{}",
                    deviceId, campaignId, CP_IMPR_FREQ_PREFIX, e);
        }
    }

    public void decrCap(long campaignId) {
        try {
            final LocalTime time = LocalTime.now();
            final int expireSeconds = 86400 - time.toSecondOfDay() + 3600; // 今天系统时区剩余时间秒+1小时
            final String date = LocalDate.now().format(KEY_DATE_FORMAT);
            final String key = String.format("cp_cap_%s_%d", date, campaignId);
            redisService.decrBy(key, 1);
            redisService.expire(key, expireSeconds);
            kafkaService.send(new ProducerRecord<>("cp_campaign_cap_decr", StringUtils.joinWith("\1", campaignId, 1)));
        } catch (Exception e) {
            LOG.error("decrCap error, cid:{}",
                    campaignId, e);
        }
    }

    public int getCampaignCap(long campaignId) {
        final String redisKeyDatePrefix = LocalDate.now().format(KEY_DATE_FORMAT);
        try {
            String key = String.format("cp_cap_%s_%d", redisKeyDatePrefix, campaignId);
            String cap = redisService.get(key);
            if (StringUtils.isNotBlank(cap)) {
                return NumberUtils.toInt(cap);
            }
        } catch (Exception e) {
            LOG.error("getCampaignCap error, cid:{}",
                    campaignId, e);
        }
        return 0;
    }
}
