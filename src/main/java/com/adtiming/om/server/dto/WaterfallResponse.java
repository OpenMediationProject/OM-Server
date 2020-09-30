// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

import java.util.List;

public class WaterfallResponse {

    public static final int CODE_COUNTRY_NOT_FOUND = 10;      //country not found
    public static final int CODE_PUB_APP_INVALID = 20;       //pub app invalid
    public static final int CODE_PLACEMENT_INVALID = 30;     //placement invalid
    public static final int CODE_INSTANCE_EMPTY = 40;        //instance empty

    private int code;
    private String msg;
    private List<Integer> ins;
    public List<S2SBidResponse> bidresp;
    private List<CharSequence> debug;
    private int abt;

    public WaterfallResponse(int code, String msg, int abt, List<CharSequence> debug) {
        this.code = code;
        this.msg = msg;
        this.abt = abt;
        this.debug = debug;
    }

    public WaterfallResponse() {
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public List<Integer> getIns() {
        return ins;
    }

    public void setIns(List<Integer> ins) {
        this.ins = ins;
    }

    public List<CharSequence> getDebug() {
        return debug;
    }

    public void setDebug(List<CharSequence> debug) {
        this.debug = debug;
    }

    public int getAbt() {
        return abt;
    }

    public void setAbt(int abt) {
        this.abt = abt;
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
