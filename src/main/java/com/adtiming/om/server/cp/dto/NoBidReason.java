package com.adtiming.om.server.cp.dto;

public interface NoBidReason {

    int UNKNOWN_ERROR = 0;
    int TECHNICAL_ERROR = 1;
    int INVALID_REQUEST = 2;
    int KNOWN_WEB_SPIDER = 3;
    int SUSPECTED_NONHUMAN_TRAFFIC = 4;
    int CLOUD_DATACENTER_PROXYIP = 5;
    int UNSUPPORTED_DEVICE = 6;
    int BLOCKED_PUBLISHER = 7;
    int UNMATCHED_USER = 8;
    int DAILY_READER_CAP = 9;
    int DAILY_DOMAIN_CAP = 10;
    int INVALID_PLACEMENT = 11;

}
