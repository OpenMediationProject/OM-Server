// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

import java.util.ArrayList;
import java.util.List;

public class WaterfallResponse extends Debugable<WaterfallResponse> {

    public static final int CODE_COUNTRY_NOT_FOUND = 10;     // country not found
    public static final int CODE_PUB_APP_INVALID = 20;       // pub app invalid
    public static final int CODE_PLACEMENT_INVALID = 30;     // placement invalid
    public static final int CODE_INSTANCE_EMPTY = 40;        // instance empty
    public static final int CODE_NOAVAILABLE_INSTANCE = 50;  // no available instance

    private int code;
    private String msg;
    private List<Integer> ins;
    public List<S2SBidResponse> bidresp;
    private int abt;

    public WaterfallResponse(int abt, boolean debug) {
        this.abt = abt;
        if (debug) {
            setDebug(new ArrayList<>());
        }
    }

    public int getCode() {
        return code;
    }

    public WaterfallResponse setCode(int code) {
        this.code = code;
        return this;
    }

    public String getMsg() {
        return msg;
    }

    public WaterfallResponse setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public List<Integer> getIns() {
        return ins;
    }

    public WaterfallResponse setIns(List<Integer> ins) {
        this.ins = ins;
        return this;
    }

    public int getAbt() {
        return abt;
    }

    public WaterfallResponse setAbt(int abt) {
        this.abt = abt;
        return this;
    }

    public static class S2SBidResponse {
        public int iid;
        public Integer nbr;
        public String err;
        public Float price;
        public String adm;
        public String nurl;
        public String lurl;
        public int expire;
    }
}
