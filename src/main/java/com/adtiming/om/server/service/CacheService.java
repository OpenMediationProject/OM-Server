// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.service;

import com.adtiming.om.pb.*;
import com.adtiming.om.server.dto.*;
import com.adtiming.om.server.util.Encrypter;
import com.adtiming.om.server.util.Util;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * Cache PB Data in memory
 * rsync & load *.gz files from cache directory minutely
 */
@Service
public class CacheService {

    private static final Logger LOG = LogManager.getLogger();

    @Resource
    private AppConfig appConfig;

    private final File cacheDir = new File("cache");

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

    // key: currency
    private Map<String, Float> currencyRate = Collections.emptyMap();

    // key: placementId, country
    private Map<Integer, Map<String, PlacementPB.PlacementAbTest>> placementAbTest = Collections.emptyMap();

    // for auto waterfall, key: instanceId, country
    private Map<Integer, Map<String, Float>> instanceCountryEcpm3h = Collections.emptyMap();
    private Map<Integer, Map<String, Float>> instanceCountryEcpm6h = Collections.emptyMap();
    private Map<Integer, Map<String, Float>> instanceCountryEcpm12h = Collections.emptyMap();
    private Map<Integer, Map<String, Float>> instanceCountryEcpm24h = Collections.emptyMap();
    // key: adNetworkId, country
    private Map<Integer, Map<String, Float>> adnCountryEcpm24h = Collections.emptyMap();
    // key: adNetowrkId
    private Map<Integer, Float> adnEcpm3d = Collections.emptyMap();
    // key: adNetworkId, adType, country
    private Map<Integer, Map<Integer, Map<String, Float>>> adnAdTypeCountryEcpm3d = Collections.emptyMap();

    @PostConstruct
    private void init() {
        if (!cacheDir.exists() && cacheDir.mkdir())
            LOG.debug("mkdir {}", cacheDir);
        rsyncCache();
        reloadCache();
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void reloadCacheCron() {
        rsyncCache();
        reloadCache();
    }

    private void rsyncCache() {
        try {
            String shell = String.format("rsync -zavSHuq %s::om_cache/*.gz %s/", appConfig.getDtask(), cacheDir.getName());
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

        // loadInstanceCountryEcpm
        getInstanceCountryEcpm("3h", m -> this.instanceCountryEcpm3h = m);
        getInstanceCountryEcpm("6h", m -> this.instanceCountryEcpm6h = m);
        getInstanceCountryEcpm("12h", m -> this.instanceCountryEcpm12h = m);
        getInstanceCountryEcpm("24h", m -> this.instanceCountryEcpm24h = m);
        loadAdnCountryEcpm24h();
        loadAdnEcpm();
        loadAdnAdTypeCountryEcpm3d();

        LOG.info("reload cache finished, cost: {} ms", System.currentTimeMillis() - start);
    }

    // store file md5, check if file has been modified
    private Map<String, String> fileMd5Map = new HashMap<>();

    private void load(String name, LoadFun fn) {
        LOG.debug("load {} start", name);
        long start = System.currentTimeMillis();
        File file = new File(cacheDir, name + ".gz");
        if (!file.exists()) {
            LOG.warn("load {} failed, file not exists", name);
            return;
        }

        String fileMd5 = Encrypter.md5(file);
        if (fileMd5.equals(fileMd5Map.get(name))) {
            LOG.debug("skip {}, not modified", name);
            return;
        }

        try (InputStream in = new GZIPInputStream(new FileInputStream(file))) {
            fn.read(in);
            fileMd5Map.put(name, fileMd5);
            LOG.debug("load {} finished, cost {} ms", name, System.currentTimeMillis() - start);
        } catch (Exception e) {
            LOG.error("load {} error", name, e);
        }
    }

    @FunctionalInterface
    private interface LoadFun {
        void read(InputStream in) throws Exception;
    }

    private <K, V> Map<K, V> newMap(Map<K, V> map, int Default, int gt) {
        return new HashMap<>((map == null || map.isEmpty()) ? Default : map.size() + gt);
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
            while (true) {
                AdNetworkPB.InstanceRule o = AdNetworkPB.InstanceRule.parseDelimitedFrom(in);
                if (o == null) break;
                InstanceRule rule = new InstanceRule(o);

                Map<String, List<InstanceRule>> countryRuleList = map
                        .computeIfAbsent(o.getPlacementId(), k -> new HashMap<>());
                o.getCountryList().forEach(country -> countryRuleList.computeIfAbsent(country, k -> new ArrayList<>()).add(rule));
            }

            // 对 ruleList 按 priority 由小到大进行排序
            map.forEach((pid, countryRuleList) -> {
                countryRuleList.forEach((country, ruleList) -> {
                    ruleList.sort(Comparator.comparingInt(InstanceRule::getPriority));
                });
            });

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

    private void getInstanceCountryEcpm(String hourCount, Consumer<Map<Integer, Map<String, Float>>> fn) {
        load("stat_instance_country_ecpm_" + hourCount, in -> {
            Map<Integer, Map<String, Float>> map = newMap(null, 2000, 100);
            while (true) {
                StatPB.InstanceCountryEcpm o = StatPB.InstanceCountryEcpm.parseDelimitedFrom(in);
                if (o == null) break;
                map.computeIfAbsent(o.getInstanceId(), k -> new HashMap<>())
                        .put(o.getCountry(), o.getEcpm());
            }
            fn.accept(map);
        });
    }

    private void loadAdnCountryEcpm24h() {
        load("stat_adn_country_ecpm24h", in -> {
            Map<Integer, Map<String, Float>> map = newMap(this.adnCountryEcpm24h, 1000, 100);
            while (true) {
                StatPB.AdNetworkCountryEcpm o = StatPB.AdNetworkCountryEcpm.parseDelimitedFrom(in);
                if (o == null) break;
                map.computeIfAbsent(o.getAdnId(), k -> new HashMap<>())
                        .put(o.getCountry(), o.getEcpm());
            }
            this.adnCountryEcpm24h = map;
        });
    }

    private void loadAdnEcpm() {
        load("stat_adn_ecpm3d", in -> {
            Map<Integer, Float> map = newMap(this.adnEcpm3d, 14, 10);
            while (true) {
                StatPB.AdNetworkEcpm o = StatPB.AdNetworkEcpm.parseDelimitedFrom(in);
                if (o == null) break;
                map.put(o.getAdnId(), o.getEcpm());
            }
            this.adnEcpm3d = map;
        });
    }

    private void loadAdnAdTypeCountryEcpm3d() {
        load("stat_adn_adtype_country_ecpm3d", in -> {
            Map<Integer, Map<Integer, Map<String, Float>>> map = newMap(this.adnAdTypeCountryEcpm3d, 1000, 100);
            while (true) {
                StatPB.AdNetworkAdTypeCountryEcpm o = StatPB.AdNetworkAdTypeCountryEcpm.parseDelimitedFrom(in);
                if (o == null) break;
                map.computeIfAbsent(o.getMediationId(), k -> new HashMap<>())
                        .computeIfAbsent(o.getPlacementType(), k -> new HashMap<>())
                        .put(o.getCountry(), o.getEcpm());
            }
            this.adnAdTypeCountryEcpm3d = map;
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
            String brand, String model, String channel, int modelType) {
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
            if (rule.isHardMatched(brand, model, channel, modelType)) {
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
        List<InstanceRule> allRuleList = countryRuleList.get(Util.COUNTRY_ALL);// // 00 represent ALL COUNTRY
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

    public InstanceEcpm getInstanceEcpm(int mid, int iid, String country, int placementType) {
        Map<String, Float> countryEcpm3h = instanceCountryEcpm3h.get(iid);
        if (countryEcpm3h != null) {
            Float ecpm = countryEcpm3h.get(country);
            if (ecpm != null) {
                return new InstanceEcpm(iid, ecpm, "instanceCountryEcpm3h");
            }
        }

        Map<String, Float> countryEcpm6h = instanceCountryEcpm6h.get(iid);
        if (countryEcpm6h != null) {
            Float ecpm = countryEcpm6h.get(country);
            if (ecpm != null) {
                return new InstanceEcpm(iid, ecpm, "instanceCountryEcpm6h");
            }
        }

        Map<String, Float> countryEcpm12h = instanceCountryEcpm12h.get(iid);
        if (countryEcpm12h != null) {
            Float ecpm = countryEcpm12h.get(country);
            if (ecpm != null) {
                return new InstanceEcpm(iid, ecpm, "instanceCountryEcpm12h");
            }
        }

        Map<String, Float> countryEcpm24h = instanceCountryEcpm24h.get(iid);
        if (countryEcpm24h != null) {
            Float ecpm = countryEcpm24h.get(country);
            if (ecpm != null) {
                return new InstanceEcpm(iid, ecpm, "instanceCountryEcpm24h");
            }
        }

        Map<String, Float> mEcpm24h = adnCountryEcpm24h.get(mid);
        if (mEcpm24h != null) {
            Float ecpm = mEcpm24h.get(country);
            if (ecpm != null) {
                return new InstanceEcpm(iid, ecpm, "mediationCountryEcpm24h");
            }
        }
        Map<String, Float> mtcEcpm = adnAdTypeCountryEcpm3d.getOrDefault(mid, Collections.emptyMap()).get(placementType);
        if (mtcEcpm != null) {
            Float ecpm = mtcEcpm.get(country);
            if (ecpm != null) {
                return new InstanceEcpm(iid, ecpm, "mediationAdTypeCountryEcpm3d");
            }
        }
        Float ecpm = adnEcpm3d.get(mid);
        if (ecpm != null) {
            return new InstanceEcpm(iid, ecpm, "mediationEcpm3d");
        }
        return new InstanceEcpm(iid, -1F, "noEcpmData");
    }

    private <T> List<T> immutableList(List<T> list) {
        if (list == null) {
            return null;
        }
        return Collections.unmodifiableList(list);
    }

}
