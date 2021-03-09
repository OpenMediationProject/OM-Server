package com.adtiming.om.server.dto;

import java.time.format.DateTimeFormatter;
import java.util.List;

public interface WfResInterface {
    DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    int CODE_COUNTRY_NOT_FOUND = 10;     // country not found
    int CODE_PUB_APP_INVALID = 20;       // pub app invalid
    int CODE_PLACEMENT_INVALID = 30;     // placement invalid
    int CODE_INSTANCE_EMPTY = 40;        // instance empty
    int CODE_NOAVAILABLE_INSTANCE = 50;  // no available instance

    void initBidResponse(List<WaterfallResponse.S2SBidResponse> bidresp);

    void addS2sBidResponse(WaterfallResponse.S2SBidResponse res);

    int getCode();

    WfResInterface setCode(int code);

    String getMsg();

    WfResInterface setMsg(String msg);

    List<?> getIns();

    WfResInterface setIns(List<?> ins);

    List<CharSequence> getDebug();

    WfResInterface setDebug(List<CharSequence> debug);

    int getAbt();

    WfResInterface setAbt(int abt);

    boolean isDebugEnabled();

    WfResInterface addDebug(String str);

    WfResInterface addDebug(String str, Object... args);
}
