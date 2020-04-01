// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

public abstract class ConnectionType {

    public static final int UNKNOWN = 0;
    public static final int Ethernet = 1;
    public static final int WIFI = 2;
    public static final int CELLULAR_UNKNOWN_GENERATION = 3;
    public static final int CELLULAR_2G = 4;
    public static final int CELLULAR_3G = 5;
    public static final int CELLULAR_4G = 6;
    public static final int CELLULAR_5G = 7;
    public static final int CELLULAR_6G = 8;

    /**
     * Corresponds to the database Connection type, binary [6G,5G,4G,3G,2G,wifi]
     */
    public static int convertTypeToBinary(int type) {
        if (type <= WIFI)
            return 1;// WIFI
        if (type == CELLULAR_UNKNOWN_GENERATION)
            return 2; // Cellular Unknown Generation As 2G
        return 1 << (type - 3);
    }

}
