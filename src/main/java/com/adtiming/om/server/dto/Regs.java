package com.adtiming.om.server.dto;

public class Regs {
    private Integer gdpr;
    private Integer coppa;
    private Integer ccpa;

    public Integer getGdpr() {
        return gdpr;
    }

    public void setGdpr(Integer gdpr) {
        this.gdpr = gdpr;
    }

    public Integer getCoppa() {
        return coppa;
    }

    public void setCoppa(Integer coppa) {
        this.coppa = coppa;
    }

    public Integer getCcpa() {
        return ccpa;
    }

    public void setCcpa(Integer ccpa) {
        this.ccpa = ccpa;
    }

    public boolean hasAny() {
        return (gdpr != null && gdpr == 1)
                || (ccpa != null && ccpa == 1)
                || (coppa != null && coppa == 1);
    }
}