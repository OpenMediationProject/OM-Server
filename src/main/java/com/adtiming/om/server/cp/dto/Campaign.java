// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.cp.dto;

import com.adtiming.om.pb.CrossPromotionPB;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Campaign {

    private final CrossPromotionPB.CpCampaign campaign;
    private Map<String, Float> countryBidPrice = Collections.emptyMap();

    public Campaign(CrossPromotionPB.CpCampaign cpCampaign) {
        this.campaign = cpCampaign;
        if (cpCampaign.getCountryBidpriceCount() > 0) {
            countryBidPrice = cpCampaign.getCountryBidpriceMap();
        }
    }

    public long getId() {
        return campaign.getId();
    }

    public int getSkaCampaignId() {
        return campaign.getSkaCampaignId();
    }

    public int getPublisherId() {
        return campaign.getPublisherId();
    }

    public int getType() {
        return campaign.getType();
    }

    public String getName() {
        return campaign.getName();
    }

    public String getAppId() {
        return campaign.getAppId();
    }

    public String getAppName() {
        return campaign.getAppName();
    }

    public String getPreviewUrl() {
        return campaign.getPreviewUrl();
    }

    public int getPlatform() {
        return campaign.getPlatform();
    }

    public int getBillingType() {
        return campaign.getBillingType();
    }

    public float getPrice() {
        return campaign.getPrice();
    }

    public int getDailyCap() {
        return campaign.getDailyCap();
    }

    public float getDailyBudget() {
        return campaign.getDailyBudget();
    }

    public float getMaxBidprice() {
        return campaign.getMaxBidprice();
    }

    public Map<String, Float> getCountryBidPriceMap() {
        return campaign.getCountryBidpriceMap();
    }

    public Float getCountryBidPrice(String country) {
        Float bid = countryBidPrice.get(country);
        if (bid == null) {
            bid = campaign.getBidprice();
        }
        return bid;
    }

    public int getImprCap() {
        return campaign.getImprCap();
    }

    public int getImprFreq() {
        return campaign.getImprFreq();
    }

    public String getAdDomain() {
        return campaign.getAdDomain();
    }

    public String getClickUrl() {
        return campaign.getClickUrl();
    }

    public List<String> getImprTkUrlsList() {
        return campaign.getImprTkUrlsList();
    }

    public List<String> getClickTkUrlsList() {
        return campaign.getClickTkUrlsList();
    }

    public int getOpenType() {
        return campaign.getOpenType();
    }
}
