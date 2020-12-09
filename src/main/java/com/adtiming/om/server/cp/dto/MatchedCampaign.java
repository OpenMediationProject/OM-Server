// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.cp.dto;

import com.adtiming.om.pb.CrossPromotionPB.App;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MatchedCampaign {

    public static final Logger log = LogManager.getLogger();

    private final Campaign campaign;
    private final MatchedCreative creative;
    private final App app;

    // for bid
    private final float bidPrice;
    private float finalBidPrice;

    public MatchedCampaign(Campaign campaign, App app, MatchedCreative creative, float bidPrice) {
        this.app = app;
        this.campaign = campaign;
        this.creative = creative;
        this.bidPrice = bidPrice;
    }

    public long getCampaignId() {
        return campaign.getId();
    }

    public long getCreativeId() {
        return creative == null || creative.creative == null ? 0 : creative.creative.getId();
    }

    public App getApp() {
        return app;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public MatchedCreative getCreative() {
        return creative;
    }

    public float getBidPrice() {
        return bidPrice;
    }

    public float getFinalBidPrice() {
        return finalBidPrice;
    }

    public MatchedCampaign setFinalBidPrice(float finalBidPrice) {
        this.finalBidPrice = finalBidPrice;
        return this;
    }


}
