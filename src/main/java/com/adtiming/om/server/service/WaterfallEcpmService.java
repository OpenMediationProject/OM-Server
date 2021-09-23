package com.adtiming.om.server.service;

import com.adtiming.om.pb.StatPB;
import com.adtiming.om.server.dto.Instance;
import com.adtiming.om.server.dto.InstanceEcpm;
import com.adtiming.om.server.util.MathUtil;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Service
public class WaterfallEcpmService extends PBLoader {
    private static final Logger log = LogManager.getLogger();

    private static final BigDecimal d1000 = new BigDecimal(1000);

    // for auto waterfall old ECPM算法
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

    // for ExponentialSmoothing ECPM指数平滑算法使用
    private Map<Integer, Map<String, TreeMap<Integer, BigDecimal[]>>> instanceCountryEcpm7d = Collections.emptyMap();
    private Map<Integer, Map<Integer, Map<String, Float>>> adnAdTypeCountryEcpm = Collections.emptyMap();
    private Map<Integer, Map<String, Float>> adTypeCountryEcpm = Collections.emptyMap();
    private Map<String, Float> countryEcpm = Collections.emptyMap();
    private Map<Integer, Map<Integer, Float>> adnAdTypeEcpm = Collections.emptyMap();
    private Map<Integer, Float> adTypeEcpm = Collections.emptyMap();
    private Map<Integer, Float> adnEcpm = Collections.emptyMap();
    private float systemDefaultInstanceEcpm;
    public int defalutEcpmAlgorithmId;

    @Resource
    private DictManager dm;

    public void reloadCache() {
        // loadInstanceCountryEcpm
        getInstanceCountryEcpm("3h", m -> this.instanceCountryEcpm3h = m);
        getInstanceCountryEcpm("6h", m -> this.instanceCountryEcpm6h = m);
        getInstanceCountryEcpm("12h", m -> this.instanceCountryEcpm12h = m);
        getInstanceCountryEcpm("24h", m -> this.instanceCountryEcpm24h = m);
        loadAdnCountryEcpm24h();
        loadAdnEcpm();
        loadAdNetworkAdTypeCountryEcpm3d();

        //for ExponentialSmoothing
        loadInstanceCountryEcpm7d();
        loadAdnAdTypeCountryEcpm3d();
        loadAdTypeCountryEcpm3d();
        loadCountryEcpm3d();
        loadAdnAdTypeEcpm3d();
        loadAdTypeEcpm3d();
        loadAdnEcpm3d();
        systemDefaultInstanceEcpm = dm.floatVal("/om/system_instance_defualt_ecpm", 1.0F);
        defalutEcpmAlgorithmId = dm.intVal("/om/default_ecpm_algorithm_id", 2);
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

    private void loadAdNetworkAdTypeCountryEcpm3d() {
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

    private void loadInstanceCountryEcpm7d() {
        load("instance_country_ecpm_7d", in -> {
            Map<Integer, Map<String, TreeMap<Integer, BigDecimal[]>>> map = newMap(this.instanceCountryEcpm7d, 1000, 100);
            while (true) {
                StatPB.InstanceCountryDayEcpm o = StatPB.InstanceCountryDayEcpm.parseDelimitedFrom(in);
                if (o == null) break;
                BigDecimal[] imprCost = map.computeIfAbsent(o.getInstanceId(), k -> new HashMap<>())
                        .computeIfAbsent(o.getCountry(), k -> new TreeMap<>(Collections.reverseOrder()))
                        .computeIfAbsent(NumberUtils.toInt(o.getDay()), k -> new BigDecimal[2]);
                imprCost[0] = new BigDecimal(String.valueOf(o.getImpr()));
                imprCost[1] = new BigDecimal(String.valueOf(o.getCost()));
            }
            this.instanceCountryEcpm7d = map;
        });
    }

    private void loadAdnAdTypeCountryEcpm3d() {
        load("adn_adtype_country_ecpm_3d", in -> {
            Map<Integer, Map<Integer, Map<String, Float>>> map = newMap(this.adnAdTypeCountryEcpm, 1000, 100);
            while (true) {
                StatPB.AdnCountryAdTypeEcpm o = StatPB.AdnCountryAdTypeEcpm.parseDelimitedFrom(in);
                if (o == null) break;
                map.computeIfAbsent(o.getAdnId(), k -> new HashMap<>())
                        .computeIfAbsent(o.getAdType(), k -> new HashMap<>())
                        .put(o.getCountry(), o.getEcpm());
            }
            this.adnAdTypeCountryEcpm = map;
        });
    }

    private void loadAdTypeCountryEcpm3d() {
        load("adtype_country_ecpm_3d", in -> {
            Map<Integer, Map<String, Float>> map = newMap(this.adTypeCountryEcpm, 1000, 100);
            while (true) {
                StatPB.CountryAdTypeEcpm o = StatPB.CountryAdTypeEcpm.parseDelimitedFrom(in);
                if (o == null) break;
                map.computeIfAbsent(o.getAdType(), k -> new HashMap<>())
                        .put(o.getCountry(), o.getEcpm());
            }
            this.adTypeCountryEcpm = map;
        });
    }

    private void loadCountryEcpm3d() {
        load("country_ecpm_3d", in -> {
            Map<String, Float> map = newMap(this.countryEcpm, 1000, 100);
            while (true) {
                StatPB.CountryAdTypeEcpm o = StatPB.CountryAdTypeEcpm.parseDelimitedFrom(in);
                if (o == null) break;
                map.put(o.getCountry(), o.getEcpm());
            }
            this.countryEcpm = map;
        });
    }

    private void loadAdnAdTypeEcpm3d() {
        load("adn_adtype_ecpm_3d", in -> {
            Map<Integer, Map<Integer, Float>> map = newMap(this.adnAdTypeEcpm, 1000, 100);
            while (true) {
                StatPB.AdnAdTypeEcpm o = StatPB.AdnAdTypeEcpm.parseDelimitedFrom(in);
                if (o == null) break;
                map.computeIfAbsent(o.getAdnId(), k -> new HashMap<>())
                        .put(o.getAdType(), o.getEcpm());
            }
            this.adnAdTypeEcpm = map;
        });
    }

    private void loadAdTypeEcpm3d() {
        load("adtype_ecpm_3d", in -> {
            Map<Integer, Float> map = newMap(this.adTypeEcpm, 1000, 100);
            while (true) {
                StatPB.AdTypeEcpm o = StatPB.AdTypeEcpm.parseDelimitedFrom(in);
                if (o == null) break;
                map.put(o.getAdType(), o.getEcpm());
            }
            this.adTypeEcpm = map;
        });
    }

    private void loadAdnEcpm3d() {
        load("adn_ecpm_3d", in -> {
            Map<Integer, Float> map = newMap(this.adnEcpm, 1000, 100);
            while (true) {
                StatPB.AdnEcpm o = StatPB.AdnEcpm.parseDelimitedFrom(in);
                if (o == null) break;
                map.put(o.getAdnId(), o.getEcpm());
            }
            this.adnEcpm = map;
        });
    }

    // 老的Ecpm处理
    public InstanceEcpm getOldInstanceEcpm(int mid, int iid, String country, int placementType) {
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

    public InstanceEcpm getInstanceEcpm(Instance instance, int algorithmId, String country, int adType) {
        switch (algorithmId) {
            case 1:
            default:
                return getOldInstanceEcpm(instance.getId(), instance.getId(), country, adType);
            case 2:
                return getExpSmoothInstanceEcpm(instance, country, adType);
        }
    }

    // 新ecpm算法 指数平滑
    private InstanceEcpm getExpSmoothInstanceEcpm(Instance instance, String country, int adType) {
        TreeMap<Integer, BigDecimal[]> dayEcpm = instanceCountryEcpm7d.getOrDefault(instance.getId(), Collections.emptyMap())
                .get(country);
        if (isColdStart(instance.getId(), country)) {//冷启动
            return getColdStartEcpm(instance, dayEcpm, country, adType);
        } else {//通过一次指数平滑取Ecpm
            BigDecimal[] ecpms = new BigDecimal[dayEcpm.size()];
            final int[] index = {dayEcpm.size() - 1};//数据时间序列由于是降序，需要倒置下
            dayEcpm.forEach((day, imprCost) -> {
                ecpms[index[0]] = imprCost[1].multiply(d1000).divide(imprCost[0], 6, BigDecimal.ROUND_HALF_UP);
                index[0]--;
            });
            //离散系数 coefficient of variation
            float cov = MathUtil.cov(ecpms).floatValue();
            //1.离散系数 > 0.15, level = 0.9
            //2.离散系数 > 0.03, 且<0.15, level = 0.6
            //3.离散系数 < 0.03, level =0.3
            float level;
            if (cov >= 0.15F) {
                level = 0.9F;
            } else if (cov > 0.03 && cov < 0.15F) {
                level = 0.6F;
            } else {
                level = 0.3F;
            }
            BigDecimal smoothParam = new BigDecimal(String.valueOf(level));
            BigDecimal predictVal = MathUtil.expSmoothing(smoothParam, ecpms);
            return new InstanceEcpm(instance.getId(), predictVal.floatValue(), "expSmooth",
                    cov, smoothParam, ecpms);
        }
    }

    //获取冷启动Ecpm
    private InstanceEcpm getColdStartEcpm(Instance instance, TreeMap<Integer, BigDecimal[]> dayEcpm, String country, int adType) {
        Float ecpm = null;
        String dataLevel = "";
        int adnId = instance.getAdnId();
        if (dayEcpm != null && !dayEcpm.isEmpty()) {
            AtomicInteger totalImpr = new AtomicInteger();
            final BigDecimal[] totalCost = {BigDecimal.ZERO};
            dayEcpm.forEach((day, imprCost) -> {
                totalImpr.addAndGet(imprCost[0].intValue());
                totalCost[0] = totalCost[0].add(imprCost[1]);
            });
            if (totalImpr.intValue() > 100) {//历史 Total Impressions > 100 eCPM 1. 历史均值eCPM（1-2day）
                ecpm = totalCost[0].divide(BigDecimal.valueOf(totalImpr.intValue()), 6, BigDecimal.ROUND_HALF_UP)
                        .multiply(d1000).floatValue();
                dataLevel = "instanceCountryEcpm-TotalGather100";
            } else {
                ecpm = instance.getInstanceManualEcpm(country);
                dataLevel = "manualEcpm";
                if (ecpm == null) {
                    ecpm = totalCost[0].divide(BigDecimal.valueOf(totalImpr.intValue()), 6, BigDecimal.ROUND_HALF_UP)
                            .multiply(d1000).floatValue();
                    dataLevel = "instanceCountryEcpm-TotalLess100";
                }
            }
        }
        if (ecpm == null) {
            ecpm = instance.getInstanceManualEcpm(country);
            dataLevel = "manualEcpm";
        }
        if (ecpm == null) {
            ecpm = adnAdTypeCountryEcpm.getOrDefault(adnId, Collections.emptyMap())
                    .getOrDefault(adType, Collections.emptyMap())
                    .get(country);
            dataLevel = "adnAdTypeCountryEcpm3d";
            if (ecpm == null) {
                ecpm = adTypeCountryEcpm.getOrDefault(adType, Collections.emptyMap())
                        .get(country);
                dataLevel = "adTypeCountryEcpm3d";
            }
            if (ecpm == null) {
                ecpm = countryEcpm.get(country);
                dataLevel = "countryEcpm3d";
            }
            if (ecpm == null) {
                ecpm = adnAdTypeEcpm.getOrDefault(adnId, Collections.emptyMap()).get(adType);
                dataLevel = "adnAdTypeEcpm3d";
            }
            if (ecpm == null) {
                ecpm = adTypeEcpm.get(adType);
                dataLevel = "adnAdTypeEcpm3d";
            }
            if (ecpm == null) {//
                ecpm = adnEcpm.get(adnId);
                dataLevel = "adnEcpm3d";
            }
            if (ecpm == null) {
                ecpm = systemDefaultInstanceEcpm;
                dataLevel = "defaultInstanceEcpm";
            }
        }
        return new InstanceEcpm(instance.getId(), ecpm, dataLevel);
    }

    //是否满足冷启动条件
    //1.昨日 Impressions <50
    //2.且，数据天数 <3
    private boolean isColdStart(int iid, String country) {
        TreeMap<Integer, BigDecimal[]> dayEcpm = instanceCountryEcpm7d.getOrDefault(iid, Collections.emptyMap())
                .get(country);
        if (dayEcpm != null) {
            if (dayEcpm.size() > 3) {
                return false;
            }
            BigDecimal[] imprCost = dayEcpm.get(dayEcpm.firstKey());
            return imprCost == null || imprCost[0].intValue() < 50;
        }
        return true;
    }
}
