package com.adtiming.om.server.web;

import com.adtiming.om.pb.AdNetworkPB;
import com.adtiming.om.server.dto.*;
import com.adtiming.om.server.service.*;
import com.adtiming.om.server.util.Compressor;
import com.adtiming.om.server.util.RandomUtil;
import com.adtiming.om.server.util.Util;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.adtiming.om.server.dto.AdNetwork.*;
import static com.adtiming.om.server.dto.WaterfallResponse.CODE_NOAVAILABLE_INSTANCE;
import static java.nio.charset.StandardCharsets.UTF_8;

public class WaterfallBase extends BaseController {

    private static final Logger LOG = LogManager.getLogger();

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private CacheService cacheService;

    @Resource
    private LogService logService;

    @Resource
    private WaterfallEcpmService ecpmService;


    private final RequestConfig bidReqCfg = RequestConfig.custom()
            .setConnectTimeout(2000)
            .setSocketTimeout(1000)
            .setRedirectsEnabled(false)
            .build();
    private final Header bidReqUa = new BasicHeader("User-Agent", "om-bid-s2s");
    private final Header bidReqAe = new BasicHeader("Accept-Encoding", "gzip, deflate");

    @Resource
    private HttpAsyncClient httpAsyncClient;

    @Resource
    private ResponseContentEncoding responseContentEncoding;

    WaterfallRequest fillRequestParams(byte[] data, int apiv, String sdkv, int plat, String ua, String reqHost, HttpServletRequest req, GeoService geoService, AppConfig cfg, CacheService cacheService) {
        WaterfallRequest o;
        try {
            o = objectMapper.readValue(Compressor.gunzip2s(data), WaterfallRequest.class);
            o.setApiv(apiv);
            o.setSdkv(sdkv);
            o.setPlat(plat);
            o.setUa(ua);
            o.setReqHost(reqHost);
            o.setGeo(geoService.getGeoData(req, o));
            o.setAppConfig(cfg);
            o.processBidPrices(cacheService);
            o.setMtype(Util.getModelType(plat, o.getModel(), ua));
        } catch (Exception e) {
            LOG.warn("wf decode fail v{}", apiv, e);
            return null;
        }
        return o;
    }

    Set<Integer> getBlockAdnIds(List<AdNetworkApp> adnApps, WaterfallRequest o) {
        Set<Integer> removeAdnIds = Collections.emptySet();
        for (AdNetworkApp app : adnApps) {
            if (app.isBlock(o, "*")) {
                if (removeAdnIds.isEmpty())
                    removeAdnIds = new HashSet<>();
                removeAdnIds.add(app.getAdnId());
            }
        }
        return removeAdnIds;
    }

    List<Instance> matchDev(Integer devDevicePubId, Placement p, CacheService cacheService) {
        if (devDevicePubId != null) {
            // When devDevicePubId is 0, all apps are remediated in test mode
            if (devDevicePubId == 0 || devDevicePubId == p.getPublisherId()) {
                Integer adnId = cacheService.getDevAppAdnId(p.getPubAppId());
                if (adnId != null) {
                    return cacheService.getPlacementAdnInstances(p.getId(), adnId);
                }
            }
        }
        return null;
    }

    boolean matchPlacement(WaterfallRequest o, Placement p) {
        if (p.isBlockSdkVersion(o.getSdkv())) {
            return true;
        }

        if (!p.isAllowOsv(o.getOsv())) {
            return true;
        }

        if (!p.isAllowMake(o.getMake())) {
            return true;
        }

        if (!p.isAllowBrand(o.getBrand())) {
            return true;
        }

        if (!p.isAllowModel(o.getModel())) {
            return true;
        }

        if (p.isBlockDeviceId(o.getDid())) {
            return true;
        }

        return !p.isAllowPeriod(o.getCountry());
    }

    InstanceRule getMatchedRule(List<InstanceRule> rules, WaterfallRequest o) {
        if (rules == null || rules.isEmpty())
            return null;
        for (InstanceRule rule : rules) {
            if (rule.isMatched(o)) {
                return rule;
            }
        }
        return null;
    }

    public static class WaterfallInstance {
        public Instance instance;
        public float ecpm;

        public WaterfallInstance(Instance instance, float ecpm) {
            this.instance = instance;
            this.ecpm = ecpm;
        }

        @Override
        public String toString() {
            return Integer.toString(instance.getId());
        }
    }

    List<WaterfallInstance> getIns(Integer devDevicePubId, WaterfallRequest o, Placement p, WfResInterface res,
                                   @Nullable Map<Integer, Instance> returnBidIids, InstanceRule rule) {
        //dev mode
        List<Instance> devIns = matchDev(devDevicePubId, p, cacheService);
        if (devIns != null && !devIns.isEmpty()) {
            List<WaterfallInstance> rv = new ArrayList<>(devIns.size());
            for (Instance ins : devIns) {
                if (ins.isHeadBidding()) {
                    AdNetworkPB.AdNetwork adn = cacheService.getAdNetwork(ins.getAdnId());
                    //The previous version of waterfall V4 Waterfall does not modify the Bidding processing logic
                    if (!o.isOmWaterfallV4() || (o.isOmWaterfallV4() && adn.getBidType() != 3)) {
                        if (returnBidIids != null) returnBidIids.put(ins.getId(), ins);
                        // Remove bid instance of nobid (Standard C2S & S2S)
                        continue;
                    }
                }
                rv.add(new WaterfallInstance(ins, 0F));
            }
            return rv.isEmpty() ? null : rv;
        }


        InstanceRule matchedRule = rule;
        if (matchedRule == null) {
            List<InstanceRule> rules = cacheService.getCountryRules(p.getId(), o.getCountry());
            matchedRule = getMatchedRule(rules, o);
        }
        if (res.isDebugEnabled()) {
            if (matchedRule != null) {
                res.addDebug("hit rule: %d", matchedRule.getId());
            } else {
                res.addDebug("miss rule");
            }
        }

        List<AdNetworkApp> adnApps = cacheService.getAdnApps(p.getPubAppId());
        if (adnApps == null || adnApps.isEmpty()) {
            return null;
        }

        Set<Integer> blockAdnIds = getBlockAdnIds(adnApps, o);

        List<Instance> mps = cacheService.getPlacementInstances(p.getId());
        if (mps == null || mps.isEmpty()) {
            return null;
        }

        List<Instance> pmInstances = new ArrayList<>(mps);
        List<WaterfallInstance> sortIns;
        if (matchedRule != null) {//has rule
            Set<Integer> insIdSet = matchedRule.getInstanceList();
            if (res.isDebugEnabled()) {
                res.addDebug("Hit Rule:" + matchedRule.getId());
                res.addDebug("Ecpm Algorithm:" + matchedRule.getAlgorithmId());
                res.addDebug("Rule instance list:" + insIdSet);
            }
            sortIns = getInstanceByRule(matchedRule, pmInstances, returnBidIids, blockAdnIds, o, res);
        } else {// Rule is not configured by ecpm absolute priority ordering
            if (res.isDebugEnabled()) {
                res.addDebug("Missmatch mediation rule");
                res.addDebug("Ecpm Algorithm:" + ecpmService.defalutEcpmAlgorithmId);
            }
            Map<Integer, Float> insEcpm = new HashMap<>(pmInstances.size());
            float totalEcpm = 0;
            Map<Float, List<WaterfallInstance>> priorityIns = new HashMap<>(pmInstances.size());
            for (Instance ins : pmInstances) {
                if (blockAdnIds.contains(ins.getAdnId())
                        || o.getBidPriceMap().containsKey(ins.getId())
                        || !ins.matchInstance(o)) {
                    continue;
                }
                AdNetworkPB.AdNetwork adn = cacheService.getAdNetwork(ins.getAdnId());
                //The previous version of waterfall V4 Waterfall does not modify the Bidding processing logic
                if (ins.isHeadBidding() && (!o.isOmWaterfallV4()
                        || (o.isOmWaterfallV4() && adn.getBidType() != 3))) {
                    if (returnBidIids != null) returnBidIids.put(ins.getId(), ins);
                    // Remove bid instance of nobid (Standard C2S & S2S)
                    continue;
                }

                totalEcpm += setInstanceEcpm(o, res, null, ins, priorityIns, insEcpm, totalEcpm);
            }
            if (priorityIns.isEmpty()) {
                res.addDebug("Has no instance match");
                return null;
            }
            if (priorityIns.containsKey(-1F)) {//No ecpm use average ecpm
                float avgEcpm = totalEcpm > 0 ? totalEcpm / insEcpm.size() : 0;
                List<WaterfallInstance> needAvgEcpmIns = priorityIns.get(-1F);
                if (res.isDebugEnabled() && !CollectionUtils.isEmpty(needAvgEcpmIns)) {
                    res.addDebug("No rule auto optimize,Instance AvgEcpm:%f,UseAvgEcpm:%s", avgEcpm, needAvgEcpmIns);
                }
                needAvgEcpmIns.forEach(wfIns -> wfIns.ecpm = avgEcpm);
                priorityIns.computeIfAbsent(avgEcpm, k -> new ArrayList<>()).addAll(needAvgEcpmIns);
                priorityIns.remove(-1F);
            }
            sortIns = Util.sortByEcpm(priorityIns);
            if (res.isDebugEnabled()) {
                res.addDebug("Instance Ecpm: %s", insEcpm);
                res.addDebug("Instance sort: %s", sortIns);
            }
        }
        return sortIns;
    }

    private List<WaterfallInstance> getInstanceByRule(InstanceRule rule, List<Instance> insList,
                                                      @Nullable Map<Integer, Instance> returnBidInsMap,
                                                      Set<Integer> blockAdnIdSet, WaterfallRequest req, WfResInterface res) {
        if (rule == null || CollectionUtils.isEmpty(insList))
            return Collections.emptyList();
        Map<Integer, InstanceRuleGroup> ruleGroupMap = rule.getRuleGroups();
        if (ruleGroupMap.isEmpty())
            return Collections.emptyList();
        List<WaterfallInstance> returnIns = new ArrayList<>();
        Map<WaterfallInstance, Integer> insWeight = new HashMap<>();
        TreeMap<Float, WaterfallInstance> biddingIns = new TreeMap<>();
        for (Map.Entry<Integer, InstanceRuleGroup> entry : ruleGroupMap.entrySet()) {
            Integer groupLevel = entry.getKey();
            InstanceRuleGroup group = entry.getValue();
            if (rule.getSortType() == 0) {//Weight sorting regardless of group configuration
                Set<Integer> insIds = group.getInsList();
                Map<Integer, Integer> ruleInsWeight = group.getInsPriority();
                for (int insId : insIds) {
                    Instance ins = cacheService.getInstanceById(insId);
                    if (ins == null || blockAdnIdSet.contains(ins.getAdnId())
                            || req.getBidPriceMap().containsKey(ins.getId())
                            || !ins.matchInstance(req)) {
                        continue;
                    }
                    AdNetworkPB.AdNetwork adn = cacheService.getAdNetwork(ins.getAdnId());
                    //The previous version of waterfall V4 Waterfall does not modify the Bidding processing logic
                    if (ins.isHeadBidding() && (!req.isOmWaterfallV4()
                            || (req.isOmWaterfallV4() && adn.getBidType() != 3))) {
                        if (returnBidInsMap != null) returnBidInsMap.put(ins.getId(), ins);
                        // Remove bid instance of nobid (Standard C2S & S2S)
                        continue;
                    }
                    InstanceEcpm ecpm = ecpmService.getInstanceEcpm(ins, rule.getAlgorithmId(), req.getCountry(),
                            req.getAdType());
                    Float bidPrice = null;
                    if (!req.getBidPriceMap().isEmpty()) {
                        bidPrice = req.getBidPrice(insId);
                    }
                    if (bidPrice != null) {
                        ecpm.ecpm = bidPrice;
                        ecpm.dataLevel = "bidCachePrice";
                    }
                    WaterfallInstance wfIns = new WaterfallInstance(ins, ecpm.ecpm);
                    if (ins.isHeadBidding()) {
                        if (res.isDebugEnabled()) {
                            res.addDebug("Bidding Instance,AdnId:%d,instanceId:%d,InstanceName:%s,Ecpm:%f,Ecpm Level:%s,data:%s",
                                    adn.getId(), ins.getId(), ins.getName(), ecpm.ecpm, ecpm.dataLevel,
                                    JSON.toJSONString(ecpm.ecpmDatas));
                        }
                        biddingIns.put(ecpm.ecpm, wfIns);
                    } else {
                        if (res.isDebugEnabled()) {
                            res.addDebug("AdnId:%d,instanceId:%d,InstanceName:%s,Ecpm:%f,Ecpm Level:%s,data:%s",
                                    adn.getId(), ins.getId(), ins.getName(), ecpm.ecpm, ecpm.dataLevel,
                                    JSON.toJSONString(ecpm.ecpmDatas));

                        }
                        insWeight.put(wfIns, ruleInsWeight.get(insId));
                    }
                }
            } else {
                if (group.getAutoSwitch() == 1) {//auto waterfall sorted by ecpm
                    Set<Integer> insIds = group.getInsList();
                    if (res.isDebugEnabled()) {
                        res.addDebug("Group:%d,autoSwitch:%s", groupLevel, group.getAutoSwitch() == 1 ? "On" : "Off");
                        res.addDebug("Group instances:%s", JSON.toJSONString(insIds));
                    }
                    Map<Integer, Float> insEcpm = new HashMap<>(insIds.size());
                    float totalEcpm = 0;
                    Map<Float, List<WaterfallInstance>> sortInsMap = new HashMap<>(insIds.size());
                    for (int insId : insIds) {
                        Instance ins = cacheService.getInstanceById(insId);
                        if (ins == null || blockAdnIdSet.contains(ins.getAdnId())
                                || req.getBidPriceMap().containsKey(ins.getId())
                                || !ins.matchInstance(req)) {
                            continue;
                        }
                        AdNetworkPB.AdNetwork adn = cacheService.getAdNetwork(ins.getAdnId());
                        //The previous version of waterfall V4 Waterfall does not modify the Bidding processing logic
                        if (ins.isHeadBidding() && (!req.isOmWaterfallV4()
                                || (req.isOmWaterfallV4() && adn.getBidType() != 3))) {
                            if (returnBidInsMap != null) returnBidInsMap.put(ins.getId(), ins);
                            // Remove bid instance of nobid (Standard C2S & S2S)
                            continue;
                        }
                        InstanceEcpm ecpm = ecpmService.getInstanceEcpm(ins, rule.getAlgorithmId(), req.getCountry(),
                                req.getAdType());
                        Float bidPrice = null;
                        if (!req.getBidPriceMap().isEmpty()) {
                            bidPrice = req.getBidPrice(insId);
                        }
                        if (bidPrice != null) {
                            ecpm.ecpm = bidPrice;
                            ecpm.dataLevel = "bidCachePrice";
                        }
                        if (ins.isHeadBidding()) {
                            if (res.isDebugEnabled()) {
                                res.addDebug("Bidding Instance,AdnId:%d,instanceId:%d,InstanceName:%s,Ecpm:%f,Ecpm Level:%s,data:%s",
                                        adn.getId(), ins.getId(), ins.getName(), ecpm.ecpm, ecpm.dataLevel,
                                        JSON.toJSONString(ecpm.ecpmDatas));
                            }
                            biddingIns.put(ecpm.ecpm, new WaterfallInstance(ins, ecpm.ecpm));
                        } else {
                            totalEcpm = setInstanceEcpm(req, res, rule, ins, sortInsMap, insEcpm, totalEcpm);
                        }
                    }
                    if (sortInsMap.isEmpty()) {
                        continue;
                    }
                    List<WaterfallInstance> needAvgEcpmIns = sortInsMap.get(-1F);
                    if (needAvgEcpmIns != null) {
                        float avgEcpm = totalEcpm > 0 ? totalEcpm / insEcpm.size() : 0;//Average ecpm
                        if (res.isDebugEnabled()) {
                            res.addDebug("Rule auto optimize,Instance AvgEcpm:%f,UseAvgEcpm:%s", avgEcpm, needAvgEcpmIns);
                        }
                        needAvgEcpmIns.forEach(o -> o.ecpm = avgEcpm);
                        // Average ecpm without ecpm data
                        sortInsMap.computeIfAbsent(avgEcpm, k -> new ArrayList<>()).addAll(needAvgEcpmIns);
                        sortInsMap.remove(-1F);
                    }
                    List<WaterfallInstance> sortIns = Util.sortByEcpm(sortInsMap);//按ecpm排序,相同ecpm shuffle
                    if (res.isDebugEnabled()) {
                        res.addDebug("Instance Ecpm:" + insEcpm);
                        res.addDebug("Instance sort result:" + sortIns);
                    }
                    returnIns.addAll(sortIns);
                } else {//手动排序
                    Map<Integer, Integer> iidConfigPriority = group.getInsPriority();
                    Set<Integer> insIds = group.getInsList();
                    if (res.isDebugEnabled()) {
                        res.addDebug("Group:%d,autoSwitch:%s", groupLevel, group.getAutoSwitch() == 1 ? "On" : "Off");
                        res.addDebug("Group instances:%s", JSON.toJSONString(insIds));
                    }
                    Map<WaterfallInstance, Integer> priorityIns = new HashMap<>(iidConfigPriority.size());
                    float totalEcpm = 0;
                    int ecpmSize = 0;
                    for (int insId : insIds) {
                        Instance ins = cacheService.getInstanceById(insId);
                        if (ins == null || blockAdnIdSet.contains(ins.getAdnId())
                                || req.getBidPriceMap().containsKey(ins.getId())
                                || !ins.matchInstance(req)) {
                            continue;
                        }
                        AdNetworkPB.AdNetwork adn = cacheService.getAdNetwork(ins.getAdnId());
                        //The previous version of waterfall V4 Waterfall does not modify the Bidding processing logic
                        if (ins.isHeadBidding() && (!req.isOmWaterfallV4()
                                || (req.isOmWaterfallV4() && adn.getBidType() != 3))) {
                            if (returnBidInsMap != null) returnBidInsMap.put(ins.getId(), ins);
                            // Remove bid instance of nobid (Standard C2S & S2S)
                            continue;
                        }
                        InstanceEcpm ecpm = ecpmService.getInstanceEcpm(ins, rule.getAlgorithmId(), req.getCountry(), req.getAdType());
                        Float bidPrice = null;
                        if (!req.getBidPriceMap().isEmpty()) {
                            bidPrice = req.getBidPrice(insId);
                        }
                        if (bidPrice != null) {
                            ecpm.ecpm = bidPrice;
                            ecpm.dataLevel = "bidCachePrice";
                        }
                        float ecpmVal = 0F;
                        if (ecpm.ecpm > -1F) {
                            ecpmVal = ecpm.ecpm;
                            totalEcpm += ecpm.ecpm;
                            ecpmSize++;
                        }
                        if (ins.isHeadBidding()) {
                            if (res.isDebugEnabled()) {
                                res.addDebug("Bidding Instance,AdnId:%d,instanceId:%d,InstanceName:%s,Ecpm:%f,Ecpm Level:%s,data:%s",
                                        adn.getId(), ins.getId(), ins.getName(), ecpm.ecpm, ecpm.dataLevel,
                                        JSON.toJSONString(ecpm.ecpmDatas));
                            }
                            biddingIns.put(ecpmVal, new WaterfallInstance(ins, ecpmVal));
                        } else {
                            if (res.isDebugEnabled()) {
                                res.addDebug("Instance,AdnId:%d,instanceId:%d,InstanceName:%s,Ecpm:%f,Ecpm Level:%s,data:%s",
                                        adn.getId(), ins.getId(), ins.getName(), ecpm.ecpm, ecpm.dataLevel,
                                        JSON.toJSONString(ecpm.ecpmDatas));
                            }
                            int priority = iidConfigPriority.get(ins.getId());
                            if (priority > 0) {
                                priorityIns.put(new WaterfallInstance(ins, ecpmVal), priority);
                            }
                        }
                    }
                    if (priorityIns.isEmpty()) {
                        continue;
                    }
                    List<WaterfallInstance> sortIns;
                    //Sorting type, 0: use weight; 1: use absolute priority
                    if (rule.getSortType() == 0) {
                        sortIns = RandomUtil.sortByWeight(priorityIns);
                    } else {//In absolute priority, an instance is randomly selected with the same weight
                        sortIns = Util.sortByPriority(priorityIns);
                    }
                    if (totalEcpm > 0) {
                        float avgEcpm = totalEcpm / ecpmSize;
                        sortIns.forEach(o -> {
                            if (o.ecpm == 0) {
                                o.ecpm = avgEcpm;
                            }
                        });
                    }
                    if (res.isDebugEnabled()) {
                        res.addDebug("Instance sort result:%s", sortIns);
                    }
                    returnIns.addAll(sortIns);
                }
            }
        }
        if (rule.getSortType() == 0) {//Sort by weight
            if (biddingIns.isEmpty() && insWeight.isEmpty()) {
                return Collections.emptyList();
            }
            List<WaterfallInstance> sortIns = RandomUtil.sortByWeight(insWeight);
            returnIns.addAll(sortIns);
        }
        if (!biddingIns.isEmpty()) {
            biddingIns.forEach((ecpm, bidIns) -> {
                if (returnIns.isEmpty()) {//Only non-standard c2s scene
                    returnIns.add(bidIns);
                } else {//According to the order of ecpm insertion
                    boolean added = false;
                    for (int i = 0; i < returnIns.size(); i++) {
                        WaterfallInstance ins = returnIns.get(i);
                        if (ins == null || ecpm > ins.ecpm) {
                            added = true;
                            returnIns.add(i, bidIns);
                            break;
                        }
                    }
                    if (!added) {
                        returnIns.add(bidIns);
                    }
                }
            });
        }
        if (!returnIns.isEmpty()) {
            if (res.isDebugEnabled()) {
                List<String> list = new ArrayList<>();
                for (WaterfallInstance wfIns : returnIns) {
                    list.add(String.format("AdnId:%d, instanceId:%d, InstanceName:%s, Ecpm:%f",
                            wfIns.instance.getAdnId(), wfIns.instance.getId(), wfIns.instance.getName(), wfIns.ecpm));
                }
                res.addDebug("Final result:" + JSON.toJSONString(list));
            }
        }
        return returnIns;
    }

    private float setInstanceEcpm(WaterfallRequest req, WfResInterface res, InstanceRule rule, Instance ins,
                                  Map<Float, List<WaterfallInstance>> sortIns,
                                  Map<Integer, Float> insEcpm, float totalEcpm) {
        int algorithmId = rule != null ? rule.getAlgorithmId() : ecpmService.defalutEcpmAlgorithmId;
        int placementType = req.getAdType();
        InstanceEcpm ecpm = ecpmService.getInstanceEcpm(ins, algorithmId, req.getCountry(), placementType);
        Float bidPrice = null;
        if (!req.getBidPriceMap().isEmpty()) {
            bidPrice = req.getBidPrice(ins.getId());
        }
        if (bidPrice != null) {
            ecpm.ecpm = bidPrice;
            ecpm.dataLevel = "bidCachePrice";
        }
        sortIns.computeIfAbsent(ecpm.ecpm, k -> new ArrayList<>()).add(new WaterfallInstance(ins, ecpm.ecpm));
        if (ecpm.ecpm > -1F) {
            insEcpm.put(ins.getId(), ecpm.ecpm);
            totalEcpm += ecpm.ecpm;
        }
        if (res.isDebugEnabled()) {
            res.addDebug("AdnId:%d,instanceId:%d,InstanceName:%s,Ecpm:%f,Ecpm Level:%s,data:%s",
                    ins.getAdnId(), ins.getId(), ins.getName(), ecpm.ecpm, ecpm.dataLevel,
                    JSON.toJSONString(ecpm.ecpmDatas));
        }
        return totalEcpm;
    }

    /*void removeUnsupportedInstance(CacheService cs, int deviceAbTest, boolean isAppAbTestOn,
                                   Placement p, List<Instance> instances) {
        //App opens abtest, placement is not open, device belongs to group B
        if (isAppAbTestOn && !p.isAbTestOn() && deviceAbTest == 1) {
            List<AdNetworkApp> bGroupAdnApp = cs.getAdnApps(p.getPubAppId());
            if (bGroupAdnApp != null && !bGroupAdnApp.isEmpty()) {
                Map<Integer, AdNetworkApp> bGroupMsdkAppMap = bGroupAdnApp.stream().collect(Collectors.toMap(AdNetworkApp::getAdnId, o -> o));
                //Remove AdNetwork not included in group B
                instances.removeIf(o -> !bGroupMsdkAppMap.containsKey(o.getAdnId()));
            }
        }
    }*/

    public List<Integer> getInsWithBidInstance(WaterfallRequest req, List<WaterfallInstance> insList) {
        List<WaterfallInstance> list = getWfInsWithBidInstance(req, insList);
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return list.stream().map(o -> o.instance.getId()).collect(Collectors.toList());
    }

    public List<WaterfallInstance> getWfInsWithBidInstance(WaterfallRequest req, List<WaterfallInstance> insList) {
        if (CollectionUtils.isEmpty(insList) && req.getBidPriceMap().isEmpty()) {
            return null;
        }

        if (CollectionUtils.isEmpty(insList)) {
            insList = new ArrayList<>();
        }

        // insert bid instance
        if (!req.getBidPriceMap().isEmpty()) {
            List<WaterfallInstance> finalInsList = insList;
            req.getBidPriceMap().forEach((iid, bidPrice) -> {
                boolean added = false;
                Instance ins = cacheService.getInstanceById(iid);
                if (ins == null) {
                    return;
                }
                WaterfallInstance bidIns = new WaterfallInstance(ins, bidPrice);
                for (int i = 0; i < finalInsList.size(); i++) {
                    float ecpm = finalInsList.get(i).ecpm;
                    if (bidPrice > ecpm) {
                        finalInsList.add(i, bidIns);
                        added = true;
                        break;
                    }
                }
                if (!added) {
                    finalInsList.add(bidIns);
                }
            });
        }
        return insList;
    }

    @FunctionalInterface
    public interface S2SBidCallback {
        void cb(DeferredResult<Object> dr);
    }

    protected DeferredResult<Object> bid(WaterfallRequest o, LrRequest wfLr, boolean isTest, Placement placement, WfResInterface resp, S2SBidCallback callback) {
        final boolean DEBUG = resp.getDebug() != null;
        resp.initBidResponse(new ArrayList<>(o.getBids2s().size()));
        DeferredResult<Object> dr = new DeferredResult<>(1500L);

        AtomicInteger count = new AtomicInteger(o.getBids2s().size());
        for (WaterfallRequest.S2SBidderToken bidderToken : o.getBids2s()) {
            HttpPost bidreq = new HttpPost(bidderToken.endpoint);
            bidreq.setConfig(bidReqCfg);
            bidreq.setHeader(bidReqUa);
            bidreq.setHeader(bidReqAe);
            if (bidderToken.adn == ADN_FACEBOOK) {
                bidreq.setHeader("X-FB-Pool-Routing-Token", bidderToken.token);
            } else if (bidderToken.adn == ADN_MINTEGRAL) {
                bidreq.setHeader("openrtb", "2.5");
            } else if (bidderToken.adn == ADN_VUNGLE) {
                bidreq.setHeader("X-OpenRTB-Version", "2.5");
                bidreq.setHeader("Accept", "application/json");
            }

            String reqData = buildBidReqData(o, isTest, placement, bidderToken).toJSONString();
            bidreq.setEntity(new StringEntity(reqData, ContentType.APPLICATION_JSON));

            if (DEBUG) {
                resp.getDebug().add(String.format("bidreq to %d, %s, bid: %s", bidderToken.iid, bidderToken.endpoint, reqData));
            }

            // write bid request log
            LrRequest lr = o.copyTo(new LrRequest());
            lr.setBid(1);
            lr.setBidReq(1);
            lr.setType(EventLogRequest.INSTANCE_BID_REQUEST);
            lr.setMid(bidderToken.adn);
            lr.setPlacement(placement);
            lr.setIid(bidderToken.iid);
            lr.setRuleId(wfLr.getRuleId());
            lr.setRuleType(wfLr.getRuleType());
            lr.setRp(wfLr.getRp());
            lr.setIi(wfLr.getIi());
            lr.setAdnPk(bidderToken.pkey);
//            lr.writeToLog(logService);

            HttpClientContext hcc = HttpClientContext.create();
            httpAsyncClient.execute(bidreq, hcc, new FutureCallback<HttpResponse>() {
                @Override
                public void completed(HttpResponse result) {
                    HttpEntity entity = null;
                    String content = null;
                    JSONObject bidresp = null;
                    String err = null;
                    String headers = null;
                    try {
                        if (DEBUG) {
                            headers = StringUtils.join(result.getAllHeaders(), '\n');
                        }
                        responseContentEncoding.process(result, hcc);
                        entity = result.getEntity();
                        StatusLine sl = result.getStatusLine();
                        int code = sl.getStatusCode();
                        if (code != 200) {
                            if (bidderToken.adn == ADN_FACEBOOK) {
                                Header hErr = result.getFirstHeader("x-fb-an-errors");
                                if (hErr != null) {
                                    err = hErr.getValue();
                                }
                            }
                            LOG.debug("pid: {}, adn {} nobid: {}, err: {}", o.getPid(), bidderToken.adn, sl, err);
                        }
                        if (entity != null) {
                            content = EntityUtils.toString(entity, UTF_8);
                            if (ContentType.APPLICATION_JSON.getMimeType().equals(ContentType.get(entity).getMimeType())) {
                                bidresp = JSONObject.parseObject(content);
                            }
                        }
                        if (DEBUG) {
                            resp.getDebug().add(String.format("bidresp from %d, headers: %s, body: %s",
                                    bidderToken.iid, headers, content));
                        }
                    } catch (Exception e) {
                        if (DEBUG) {
                            resp.getDebug().add(String.format("bidresp from %d error, %s, headers: %s, body: %s",
                                    bidderToken.iid, e, headers, content));
                        }
                        LOG.error("adn {} parse error, req: {}, resp: {}", bidderToken.adn, reqData, content, e);
                    } finally {
                        EntityUtils.consumeQuietly(entity);
                    }
                    handleS2SBidResponse(count, bidderToken, isTest, bidresp, err, o, resp, lr, callback, dr);
                }

                @Override
                public void failed(Exception ex) {
                    String msg = ex.toString();
                    LOG.debug("adn {} failed: {}", bidderToken.adn, msg);
                    if (DEBUG) {
                        resp.getDebug().add(String.format("bidresp from %d failed: %s", bidderToken.iid, ex));
                    }
                    handleS2SBidResponse(count, bidderToken, isTest, null, msg, o, resp, lr, callback, dr);
                }

                @Override
                public void cancelled() {
                    handleS2SBidResponse(count, bidderToken, isTest, null, "cancelled", o, resp, lr, callback, dr);
                }
            });
        }
        return dr;
    }

    private void handleS2SBidResponse(
            AtomicInteger count, WaterfallRequest.S2SBidderToken bidderToken, boolean isTest,
            JSONObject bidresp, String err, WaterfallRequest o, WfResInterface resp, LrRequest lr,
            S2SBidCallback callback, DeferredResult<Object> dr) {
        try {
            WaterfallResponse.S2SBidResponse bres = new WaterfallResponse.S2SBidResponse();
            bres.iid = bidderToken.iid;
            bres.err = err;
            resp.addS2sBidResponse(bres);
            if (bidresp == null) {
                bres.nbr = 0;
            } else {
                bres.nbr = bidresp.getInteger("nbr");
                if (bres.nbr != null) {
                    return;
                }
                String cur = bidresp.getString("cur");
//                String bidid = bidresp.getString("bidid");
                JSONObject bid = bidresp
                        .getJSONArray("seatbid").getJSONObject(0)
                        .getJSONArray("bid").getJSONObject(0);
                float price = cacheService.getUsdMoney(cur, bid.getFloatValue("price"));
                bres.price = isTest ? 0F : price; // TestMode, no billing
                bres.adm = bid.getString("adm");
                bres.nurl = bid.getString("nurl");
                bres.lurl = bid.getString("lurl");
                o.setBidPrice(bidderToken.iid, price);
                if (bidderToken.adn == ADN_VUNGLE) {
                    bres.expire = 5;
                } else {
                    bres.expire = 30;
                }

                // write bid response log
//                LrRequest lr = o.copyTo(new LrRequest());
//                lr.setBid(1);
//                lr.setType(EventLogRequest.INSTANCE_BID_RESPONSE);
//                lr.setMid(bidderToken.adn);
//                lr.setPlacement(placement);
//                lr.setIid(bidderToken.iid);
                lr.setBidRes(1);
                if (!isTest) {
                    lr.setBidResPrice(price);
                }
            }
        } catch (Exception e) {
            LOG.error("set s2s bidresp error", e);
        } finally {
            if (count.decrementAndGet() == 0) {
                callback.cb(dr);
            }
            lr.writeToLog(logService);
        }
    }

    private JSONObject buildBidReqData(WaterfallRequest ps, boolean isTest, Placement placement, WaterfallRequest.S2SBidderToken bidderToken) {
        JSONObject bid = new JSONObject().fluentPut("id", ps.getReqId());
        if (isTest) { // Test mode
            bid.put("test", 1);
        }
        bid.put("at", 1);
        bid.put("tmax", 1000);

        if (bidderToken.adn == ADN_ADTIMING || bidderToken.adn == ADN_CROSSPROMOTION) {
            bid.put("user", new JSONObject().fluentPut("id", bidderToken.token));
        } else if (bidderToken.adn == ADN_FACEBOOK || bidderToken.adn == ADN_MINTEGRAL) {
            bid.put("user", new JSONObject().fluentPut("buyeruid", bidderToken.token));
        }

        JSONObject app = new JSONObject()
                .fluentPut("ver", ps.getAppv())
                .fluentPut("bundle", ps.getBundle());

        if (bidderToken.adn == ADN_FACEBOOK) {
            app.put("publisher", new JSONObject().fluentPut("id", bidderToken.appId));
            bid.put("ext", new JSONObject().fluentPut("platformid", bidderToken.appId));
        } else if (bidderToken.adn == ADN_MINTEGRAL) {
            int i = bidderToken.appId.indexOf('#');
            app.put("id", i == -1 ? bidderToken.appId : bidderToken.appId.substring(0, i));
        } else {
            app.put("id", bidderToken.appId);
        }
        bid.put("app", app);

        JSONObject device = new JSONObject()
                .fluentPut("ip", ps.getIp())
                .fluentPut("ifa", ps.getDid())
                .fluentPut("ua", ps.getUa())
                .fluentPut("make", ps.getMake())
                .fluentPut("model", ps.getModel())
                .fluentPut("os", ps.getPlat() == 0 ? "iOS" : "Android")
                .fluentPut("osv", ps.getOsv())
                .fluentPut("language", ps.getLang())
                .fluentPut("carrier", ps.getCarrier())
                .fluentPut("mccmnc", ps.getMccmnc())
                .fluentPut("connectiontype", ps.getContype());
        if (bidderToken.adn == ADN_MINTEGRAL) {
            device.fluentPut("w", ps.getWidth()).fluentPut("h", ps.getHeight());
        }
        bid.put("device", device);

        JSONObject imp = new JSONObject()
                .fluentPut("id", "1")
                .fluentPut("tagid", bidderToken.pkey);

        if (bidderToken.adn == ADN_VUNGLE) {
            imp.put("ext", Collections.singletonMap("vungle", Collections.singletonMap("bid_token", bidderToken.token)));
        }

        JSONObject size = new JSONObject()
                .fluentPut("w", ps.getWidth())
                .fluentPut("h", ps.getHeight());
        switch (placement.getAdType()) {
            case Banner:
                if (bidderToken.adn == ADN_FACEBOOK) {
                    size.put("w", -1);
                }
                imp.put("banner", size);
                break;
            case RewardVideo:
                if (bidderToken.adn == ADN_FACEBOOK) {
                    size.put("ext", new JSONObject().fluentPut("videotype", "rewarded"));
                    size.fluentPut("w", 0).fluentPut("h", 0);
                }
                imp.put("video", size);
                break;
            case Interstitial:
                if (bidderToken.adn == ADN_FACEBOOK) {
                    size.fluentPut("w", 0).fluentPut("h", 0);
                    imp.put("instl", 1);
                    imp.put("banner", size);
                } else {
                    imp.put("video", size);
                }
                break;
            case Native:
                if (bidderToken.adn == ADN_FACEBOOK) {
                    size.fluentPut("w", -1).fluentPut("h", -1);
                }
                imp.put("native", size);
                break;
            default:
                LOG.warn("unsupport adType for bid request");
                break;
        }
        bid.put("imp", Collections.singletonList(imp));

        Regs dRegs = ps.getRegs();
        if (dRegs != null) {
            JSONObject regs = new JSONObject();
            if (dRegs.getCoppa() != null) {
                regs.put("coppa", dRegs.getCoppa());
            }
            if (dRegs.getGdpr() != null) {
                ((JSONObject) regs.computeIfAbsent("ext", k -> new JSONObject())).put("gdpr", dRegs.getGdpr());
            }
            if (dRegs.getCcpa() != null) {
                ((JSONObject) regs.computeIfAbsent("ext", k -> new JSONObject())).put("ccpa", dRegs.getCcpa());
            }
            if (!regs.isEmpty()) {
                bid.put("regs", regs);
            }
        }

        return bid;
    }

    public ResponseEntity<?> asyncExceptionHandler(HttpServletRequest req) throws IOException {
        WaterfallResponse res = (WaterfallResponse) req.getAttribute("res");
        LrRequest lr = (LrRequest) req.getAttribute("lr");
        WaterfallRequest o = (WaterfallRequest) req.getAttribute("params");
        if (res == null || lr == null || o == null) {
            return ResponseEntity.noContent().build();
        }
        Object insObject = req.getAttribute("insList");
        if (insObject != null) {
            //noinspection unchecked
            List<WaterfallInstance> insList = (List<WaterfallInstance>) insObject;
            if (!CollectionUtils.isEmpty(insList)) {
                res.setIns(getInsWithBidInstance(o, insList));
            }
        }
        lr.writeToLog(logService);
        if (res.getIns() == null || res.getIns().isEmpty()) {
            res.setCode(CODE_NOAVAILABLE_INSTANCE);
        }
        return response(res);
    }
}
