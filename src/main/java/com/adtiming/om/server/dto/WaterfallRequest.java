// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

import com.adtiming.om.server.service.CacheService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WaterfallRequest extends CommonRequest {

    private int pid;       //  PlacementID
    private float iap;     //  IAP, inAppPurchase
    private int imprTimes; //  Number of impressions of this placement within 24 hours
    private int act;       //  Load request trigger type, [1:init,2:interval,3:adclose,4:manual]
    private List<BidResponse> bidResponses;
    private Map<Integer, Float> bid = Collections.emptyMap();

    private String country;

    // not from json
    // set by api controller
    private int adType;
    private int abt; // abTest

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

    public Float getBid(int instanceId) {
        return bid.get(instanceId);
    }

    public Map<Integer, Float> getBid() {
        return bid;
    }

    public void setBid(List<BidResponse> bidResponses) {
        this.bidResponses = bidResponses;
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
    public void processBid(CacheService cs) {
        if (bidResponses == null || bidResponses.isEmpty())
            return;
        this.bid = new HashMap<>(bidResponses.size());
        for (BidResponse b : bidResponses) {
            this.bid.put(b.iid, cs.getUsdMoney(b.cur, b.price));
        }
    }

    public static class BidResponse {
        public int iid;
        public float price;
        public String cur;
    }
}
