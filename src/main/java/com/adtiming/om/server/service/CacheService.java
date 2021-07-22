// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.service;

import com.adtiming.om.pb.*;
import com.adtiming.om.server.cp.service.CpCacheService;
import com.adtiming.om.server.dto.*;
import com.adtiming.om.server.util.CountryCode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Cache PB Data in memory
 * rsync & load *.gz files from cache directory minutely
 */
@Service
public class CacheService extends PBLoader {

    private static final Logger LOG = LogManager.getLogger();

    @Resource
    private AppConfig appConfig;

    @Resource
    private DictManager dictManager;

    @Resource
    private CpCacheService cpCacheService;

    @Resource
    private WaterfallEcpmService ecpmService;

    @Resource
    private GeoService geoService;

    // key: pubAppKey
    private Map<String, PublisherApp> appkeyPubApps = Collections.emptyMap();
    // key: pubAppId
    private Map<Integer, PublisherApp> pubApps = Collections.emptyMap();
    // key: placementId
    private Map<Integer, Placement> placements = Collections.emptyMap();
    // key: pubAppId
    private Map<Integer, List<Placement>> appPlacements = Collections.emptyMap();

    // key: adNetworkId
    private Map<Integer, AdNetworkPB.AdNetwork> adNetworks;
    // key: pubAppId
    private Map<Integer, List<AdNetworkApp>> adNetworkApps;
    // key: pubAppId, AdnId
    private Map<Integer, Map<Integer, AdNetworkApp>> adNetworkAppMap;

    // key: instanceId
    private Map<Integer, Instance> instanceMap;
    // key: placementId
    private Map<Integer, List<Instance>> placementInstances;
    // key: placementId adNetworkId
    private Map<Integer, Map<Integer, List<Instance>>> placementAdnInstances;

    // key: instanceId, country
    private Map<Integer, Map<String, Float>> instanceCountryEcpm;
    // key: instanceId, country
    private Map<Integer, Map<String, Float>> placementCountryEcpm;

    // Placement FloorPrice Fix
    private Map<Integer, Map<String, Float>> placementEcpm = Collections.emptyMap();
    private Map<String, Map<Integer, Float>> countryAdTypeEcpm = Collections.emptyMap();

    // develop info
    private Map<String, DevPB.SdkDevDevice> devDevicePub = Collections.emptyMap();
    private Map<Integer, DevPB.SdkDevApp> devPubAppAdn = Collections.emptyMap();

    // key: placementId, country
    private Map<Integer, Map<String, List<InstanceRule>>> placementCountryRules = Collections.emptyMap();

    private Map<Integer, InstanceRule> instanceRuleMap = Collections.emptyMap();

    // key: currency
    private Map<String, Float> currencyRate = Collections.emptyMap();

    // key: placementId, country
    private Map<Integer, Map<String, PlacementPB.PlacementAbTest>> placementAbTest = Collections.emptyMap();

    @PostConstruct
    private void init() {
        reloadCacheCron();
        // ensure geoService init after rsync geo file
        geoService.init();
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void reloadCacheCron() {
        rsyncCache();

        dictManager.reloadCache();
        reloadCache();
        cpCacheService.reloadCache();
    }

    private void rsyncCache() {
        try {
            String shell = String.format("rsync -zavSHuq %s::om_cache/*.gz %s/", appConfig.getDtask(), CACHE_DIR.getName());
            LOG.debug(shell);
            String[] cmd = {"bash", "-c", shell};
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            IOUtils.copy(p.getInputStream(), System.out);
        } catch (Exception e) {
            LOG.error("rsync cache error", e);
        }
    }

    private void reloadCache() {
        LOG.info("reload cache start");
        long start = System.currentTimeMillis();

        loadAdNetwork();
        loadAdNetworkApp();

        loadPubApp();
        loadPlacement();
        loadInstance();

        // for Placement FloorPrice
        loadSdkPlacementEcpm();
        loadSdkCountryAdTypeEcpm();

        loadInstanceCountryEcpm();
        loadPlacementCountryEcpm();

        loadSdkDevApp();
        loadSdkDevDevice();

        loadInstanceRule();
        loadCurrency();
        loadAbTest();
        ecpmService.reloadCache();
        LOG.info("reload cache finished, cost: {} ms", System.currentTimeMillis() - start);
    }

    private void loadAdNetwork() {
        load("om_adnetwork", in -> {
            Map<Integer, AdNetworkPB.AdNetwork> map = newMap(this.adNetworks, 50, 20);
            while (true) {
                AdNetworkPB.AdNetwork o = AdNetworkPB.AdNetwork.parseDelimitedFrom(in);
                if (o == null) break;
                map.put(o.getId(), o);
            }
            this.adNetworks = map;
        });
    }

    private void loadAdNetworkApp() {
        load("om_adnetwork_app", in -> {
            Map<Integer, List<AdNetworkApp>> map = newMap(this.adNetworkApps, 50, 20);
            Map<Integer, Map<Integer, AdNetworkApp>> map1 = newMap(this.adNetworkAppMap, 50, 20);
            while (true) {
                AdNetworkPB.AdNetworkApp o = AdNetworkPB.AdNetworkApp.parseDelimitedFrom(in);
                if (o == null) break;
                AdNetworkApp adnApp = new AdNetworkApp(o);
                map.computeIfAbsent(o.getPubAppId(), k -> new ArrayList<>())
                        .add(adnApp);
                map1.computeIfAbsent(o.getPubAppId(), k -> new HashMap<>())
                        .put(o.getAdnId(), adnApp);
            }
            this.adNetworkApps = map;
            this.adNetworkAppMap = map1;
        });
    }

    private void loadPlacementCountryEcpm() {
        load("stat_placement_country_ecpm", in -> {
            Map<Integer, Map<String, Float>> placementCountryEcpm = newMap(this.placementCountryEcpm, 2000, 100);
            while (true) {
                StatPB.PlacementCountryEcpm o = StatPB.PlacementCountryEcpm.parseDelimitedFrom(in);
                if (o == null) break;
                placementCountryEcpm
                        .computeIfAbsent(o.getPlacementId(), k -> new HashMap<>())
                        .put(o.getCountry(), o.getEcpm());
            }
            this.placementCountryEcpm = placementCountryEcpm;
        });
    }

    private void loadInstanceCountryEcpm() {
        load("stat_instance_country_ecpm", in -> {
            Map<Integer, Map<String, Float>> instanceCountryEcpm = newMap(this.instanceCountryEcpm, 2000, 100);
            while (true) {
                StatPB.InstanceCountryEcpm o = StatPB.InstanceCountryEcpm.parseDelimitedFrom(in);
                if (o == null) break;
                instanceCountryEcpm
                        .computeIfAbsent(o.getInstanceId(), k -> new HashMap<>())
                        .put(o.getCountry(), o.getEcpm());
            }
            this.instanceCountryEcpm = instanceCountryEcpm;
        });
    }

    private void loadPubApp() {
        load("om_publisher_app", in -> {
            Map<Integer, PublisherApp> publisherApps = newMap(this.pubApps, 200, 20);
            Map<String, PublisherApp> appKeyPublisherApps = newMap(this.appkeyPubApps, 200, 20);
            while (true) {
                PubAppPB.PublisherApp o = PubAppPB.PublisherApp.parseDelimitedFrom(in);
                if (o == null) break;
                if (StringUtils.isNotEmpty(o.getAppKey())) {
                    PublisherApp papp = new PublisherApp(o);
                    publisherApps.put(o.getId(), papp);
                    appKeyPublisherApps.put(o.getAppKey(), papp);
                }
            }
            this.pubApps = publisherApps;
            this.appkeyPubApps = appKeyPublisherApps;
        });

    }

    private void loadPlacement() {
        load("om_placement", in -> {
            Map<Integer, Placement> placements = newMap(this.placements, 1000, 50);
            Map<Integer, List<Placement>> appPlacements = newMap(this.appPlacements, 300, 20);
            while (true) {
                PlacementPB.Placement o = PlacementPB.Placement.parseDelimitedFrom(in);
                if (o == null) break;
                Placement p = new Placement(o);
                placements.put(o.getId(), p);
                appPlacements.computeIfAbsent(o.getPubAppId(), k -> new ArrayList<>()).add(p);
            }
            this.placements = placements;
            this.appPlacements = appPlacements;
        });
    }

    private void loadInstance() {
        load("om_instance", in -> {
            Map<Integer, Instance> imap = newMap(this.instanceMap, 2000, 50);
            Map<Integer, List<Instance>> map0 = newMap(this.placementInstances, 1000, 50);
            Map<Integer, Map<Integer, List<Instance>>> map1 = newMap(this.placementAdnInstances, 1000, 50);
            while (true) {
                AdNetworkPB.Instance o = AdNetworkPB.Instance.parseDelimitedFrom(in);
                if (o == null) break;
                Instance ins = new Instance(o);
                imap.put(ins.getId(), ins);

                map0.computeIfAbsent(o.getPlacementId(), k -> new ArrayList<>())
                        .add(ins);
                map1.computeIfAbsent(o.getPlacementId(), k -> new HashMap<>())
                        .computeIfAbsent(o.getAdnId(), k -> new ArrayList<>())
                        .add(ins);
            }
            this.instanceMap = imap;
            this.placementInstances = map0;
            this.placementAdnInstances = map1;
        });
    }

    private void loadSdkDevDevice() {
        load("om_dev_device", in -> {
            Map<String, DevPB.SdkDevDevice> devDevicePub = newMap(this.devDevicePub, 50, 20);
            while (true) {
                DevPB.SdkDevDevice devDevice = DevPB.SdkDevDevice.parseDelimitedFrom(in);
                if (devDevice == null) break;
                devDevicePub.put(devDevice.getDeviceId(), devDevice);
            }
            this.devDevicePub = devDevicePub;
        });
    }

    private void loadSdkDevApp() {
        load("om_dev_app", in -> {
            Map<Integer, DevPB.SdkDevApp> devPubAppMediation = newMap(this.devPubAppAdn, 50, 20);
            while (true) {
                DevPB.SdkDevApp devApp = DevPB.SdkDevApp.parseDelimitedFrom(in);
                if (devApp == null) break;
                devPubAppMediation.put(devApp.getPubAppId(), devApp);
            }
            this.devPubAppAdn = devPubAppMediation;
        });
    }

    private void loadSdkPlacementEcpm() {
        load("stat_placement_ecpm", in -> {
            Map<Integer, Map<String, Float>> map = newMap(this.placementEcpm, 1000, 50);
            while (true) {
                StatPB.PlacementEcpm o = StatPB.PlacementEcpm.parseDelimitedFrom(in);
                if (o == null) break;
                map.computeIfAbsent(o.getPlacementId(), k -> new HashMap<>())
                        .put(o.getCountry(), o.getEcpm());
            }
            this.placementEcpm = map;
        });
    }

    private void loadSdkCountryAdTypeEcpm() {
        load("stat_country_adtype_ecpm", in -> {
            Map<String, Map<Integer, Float>> map = newMap(this.countryAdTypeEcpm, 1000, 50);
            while (true) {
                StatPB.CountryAdTypeEcpm o = StatPB.CountryAdTypeEcpm.parseDelimitedFrom(in);
                if (o == null) break;
                map.computeIfAbsent(o.getCountry(), k -> new HashMap<>())
                        .put(o.getAdType(), o.getEcpm());
            }
            this.countryAdTypeEcpm = map;
        });
    }


    private void loadInstanceRule() {
        load("om_instance_rule", in -> {
            Map<Integer, Map<String, List<InstanceRule>>> map = newMap(this.placementCountryRules, 100, 30);
            Map<Integer, InstanceRule> ruleMap = newMap(this.instanceRuleMap, 100, 30);
            while (true) {
                AdNetworkPB.InstanceRule o = AdNetworkPB.InstanceRule.parseDelimitedFrom(in);
                if (o == null) break;
                InstanceRule rule = new InstanceRule(o);

                Map<String, List<InstanceRule>> countryRuleList = map
                        .computeIfAbsent(o.getPlacementId(), k -> new HashMap<>());
                o.getCountryList().forEach(country -> countryRuleList.computeIfAbsent(country, k -> new ArrayList<>()).add(rule));
                ruleMap.put(rule.getId(), rule);
            }

            // 对 ruleList 按 priority 由小到大进行排序
            map.forEach((pid, countryRuleList) -> countryRuleList.forEach((country, ruleList) ->
                    ruleList.sort(Comparator.comparingInt(InstanceRule::getPriority))));
            this.instanceRuleMap = ruleMap;
            this.placementCountryRules = map;
        });
    }

    private void loadCurrency() {
        load("om_currency", in -> {
            Map<String, Float> cr = newMap(this.currencyRate, 200, 20);
            while (true) {
                CommonPB.Currency currency = CommonPB.Currency.parseDelimitedFrom(in);
                if (currency == null) break;
                cr.put(currency.getCurFrom(), currency.getExchangeRate());
            }
            this.currencyRate = cr;
        });
    }

    private void loadAbTest() {
        load("om_abtest", in -> {
            Map<Integer, Map<String, PlacementPB.PlacementAbTest>> map = newMap(this.placementAbTest, 200, 20);
            while (true) {
                PlacementPB.PlacementAbTest abTest = PlacementPB.PlacementAbTest.parseDelimitedFrom(in);
                if (abTest == null) break;
                map.computeIfAbsent(abTest.getPlacementId(), k -> new HashMap<>())
                        .put(abTest.getCountry(), abTest);
            }
            this.placementAbTest = map;
        });
    }

    public PublisherApp getPublisherApp(Integer pubAppId) {
        return pubApps.get(pubAppId);
    }

    public PublisherApp getPublisherApp(String appKey) {
        return appkeyPubApps.get(appKey);
    }

    public Placement getPlacement(int placementId) {
        return placements.get(placementId);
    }

    public List<Placement> getPlacementsByApp(int publisherAppId) {
        return appPlacements.get(publisherAppId);
    }

    public AdNetworkPB.AdNetwork getAdNetwork(int adNetworkId) {
        return adNetworks.get(adNetworkId);
    }

    public AdNetworkApp getAdnApp(int pubAppId, int adnId) {
        return adNetworkAppMap.getOrDefault(pubAppId, Collections.emptyMap()).get(adnId);
    }

    public List<AdNetworkApp> getAdnApps(int pubAppId) {
        return adNetworkApps.getOrDefault(pubAppId, Collections.emptyList());
    }

    public List<Instance> getPlacementAdnInstanceList(int placementId, int adnId) {
        return placementAdnInstances
                .getOrDefault(placementId, Collections.emptyMap())
                .get(adnId);
    }

    /**
     * 获取广告位下的匹配 placement rule 后的 instance 集合, 用于初始化
     *
     * @param placementId    placementId
     * @param acceptAdnIdSet 可以接受的 adnId 集合
     * @param country        country
     * @param brand          brand
     * @param model          model
     * @param channel        国内 Android channel
     * @param modelType      model type, {0:Phone,1:Pad,2:TV}
     * @return Instance list or null, 返回 list 可修改, 不影响缓存结构
     */
    public List<Instance> getPlacementInstancesAfterRuleMatch(
            int placementId, Set<Integer> acceptAdnIdSet, String country,
            String brand, String model, String channel, int modelType, String osv, String sdkv,
            String appv, String did) {
        List<InstanceRule> ruleList = getCountryRules(placementId, country);

        if (CollectionUtils.isEmpty(ruleList)) {
            List<Instance> list = getPlacementInstances(placementId);
            if (list != null && !list.isEmpty()) {
                return list.stream()
                        .filter(ins -> acceptAdnIdSet.contains(ins.getAdnId()))
                        .collect(Collectors.toList());
            }
            return list;
        }

        Set<Integer> insIdSet = new HashSet<>();
        for (InstanceRule rule : ruleList) {
            if (rule.isHardMatched(brand, model, channel, modelType, osv, sdkv, appv, did)) {
                insIdSet.addAll(rule.getInstanceList());
            }
        }

        if (insIdSet.isEmpty()) {
            return null;
        }

        return insIdSet.stream()
                .map(instanceMap::get)
                .filter(Objects::nonNull)
                .filter(ins -> acceptAdnIdSet.contains(ins.getAdnId()))
                .collect(Collectors.toList());
    }

    /**
     * For init response
     *
     * @return null if no rule
     * @see com.adtiming.om.server.dto.InitResponse
     */
    public List<InstanceRule> getCountryRules(int placementId, String country) {
        Map<String, List<InstanceRule>> countryRuleList = placementCountryRules
                .getOrDefault(placementId, Collections.emptyMap());
        if (countryRuleList.isEmpty()) {
            return null;
        }

        List<InstanceRule> ruleList = countryRuleList.get(country);
        List<InstanceRule> allRuleList = countryRuleList.get(CountryCode.COUNTRY_ALL);// // 00 represent ALL COUNTRY
        if (ruleList == null) {
            return immutableList(allRuleList);
        } else if (allRuleList == null) {
            return immutableList(ruleList);
        } else /* neither ruleList or allRuleList is null */ {
            List<InstanceRule> list = new ArrayList<>(ruleList.size() + allRuleList.size());
            list.addAll(ruleList);
            list.addAll(allRuleList);
            return immutableList(list);
        }
    }

    // for waterfall
    public List<Instance> getPlacementInstances(int placementId) {
        return placementInstances.get(placementId);
    }

    // for dev mode
    public List<Instance> getPlacementAdnInstances(int placementId, int adnId) {
        return placementAdnInstances
                .getOrDefault(placementId, Collections.emptyMap())
                .get(adnId);
    }

    public Instance getInstanceById(int insId) {
        return instanceMap.get(insId);
    }

    public Float getInstanceCountryEcpm(int instanceId, String country) {
        if (instanceCountryEcpm == null)
            return null;
        return instanceCountryEcpm
                .getOrDefault(instanceId, Collections.emptyMap())
                .get(country);
    }

    public Float getPlacementCountryEcpm(int placementId, String country) {
        if (placementCountryEcpm == null)
            return null;
        return placementCountryEcpm
                .getOrDefault(placementId, Collections.emptyMap())
                .get(country);
    }

    public Integer getDevDevicePub(String deviceId) {
        DevPB.SdkDevDevice devDevice = devDevicePub.get(deviceId);
        if (devDevice == null) {
            return null;
        }
        return devDevice.getPublisherId();
    }

    public Integer getDevDeviceAbtMode(String deviceId) {
        DevPB.SdkDevDevice devDevice = devDevicePub.get(deviceId);
        if (devDevice == null) {
            return null;
        }
        return devDevice.getAbtValue();
    }

    public Integer getDevAppAdnId(int pubAppId) {
        DevPB.SdkDevApp app = devPubAppAdn.get(pubAppId);
        if (app == null) {
            return null;
        }
        return app.getAdnId();
    }

    public float getUsdMoney(String currency, float money) {
        float rate = currencyRate.getOrDefault(currency, 1.0f);
        return money * rate;
    }

    /*public int getAbTestMode(Placement placement, String deviceId) {
        if (!placement.isAbTestOn())
            return CommonPB.ABTest.None_VALUE;
        return getAbTestMode(placement.getPubAppId(), deviceId);
    }*/

    /*public int getAbTestMode(int placementId, WaterfallRequest o) {
        PlacementPB.PlacementAbTest abTest = placementAbTest
                .getOrDefault(placementId, Collections.emptyMap())
                .get(o.getCountry());
        if (abTest == null) {
            abTest = placementAbTest.getOrDefault(placementId, Collections.emptyMap()).get(Util.COUNTRY_ALL);
            if (abTest == null) {
                return CommonPB.ABTest.None_VALUE;
            }
        }
        if (segment.isMatched(o.getCountry(), o.getContype(), o.getBrand(), o.getModel(), o.getIap(), o.getImprTimes())) {
            int aPer = abTest.getAPer();
            int bPer = abTest.getBPer();
            int aLast = aPer;
            // Compatible AB allocation ratio is not 100
            if (aPer + bPer != 100) {
                aLast = Math.round(aPer * 100.0f / (aPer + bPer) * 1.0f);
            }
            int code = Math.abs(o.getDid().hashCode());
            return code % 100 < aLast ? 1 : 2;
        }
        return CommonPB.ABTest.None_VALUE;
    }*/

    public float getPlacementEcpm(Integer placementId, String country) {
        return placementEcpm
                .getOrDefault(placementId, Collections.emptyMap())
                .getOrDefault(country, -1F);
    }

    public float getCountryAdTypeEcpm(String country, int adType) {
        return countryAdTypeEcpm
                .getOrDefault(country, Collections.emptyMap())
                .getOrDefault(adType, -1F);
    }

    private <T> List<T> immutableList(List<T> list) {
        if (list == null) {
            return null;
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * for waterfall
     *
     * @param placementId placementId
     * @param country     country
     * @param req         waterfall request
     * @return PlacementRule or null
     */
    public InstanceRule matchPlacementRule(int placementId, String country, WaterfallRequest req) {
        List<InstanceRule> ruleList = getPlacementCountryAbtRules(placementId, country);
        if (ruleList == null) return null;

        for (InstanceRule rule : ruleList) {
            if (rule.isMatched(req)) {
                return rule;
            }
        }
        return null;
    }

    private List<InstanceRule> getPlacementCountryAbtRules(int placementId, String country) {
        Map<String, List<InstanceRule>> countryRuleList = this.placementCountryRules
                .getOrDefault(placementId, Collections.emptyMap());
        if (countryRuleList.isEmpty()) {
            return null;
        }
        List<InstanceRule> ruleList = countryRuleList.get(country);
        List<InstanceRule> allRuleList = countryRuleList.get("00");
        if (ruleList == null) {
            return immutableList(allRuleList);
        } else if (allRuleList == null) {
            return immutableList(ruleList);
        } else /* neither ruleList or allRuleList is null */ {
            List<InstanceRule> list = new ArrayList<>(ruleList.size() + allRuleList.size());
            list.addAll(ruleList);
            list.addAll(allRuleList);
            return immutableList(list);
        }
    }

    public InstanceRule getInstanceRule(int ruleId) {
        return instanceRuleMap.get(ruleId);
    }

}
