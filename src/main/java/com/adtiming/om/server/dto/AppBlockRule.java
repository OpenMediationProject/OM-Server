// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

import java.util.List;

public interface AppBlockRule {
    int getId();

    String getSdkVersion();

    String getAppVersion();

    Version getOsvMax();

    Version getOsvMin();

    List<String> getMakeDeviceBlacklistList();

    int getMakeDeviceBlacklistCount();

    List<String> getBrandModelBlacklistList();

    int getBrandModelBlacklistCount();
}
