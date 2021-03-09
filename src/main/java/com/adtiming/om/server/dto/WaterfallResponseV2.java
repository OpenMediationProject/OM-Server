package com.adtiming.om.server.dto;

import com.adtiming.om.server.util.Util;
import com.adtiming.om.server.web.WaterfallBase;
import org.apache.commons.collections4.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WaterfallResponseV2 implements WfResInterface {
    private int code;
    private String msg;
    private MediationRule rule;
    private List<ResInstance> ins;
    public List<WaterfallResponse.S2SBidResponse> bidresp;
    private List<CharSequence> debug;
    private int abt;

    private InstanceRule hitRule;

    public WaterfallResponseV2(int code, String msg, int abt, boolean debug) {
        this.code = code;
        this.msg = msg;
        this.abt = abt;
        if (debug) {
            setDebug(new ArrayList<>());
        }
    }

    public void setHitRule(InstanceRule hitRule) {
        rule = new MediationRule();
        this.hitRule = hitRule;
        if (hitRule != null) {
            rule.id = hitRule.getId();
            rule.n = hitRule.getName();
            rule.t = hitRule.isAutoOpt() ? 0 : 1; //0:Auto,1:Manual
            rule.i = hitRule.getPriority();
        } else {
            rule.id = 0;
            rule.n = "DefaultAuto";
            rule.t = 0;
            rule.i = 0;
        }
    }

    @Override
    public void initBidResponse(List<WaterfallResponse.S2SBidResponse> bidresp) {
        this.bidresp = bidresp;
    }

    @Override
    public void addS2sBidResponse(WaterfallResponse.S2SBidResponse res) {
        if (bidresp == null) {
            bidresp = new ArrayList<>();
        }
        bidresp.add(res);
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public WfResInterface setCode(int code) {
        this.code = code;
        return this;
    }

    @Override
    public String getMsg() {
        return msg;
    }

    @Override
    public WfResInterface setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public List<ResInstance> getIns() {
        return ins;
    }

    public MediationRule getRule() {
        return rule;
    }

    @Override
    public WfResInterface setIns(List<?> ins) {
        if (CollectionUtils.isNotEmpty(ins)) {
            this.ins = new ArrayList<>();
            for (Object obj : ins) {
                WaterfallBase.WaterfallInstance wIns = (WaterfallBase.WaterfallInstance)obj;
                ResInstance resInstance = new ResInstance();
                resInstance.id = wIns.instance.getId();
                if (hitRule != null && !hitRule.isAutoOpt()) {
                    if (wIns.instance.isHeadBidding()) {
                        resInstance.i = 0;
                    } else {
                        resInstance.i = hitRule.getInstancePriority(wIns.instance.getId());
                    }
                } else {
                    resInstance.i = 0;
                }
                resInstance.r = wIns.ecpm;
                resInstance.rp = Util.getRevenuePrecision(wIns.instance);
                this.ins.add(resInstance);
            }
        }
        return this;
    }

    public List<CharSequence> getDebug() {
        return debug;
    }

    public WfResInterface setDebug(List<CharSequence> debug) {
        this.debug = debug;
        return this;
    }

    public WaterfallResponseV2 addDebug(String msg, Object... args) {
        if (isDebugEnabled()) {
            addDebug(LocalDateTime.now().format(FMT) + " - " + String.format(msg, args));
        }
        return this;
    }

    public int getAbt() {
        return abt;
    }

    public WfResInterface setAbt(int abt) {
        this.abt = abt;
        return this;
    }

    public boolean isDebugEnabled() {
        return debug != null;
    }

    public WfResInterface addDebug(String str) {
        if (isDebugEnabled()) {
            debug.add(LocalDateTime.now().format(FMT) + " - " + str);
        }
        return this;
    }

    public static class MediationRule {
        public int id;   //Rule ID
        public String n; //Rule Name
        public int t;    //Rule Type, 0:Auto,1:Manual
        public int i;    //Rule Priority, Only available when type is Manual
    }

    public static class ResInstance {
        public int id;   //Instance ID
        public int i;    //Priority In Rule, 0 for Auto or Bid
        public float r;  //Revenue, bidPrice or ecpm
        public int rp;   //Revenue Precision, 0:undisclosed,1:exact,2:estimated,3:defined
    }

}
