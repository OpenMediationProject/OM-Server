// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.cp.dto;

import com.adtiming.om.pb.CrossPromotionPB;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.UUID;

public class BidResponse {

    private BidRequest req;

    public String id;    // platform's request identifier
    public String bidid; // Our identifier for this response
    public String cur;   // bid currency: USD
    public Seatbid[] seatbid = {new Seatbid()};
    public Integer nbr; // No-Bid Reason Code

    public List<CharSequence> debug;

    private BidResponse() {
        this.bidid = UUID.randomUUID().toString().replace("-", "");
        getFirstBid().id = this.bidid;
    }

    public BidResponse(BidRequest req, List<CharSequence> debug) {
        this();
        this.req = req;
        this.id = req.id;
        this.debug = debug;
        getFirstBid().impid = req.imp[0].id;
    }

    public BidResponse(BidRequest req, int nbrCode, List<CharSequence> debug) {
        this(req, debug);
        this.setNbr(nbrCode);
    }

    public BidResponse setAdm(MatchedCampaign mc, String nurl) {
        this.cur = "USD";
        Seatbid.Bid b = getFirstBid();
        BidPayloadToken payload = new BidPayloadToken()
                .setTest(this.req.test == 1 ? 1 : null)
                .setTs(System.currentTimeMillis() / 1000)
                .setBidid(this.bidid)
                .setReqid(this.id)
                .setCid(mc.getCampaignId())
                .setCrid(mc.getCreativeId())
                .setPrice(mc.getFinalBidPrice());
        MatchedCreative mcrt = mc.getCreative();
        if (mcrt != null) {
            if (mcrt.materialIcon != null) {
                payload.setIcon(mcrt.materialIcon.getId());
            }
            if (mcrt.materialImgs != null) {
                for (CrossPromotionPB.CpMaterial img : mcrt.materialImgs) {
                    payload.addImg(img.getId());
                }
            }
            if (mcrt.materialVideo != null) {
                payload.setVd(mcrt.materialVideo.getId());
            }
            if (mcrt.template != null) {
                payload.setVdt(mcrt.template.getId());
            }
            if (mcrt.endcardTemplate != null) {
                payload.setEct(mcrt.endcardTemplate.getId());
            }
        }
        JSONObject adm = new JSONObject(2)
                .fluentPut("type", "ID")
                .fluentPut("payload", payload.toTokenString());
        b.adm = adm.toJSONString();

        if (req.test == 1) {
            b.price = 99.0F;
            if (nurl != null) {
                b.nurl = nurl + "&wprice=0&lcode=0";
                b.lurl = nurl + "&wprice=0&lcode=${AUCTION_LOSS}";
            }
        } else {
            b.price = mc.getFinalBidPrice();
            if (nurl != null) {
                b.nurl = nurl + "&wprice=${AUCTION_PRICE}&lcode=0";
                b.lurl = nurl + "&wprice=${AUCTION_PRICE}&lcode=${AUCTION_LOSS}";
            }
        }

        return this;
    }

    @JsonIgnore
    private Seatbid.Bid getFirstBid() {
        return this.seatbid[0].bid[0];
    }

    public static class Seatbid {
        public Bid[] bid = {new Bid()};

        public static class Bid {
            public String id; // Our identifier for this bid
            public String impid; // platform's identifier for this slot auction
            public float price; // Our bid price, in USD cents, CPM basis
            public String adm;  // Our creative - see Rendering The Ad
            public String nurl; // URL to get if we win the impression
            public String lurl; // URL to get if we lose the impression
            public String burl; // URL to get if the impression gets logged
        }
    }

    public BidResponse setNbr(int nbrCode) {
        this.nbr = nbrCode;
        this.seatbid = null;
        this.cur = null;
        return this;
    }

}
