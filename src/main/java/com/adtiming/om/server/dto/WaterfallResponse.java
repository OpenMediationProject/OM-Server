// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WaterfallResponse implements WfResInterface {
    private int code;
    private String msg;
    private List<?> ins;
    public List<S2SBidResponse> bidresp;
    private int abt;
    private List<CharSequence> debug;

    public WaterfallResponse(int abt, boolean debug) {
        this.abt = abt;
        if (debug) {
            setDebug(new ArrayList<>());
        }
    }

    @Override
    public void initBidResponse(List<S2SBidResponse> bidresp) {
        this.bidresp = bidresp;
    }

    @Override
    public void addS2sBidResponse(S2SBidResponse res) {
        if (bidresp == null) {
            bidresp = new ArrayList<>();
        }
        bidresp.add(res);
    }

    public int getCode() {
        return code;
    }

    public WfResInterface setCode(int code) {
        this.code = code;
        return this;
    }

    public String getMsg() {
        return msg;
    }

    public WfResInterface setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public List<?> getIns() {
        return ins;
    }

    @Override
    public WfResInterface setIns(List<?> ins) {
        this.ins = ins;
        return this;
    }

    @Override
    public List<CharSequence> getDebug() {
        return debug;
    }

    @Override
    public WfResInterface setDebug(List<CharSequence> debug) {
        this.debug = debug;
        return this;
    }

    public int getAbt() {
        return abt;
    }

    public WfResInterface setAbt(int abt) {
        this.abt = abt;
        return this;
    }

    @JsonIgnore
    public boolean isDebugEnabled() {
        return debug != null;
    }

    public WfResInterface addDebug(String str) {
        if (isDebugEnabled()) {
            debug.add(LocalDateTime.now().format(FMT) + " - " + str);
        }
        return this;
    }

    public WfResInterface addDebug(String msg, Object... args) {
        if (isDebugEnabled()) {
            addDebug(LocalDateTime.now().format(FMT) + " - " + String.format(msg, args));
        }
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
