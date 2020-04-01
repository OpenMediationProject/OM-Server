// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

import com.adtiming.om.server.service.LogService;

import java.util.List;

/**
 * SdkEvent, Parse from json
 */
public class EventLogRequest extends CommonRequest {

    public List<Event> events;

    public static class Event {
        public long serverTs;          // server added
        public Integer adType;         // server added
        public int abt;                // server added

        // parsed from json
        public long ts;                 // Client time in milliseconds
        public int eid;                 // EventID<
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
        public float price;             // BidResponse price
        public String cur;              // BidResponse currency

        public String getCur() {
            return null;
        }
    }

    public void writeToLog(LogService logService) {
        logService.write("om.event", this);
    }


}
