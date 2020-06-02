// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

import com.adtiming.om.server.service.CacheService;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WaterfallRequest extends CommonRequest {

    private int pid;       //  PlacementID
    private float iap;     //  IAP, inAppPurchase
    private int imprTimes; //  Number of impressions of this placement within 24 hours
    private int act;       //  Load request trigger type, [1:init,2:interval,3:adclose,4:manual]
    private List<BidPrice> bidc2s;
    private List<S2SBidderToken> bids2s;
    private String country;

    // not from json
    // set by api controller
    private int adType;
    private int abt; // abTest
    private Map<Integer, Float> bidPriceMap = Collections.emptyMap();

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public float getIap() {
        return iap;
    }

    public void setIap(float iap) {
        this.iap = iap;
    }

    public int getImprTimes() {
        return imprTimes;
    }

    public void setImprTimes(int imprTimes) {
        this.imprTimes = imprTimes;
    }

    public int getAct() {
        return act;
    }

    public void setAct(int act) {
        this.act = act;
    }

    public String getCountry() {
        return country == null ? super.getCountry() : country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public int getAbt() {
        return abt;
    }

    public void setAbt(int abt) {
        this.abt = abt;
    }

    @JsonProperty("bid")
    public void setBidc2s(List<BidPrice> bidc2s) {
        this.bidc2s = bidc2s;
    }

    @JsonProperty("bids2s")
    public void setBids2s(List<S2SBidderToken> bids2s) {
        this.bids2s = bids2s;
    }

    public List<S2SBidderToken> getBids2s() {
        return bids2s;
    }

    public Float getBidPrice(int instanceId) {
        return bidPriceMap.get(instanceId);
    }

    public void setBidPrice(int instanceId, float price) {
        if (Collections.EMPTY_MAP == bidPriceMap) {
            bidPriceMap = new HashMap<>();
        }
        bidPriceMap.put(instanceId, price);
    }

    public Map<Integer, Float> getBidPriceMap() {
        return bidPriceMap;
    }

    public void setAdType(int adType) {
        this.adType = adType;
    }

    public int getAdType() {
        return adType;
    }

    /**
     * Handle bid currency unit conversion and put it in the map
     */
    public void processBidPrices(CacheService cs) {
        if (bidc2s == null || bidc2s.isEmpty())
            return;
        this.bidPriceMap = new HashMap<>(bidc2s.size());
        for (BidPrice b : bidc2s) {
            this.bidPriceMap.put(b.iid, cs.getUsdMoney(b.cur, b.price));
        }
    }

    public static class BidPrice {
        public int iid;
        public float price;
        public String cur;
    }

    public static class S2SBidderToken {
        public int iid;
        public String token;

        // not from json
        public int adn;
        public String appId;
        public String pkey;
        public String endpoint;
    }
}
