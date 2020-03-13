// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

import com.adtiming.om.pb.AdNetworkPB;
import com.adtiming.om.pb.CommonPB;
import com.adtiming.om.pb.PlacementPB;
import com.adtiming.om.server.service.CacheService;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class InitResponse {

    private PublisherApp pubApp;
    private Map<Integer, AdnAppConf> adnApps = Collections.emptyMap();
    private List<InitPlacement> placements = Collections.emptyList();

    // use for debug only when device is overall dev
    public Integer d;

    private CacheService cs;
    private InitRequest req;
    private API api;
    private Events events;

    public InitResponse(InitRequest req, CacheService cs, PublisherApp pubApp, Integer devDevicePubId, Integer devAdnId) {
        this.req = req;
        this.pubApp = pubApp;
        this.cs = cs;
        // global test device's publisherId is zero
        if (devDevicePubId != null && devDevicePubId == 0)
            d = 1;

        this.api = new API();
        this.api.wf = "https://" + req.getReqHost() + "/wf";
        this.api.lr = "https://" + req.getReqHost() + "/lr";
        this.api.hb = "https://" + req.getReqHost() + "/hb";
        this.api.ic = "https://" + req.getReqHost() + "/ic";
        this.api.iap = "https://" + req.getReqHost() + "/iap";
        this.api.er = "https://" + req.getReqHost() + "/err";

        List<Integer> eIds = pubApp.getEventIds();
        if (!eIds.isEmpty()) {
            events = new Events();
            events.url = "https://" + req.getReqHost() + "/log";
            events.ids = eIds;
        }

        addAdnToResponseList(devAdnId);
        addPlacementToResponseList(devAdnId);
    }

    private void addAdnToResponseList(Integer devAdnId) {
        List<AdNetworkApp> adnApps = cs.getAdnApps(pubApp.getId());
        if (adnApps != null && !adnApps.isEmpty()) {
            if (this.adnApps == Collections.EMPTY_MAP)
                this.adnApps = new HashMap<>(adnApps.size());

            String device = null;
            if (req.getPlat() == CommonPB.Plat.Android_VALUE) {
                device = req.getAndroid().device;
            }

            for (AdNetworkApp adnApp : adnApps) {
                if (this.adnApps.containsKey(adnApp.getAdnId()))
                    continue;
                // dev mode
                if (devAdnId != null) {
                    if (adnApp.getAdnId() == devAdnId) {
                        addApps(adnApp);
                        break;
                    }
                } else {
                    if (adnApp.isBlock(req, device))
                        continue;
                    addApps(adnApp);
                }
            }
        }
    }

    private void addApps(AdNetworkApp m) {
        AdNetworkPB.AdNetwork adn = cs.getAdNetwork(m.getAdnId());
        if (adn != null) {
            AdnAppConf a = new AdnAppConf();
            a.id = adn.getId();
            a.n = adn.getClassName();
            a.k = m.getAppKey();
            this.adnApps.put(a.id, a);
        }
    }

    private void addPlacementToResponseList(Integer devAdnId) {
        List<Placement> placements = cs.getPlacementsByApp(pubApp.getId());
        if (placements != null && !placements.isEmpty()) {
            this.placements = new ArrayList<>(placements.size());
            for (Placement p : placements) {
                Map<Integer, List<Instance>> adnInsListMap = cs.getPlacementAdnInstanceMap(p.getId());

                AtomicBoolean hasHb = new AtomicBoolean(false);
                List<MInstance> pIns = new ArrayList<>(30);
                if (adnInsListMap != null) {
                    if (devAdnId == null) {
                        Set<Integer> ruleInsIdSet = cs.getPlacementCountryRuleInstanceIdSet(p.getId(), req.getCountry());
                        adnInsListMap.forEach((adnId, insList) -> {
                            if (this.adnApps.containsKey(adnId))
                                addInstances(p, pIns, insList, ruleInsIdSet, hasHb);
                        });
                    } else {//dev模式
                        if (this.adnApps.containsKey(devAdnId)) {
                            List<Instance> insList = adnInsListMap.get(devAdnId);
                            addInstances(p, pIns, insList, null, hasHb);
                        }
                    }
                }
                this.placements.add(new InitPlacement(p, pIns, hasHb));
            }
        }
    }

    /**
     * add instances to placement response
     *
     * @param p            placement
     * @param pIns         added to
     * @param insList      make sure this list are from the same AdNetwork
     * @param ruleInsIdSet InstanceRule id set
     */
    private void addInstances(Placement p, List<MInstance> pIns, List<Instance> insList, Set<Integer> ruleInsIdSet, AtomicBoolean hasHb) {
        if (insList == null || insList.isEmpty())
            return;
        boolean allowHb = p.isAllowHb();
        for (Instance i : insList) {
            if (ruleInsIdSet != null && !ruleInsIdSet.contains(i.getId()))
                continue;
            boolean hb = false;
            if (allowHb && !hasHb.get() && i.isHeadBidding()) {
                hasHb.set(hb = true);
            }
            pIns.add(new MInstance(i, hb));
        }
    }

    public API getApi() {
        return api;
    }

    public Events getEvents() {
        return events;
    }

    public Collection<AdnAppConf> getMs() {
        return adnApps.values();
    }

    public List<InitPlacement> getPls() {
        return placements;
    }

    public static class API {
        public String wf, lr, er, iap, ic, hb;
    }

    public static class Events {
        public String url;
        public int mn = 10; // maxNumOfEventsToUpload
        public int ci = 30; // checkInterval
        public Collection<Integer> ids;
    }

    public static class AdnAppConf {
        public int id;
        public String n;
        public String k;
    }

    public static class Scene {
        public int id;
        public String n;
        public Integer isd;
        public Integer fc, fu;
    }

    public static class InitPlacement {
        private Placement p;

        // for json output
        public int id;         // placement ID
        public int t;          // adType
        public Integer main;   // is MainPlacemnt, for RewardVideo & Interstitial
        public Integer bs, fo; // batchSize & fanOut
        public Integer fc, fu; // frequencryCap & frequencryUnit
        public Integer cs, rf; // RewardVideo & Interstitial
        public Integer rlw;    // Banner reload waterfall
        public Integer hb;     // headbidding switch [0,1]
        public List<MInstance> ins;
        public List<Scene> scenes;

        InitPlacement(Placement p, List<MInstance> pIns, AtomicBoolean hasHb) {
            this.p = p;
            this.id = p.getId();
            this.t = p.getAdTypeValue();
            this.bs = p.getBatchSize();
            this.ins = pIns;
            if (hasHb.get()) {
                this.hb = 1;
            }

            CommonPB.AdType adType = p.getAdType();
            if (adType == CommonPB.AdType.Banner || adType == CommonPB.AdType.Native) {
                if (p.getFanOut())
                    this.fo = 1;
                if (p.getFrequencyCap() > 0 && p.getFrequencyUnit() > 0) {
                    this.fc = p.getFrequencyCap();
                    this.fu = p.getFrequencyUnit();
                }
            }

            if (adType == CommonPB.AdType.RewardVideo || adType == CommonPB.AdType.Interstitial) {
                this.cs = p.getInventoryCount();
                this.rf = p.getInventoryInterval();
                if (p.isMainPlacement())
                    this.main = 1;
            } else if (adType == CommonPB.AdType.Banner) {
                this.rlw = p.getReloadInterval();
            }

            if (p.getScenesCount() > 0) {
                scenes = new ArrayList<>(p.getScenesCount());
                List<PlacementPB.Scene> ps = p.getScenes();
                boolean hasDefault = false;
                for (PlacementPB.Scene s : ps) {
                    Scene scene = new Scene();
                    scene.id = s.getId();
                    scene.n = s.getName();
                    scene.isd = s.getIsDefault();
                    if (scene.isd == 1) {
                        hasDefault = true;
                    } else {
                        scene.isd = null;
                    }
                    scene.fc = s.getFrequencyCap();
                    scene.fu = s.getFrequencyUnit();
                    scenes.add(scene);
                }
                if (!hasDefault) {
                    // When the default scene is not configured, the first one is used as the default scene.
                    scenes.get(0).isd = 1;
                }
            } else {
                Scene scene = new Scene();
                scene.n = "DS";
                scene.isd = 1;
                if (p.getFrequencyCap() > 0 && p.getFrequencyUnit() > 0) {
                    scene.fc = p.getFrequencyCap();
                    scene.fu = p.getFrequencyUnit();
                }
                scenes = Collections.singletonList(scene);
            }
        }

        public Integer getFi() {
            return p.getFrequencyInterval() > 0 ? p.getFrequencyInterval() : null;
        }

        public Integer getPt() {
            return p.getPreloadTimeout();
        }

    }

    public static class MInstance {
        private Instance o;
        private boolean hb;

        MInstance(Instance o, boolean hb) {
            this.o = o;
            this.hb = hb;
        }

        public String getK() {
            return o.getPlacementKey();
        }

        public int getId() {
            return o.getId();
        }

        public int getM() {
            return o.getAdnId();
        }

        public Integer getFc() {
            return o.getFrequencyCap() > 0 ? o.getFrequencyCap() : null;
        }

        public Integer getFu() {
            return o.getFrequencyUnit() > 0 ? o.getFrequencyUnit() : null;
        }

        public Integer getFi() {
            return o.getFrequencyInterval() > 0 ? o.getFrequencyInterval() : null;
        }

        public Integer getHb() {
            return hb ? 1 : null;
        }

        public Integer getHbt() {
            return hb ? 5000 : null;
        }

    }

}
