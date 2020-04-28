// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.web;

import com.adtiming.om.pb.CommonPB;
import com.adtiming.om.server.dto.*;
import com.adtiming.om.server.service.AppConfig;
import com.adtiming.om.server.service.CacheService;
import com.adtiming.om.server.service.GeoService;
import com.adtiming.om.server.service.LogService;
import com.adtiming.om.server.util.RandomUtil;
import com.adtiming.om.server.util.Util;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

import static com.adtiming.om.server.dto.WaterfallResponse.*;

@Controller
public class WaterfallController extends WaterfallBase {

    private static final Logger LOG = LogManager.getLogger();

    @Resource
    private AppConfig cfg;

    @Resource
    private GeoService geoService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private CacheService cacheService;

    @Resource
    private LogService logService;

    /**
     * waterfall
     */
    @PostMapping(value = "/wf", params = "v=1")
    public void wf(HttpServletRequest req, HttpServletResponse res,
                   @RequestParam("v") int version, // api version
                   @RequestParam("plat") int plat, // platform
                   @RequestParam("sdkv") String sdkv,
                   @RequestHeader("Host") String reqHost,
                   @RequestHeader(value = "User-Agent", required = false) String ua,
                   @RequestHeader(value = "debug", required = false) String debug,
                   @RequestBody byte[] data) throws IOException {
        WaterfallRequest o = fillRequestParams(data, version, sdkv, plat, ua, reqHost, req, geoService, cfg, cacheService);
        if (o == null) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        final boolean DEBUG = debug != null;
        final List<CharSequence> dmsg = DEBUG ? new DebugMsgs() : null;

        LrRequest lr = o.copyTo(new LrRequest());
        lr.setType(LrRequest.TYPE_WATERFALL_REQUEST);

        Placement placement = cacheService.getPlacement(o.getPid());
        if (placement == null) {
            response(res, new WaterfallResponse(CODE_PLACEMENT_INVALID, "placement invalid", CommonPB.ABTest.None_VALUE, dmsg));
            lr.setStatus(0, "placement invalid").writeToLog(logService);
            return;
        }
        o.setPlacement(placement);
        lr.setPlacement(placement);

        o.setAdType(placement.getAdTypeValue());
        o.setAbt(cacheService.getAbTestMode(placement.getId(), o));

        if (DEBUG) {
            dmsg.add(String.format("request ip:%s, country:%s", o.getIp(), o.getCountry()));
            if (o.getAbt() > 0) {
                dmsg.add("Placement ABTest status: On");
                dmsg.add("Placement Device ABTest Mode:" + CommonPB.ABTest.forNumber(o.getAbt()));
            }
        }
        Integer devDevicePubId = cacheService.getDevDevicePub(o.getDid());
        // dev device uses the configured abTest mode
        Integer devAbMode = null;
        if (devDevicePubId != null) {
            if (DEBUG) {
                Integer devAppAdnId = cacheService.getDevAppAdnId(placement.getPubAppId());
                dmsg.add("Is Dev Device,dev publisher:" + devDevicePubId);
                dmsg.add("Is Dev model,Dev Mediation:" + devAppAdnId);
            }
            devAbMode = cacheService.getDevDeviceAbtMode(o.getDid());
        }
        if (devAbMode != null) {
            o.setAbt(devAbMode);
            if (DEBUG) dmsg.add("Dev Device ABTest Mode:" + CommonPB.ABTest.forNumber(o.getAbt()));
        }

        PublisherApp pubApp = cacheService.getPublisherApp(placement.getPubAppId());
        if (pubApp == null) {
            response(res, new WaterfallResponse(CODE_PUB_APP_INVALID, "app invalid", o.getAbt(), dmsg));
            lr.setStatus(0, "app invalid").writeToLog(logService);
            return;
        }

        if (StringUtils.isEmpty(o.getCountry())) {
            response(res, new WaterfallResponse(CODE_COUNTRY_NOT_FOUND, "country not found", o.getAbt(), dmsg));
            lr.setStatus(0, "country not found").writeToLog(logService);
            return;
        }

        /*List<InstanceRule> rules = cacheService.getPlacementRules(placement.getId());
        InstanceRule matchedRule = null;
        for (InstanceRule rule : rules) {
            Segment segment = cacheService.getSegment(rule.getSegmentId());
            if (segment.isMatched(o.getCountry(), o.getContype(), o.getBrand(), o.getModel(), o.getIap(), o.getImprTimes())) {
                if (matchedRule == null || rule.getPriority() < matchedRule.getPriority()) {
                    matchedRule = rule;
                }
            }
        }

        int segmentId = matchedRule != null ? matchedRule.getSegmentId() : 0;
        if (DEBUG && segmentId > 0) {
            dmsg.add("hit segment:" + segmentId);
        }*/

        List<Integer> ins = getIns(devDevicePubId, o, placement, dmsg, DEBUG);

        if (ins == null || ins.isEmpty()) {
            response(res, new WaterfallResponse(CODE_INSTANCE_EMPTY, "instance empty", o.getAbt(), dmsg));
            lr.setStatus(0, "instance empty").writeToLog(logService);
            return;
        }

        WaterfallResponse resp = new WaterfallResponse(0, null, o.getAbt(), dmsg);
        resp.setIns(ins);
        response(res, resp);
        lr.setStatus(1, null).writeToLog(logService);
    }

    List<Integer> getIns(Integer devDevicePubId, WaterfallRequest o, Placement p,
                         List<CharSequence> dmsg, boolean DEBUG) throws IOException {
        //dev mode
        List<Integer> devIns = matchDev(devDevicePubId, p, cacheService);
        if (devIns != null && !devIns.isEmpty()) {
            return devIns;
        }

        //placement target filter
        if (!matchPlacement(o, p)) {
            return null;
        }

        List<InstanceRule> rules = cacheService.getCountryRules(p.getId(), o.getCountry());
        InstanceRule matchedRule = getMatchedRule(cacheService, rules, o, DEBUG, dmsg);

        int segmentId = matchedRule != null ? matchedRule.getSegmentId() : 0;
        if (DEBUG) {
            if (matchedRule != null) {
                dmsg.add("hit rule:" + matchedRule.getId());
            } else {
                dmsg.add("miss rule");
            }
            dmsg.add("hit segment:" + segmentId);
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
        List<Integer> sortIns;
        Map<Integer, Integer> insMediation = new HashMap<>(pmInstances.size());
        if (matchedRule != null) {//has rule
            Set<Integer> insList = matchedRule.getInstanceList();
            if (DEBUG) {
                dmsg.add("Rule instance list:" + objectMapper.writeValueAsString(insList));
            }
            if (insList.isEmpty()) {
                return null;
            }
            if (matchedRule.isAutoOpt()) {//auto waterfall, ecpm priority
                Map<Integer, Float> insEcpm = new HashMap<>(insList.size());
                float totalEcpm = 0;
                Map<Float, List<Integer>> priorityIns = new HashMap<>(insList.size());
                for (Instance ins : pmInstances) {
                    if (!insList.contains(ins.getId())) continue;
                    if (blockAdnIds.contains(ins.getAdnId()) || !ins.matchInstance(o))
                        continue;
                    insMediation.put(ins.getId(), ins.getAdnId());
                    totalEcpm += setInstanceEcpm(o, ins, priorityIns, insEcpm, totalEcpm, DEBUG, dmsg, p.getAdTypeValue(), cacheService);
                }
                if (priorityIns.isEmpty()) {
                    if (DEBUG) {
                        dmsg.add("Has no instance match");
                    }
                    return null;
                }
                if (priorityIns.containsKey(-1F)) {
                    float avgEcpm = totalEcpm / insEcpm.size();//avg ecpm
                    List<Integer> needAvgEcpmIns = priorityIns.get(-1F);
                    if (DEBUG) {
                        dmsg.add(String.format("Rule auto optimize,Instance AvgEcpm:%f,UseAvgEcpm:%s", avgEcpm, objectMapper.writeValueAsString(needAvgEcpmIns)));
                    }
                    priorityIns.put(avgEcpm, needAvgEcpmIns);
                    priorityIns.remove(-1F);
                }
                sortIns = Util.sortByEcpm(priorityIns);
                if (DEBUG) {
                    dmsg.add("Instance Ecpm:" + objectMapper.writeValueAsString(insEcpm));
                    dmsg.add("Instance sort:" + objectMapper.writeValueAsString(sortIns));
                }
            } else { // Non-auto sort by rule configuration
                Map<Integer, Float> instanceEcpm = null;
                if (!o.getBid().isEmpty())
                    instanceEcpm = new HashMap<>(insList.size());

                Map<Integer, Integer> insConfigWeight = matchedRule.getInstanceWeightMap();
                Map<Integer, Integer> insWeight = new HashMap<>(insList.size());
                for (Instance ins : pmInstances) {
                    if (!insList.contains(ins.getId())) continue;
                    if (blockAdnIds.contains(ins.getAdnId()) || !ins.matchInstance(o))
                        continue;
                    insMediation.put(ins.getId(), ins.getAdnId());

                    if (instanceEcpm != null) {
                        if (o.getBid(ins.getId()) != null) {
                            continue; // HeadBidding does not participate in manual sorting
                        }
                        InstanceEcpm ecpm = cacheService.getInstanceEcpm(ins.getAdnId(), ins.getId(), o.getCountry(), p.getAdTypeValue());
                        if (ecpm != null && ecpm.ecpm > -1F) {
                            instanceEcpm.put(ins.getId(), ecpm.ecpm);
                        }
                    }

                    int weight = insConfigWeight.get(ins.getId());
                    if (weight > 0) {
                        insWeight.put(ins.getId(), weight);
                    }
                }
                if (insMediation.isEmpty()) {
                    if (DEBUG) {
                        dmsg.add("Has no instance match");
                    }
                    return null;
                }

                // Sorting type, 0: use weight; 1: use absolute priority
                if (matchedRule.getSortType() == 0) {
                    sortIns = RandomUtil.sortByWeight(insWeight);
                } else {
                    // When absolute priority, an instance is randomly selected with the same weight
                    sortIns = Util.sortByPriority(insWeight);
                }

                if (DEBUG) {
                    String sortName = (matchedRule.getSortType() == 1 ? "Priority" : "Weight");
                    dmsg.add(String.format("Sort type:%s, %s:%s", sortName, sortName, objectMapper.writeValueAsString((insWeight))));
                    dmsg.add("Instance sort:" + objectMapper.writeValueAsString(sortIns));
                }

                if (instanceEcpm != null) {// Insert HeadBidding Instance
                    if (!sortIns.isEmpty()) {// There are instances other than fb headerbidding
                        sortIns = new ArrayList<>(sortIns);
                        List<Integer> finalSortIns = sortIns;
                        Map<Integer, Float> finalInstanceEcpm = instanceEcpm;
                        if (!o.getBid().isEmpty() && DEBUG) {
                            dmsg.add(String.format("instance bid info:%s", objectMapper.writeValueAsString(o.getBid())));
                            dmsg.add(String.format("instance ecpm info:%s", objectMapper.writeValueAsString(instanceEcpm)));
                        }
                        o.getBid().forEach((iid, bidPrice) -> {
                            for (int i = 0; i < finalSortIns.size(); i++) {
                                Float ecpm = finalInstanceEcpm.getOrDefault(finalSortIns.get(i), 0f);
                                if (bidPrice > ecpm) {
                                    finalSortIns.add(i, iid);
                                    break;
                                }
                            }
                        });
                    } else {
                        // Only configured with fb headerbidding instance
                        sortIns = Util.sortByPrice(o.getBid());
                    }
                    if (DEBUG) {
                        dmsg.add("Instance finally sort:" + objectMapper.writeValueAsString(sortIns));
                    }
                }
            }
        } else {// Rule is not configured by ecpm absolute priority ordering
            Map<Integer, Float> insEcpm = new HashMap<>(pmInstances.size());
            float totalEcpm = 0;
            Map<Float, List<Integer>> priorityIns = new HashMap<>(pmInstances.size());
            for (Instance ins : pmInstances) {
                if (blockAdnIds.contains(ins.getAdnId()) || !ins.matchInstance(o))
                    continue;
                insMediation.put(ins.getId(), ins.getAdnId());

                totalEcpm += setInstanceEcpm(o, ins, priorityIns, insEcpm, totalEcpm, DEBUG, dmsg,
                        p.getAdTypeValue(), cacheService);
            }
            if (priorityIns.isEmpty()) {
                if (DEBUG) {
                    dmsg.add("Has no instance match");
                }
                return null;
            }
            if (priorityIns.containsKey(-1F)) {//No ecpm use average ecpm
                float avgEcpm = totalEcpm / insEcpm.size();
                List<Integer> needAvgEcpmIns = priorityIns.get(-1F);
                if (DEBUG) {
                    dmsg.add(String.format("No rule auto optimize,Instance AvgEcpm:%f,UseAvgEcpm:%s", avgEcpm, objectMapper.writeValueAsString(needAvgEcpmIns)));
                }
                priorityIns.put(avgEcpm, needAvgEcpmIns);
                priorityIns.remove(-1F);
            }
            sortIns = Util.sortByEcpm(priorityIns);
            if (DEBUG) {
                dmsg.add("Instance Ecpm:" + objectMapper.writeValueAsString(insEcpm));
                dmsg.add("Instance sort:" + objectMapper.writeValueAsString(sortIns));
            }
        }
        return sortIns;
    }

    private float setInstanceEcpm(WaterfallRequest o, Instance ins, Map<Float, List<Integer>> priorityIns,
                                  Map<Integer, Float> insEcpm, float totalEcpm, boolean DEBUG, List<CharSequence> dmsg,
                                  int adType, CacheService cacheService) {
        Float bidPrice = o.getBid(ins.getId());
        if (bidPrice != null) {
            priorityIns.computeIfAbsent(bidPrice, k -> new ArrayList<>()).add(ins.getId());
            insEcpm.put(ins.getId(), bidPrice);
            totalEcpm += bidPrice;
            if (DEBUG) {
                dmsg.add("Instance: " + ins.getId() + ", BidPrice:" + bidPrice);
            }
        } else {
            InstanceEcpm ecpm = cacheService.getInstanceEcpm(ins.getAdnId(), ins.getId(), o.getCountry(), adType);
            priorityIns.computeIfAbsent(ecpm.ecpm, k -> new ArrayList<>()).add(ins.getId());
            if (ecpm.ecpm > -1F) {
                insEcpm.put(ins.getId(), ecpm.ecpm);
            }
            totalEcpm += ecpm.ecpm;
            if (DEBUG) {
                try {
                    dmsg.add("Instance Ecpm:" + objectMapper.writeValueAsString(ecpm));
                } catch (JsonProcessingException e) {
                    LOG.error("error", e);
                }
            }
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

}
