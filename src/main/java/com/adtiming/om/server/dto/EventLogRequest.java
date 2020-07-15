// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

import com.adtiming.om.server.service.LogService;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * SdkEvent, Parse from json
 */
public class EventLogRequest extends CommonRequest {

    public static final int INSTANCE_BID_REQUEST = 270; // C2S Bid Request
    public static final int INSTANCE_BID_RESPONSE = 271; // C2S Bid Response
    public static final int INSTANCE_BID_WIN = 273;

    public static final int CALLED_SHOW = 501;
    public static final int CALLED_IS_READY_TRUE = 502;
    public static final int CALLED_IS_READY_FALSE = 503;

    public static final Set<Integer> REQUIRED_EVENT_IDS = Collections.unmodifiableSet(Stream.of(
            INSTANCE_BID_REQUEST,
            INSTANCE_BID_RESPONSE,
            INSTANCE_BID_WIN,
            CALLED_SHOW,
            CALLED_IS_READY_TRUE,
            CALLED_IS_READY_FALSE
    ).collect(Collectors.toSet()));

    public List<Event> events;

    public static class Event {
        public long serverTs;          // server added
        public Integer adType;         // server added
        public int abt;                // server added

        // parsed from json
        public long ts;                 // Client time in milliseconds
        public int eid;                 // EventID
        public String code;             // ErrorCode
        public String msg;              // Event message
        public int pid;                 // Placement ID
        public int mid;                 // AdNetwork ID
        public int iid;                 // Instance ID
        public String adapterv;         // Adapter Version
        public String msdkv;            // AdNetowrk SDK Version
        public int scene;               // SceneID
        public int ot;                  // Orientation, [1: vertical, 2: horizontal]
        public int duration;            // Elapsed time in seconds
        public int priority;            // instance priority
        public int cs;                  // caches Stock size
        public int bid;                 // Whether it is a Bid related request
        public float price;             // BidResponse price or win price
        public String cur;              // BidResponse currency

        public String getCur() {
            return null;
        }
    }

    public void writeToLog(LogService logService) {
        logService.write("om.event", this);
    }


}
