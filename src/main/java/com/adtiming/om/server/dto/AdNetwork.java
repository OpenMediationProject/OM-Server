package com.adtiming.om.server.dto;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public interface AdNetwork {

    int ADN_ADTIMING = 1;
    int ADN_FACEBOOK = 3;
    int ADN_VUNGLE = 5;
    int ADN_MINTEGRAL = 14;
    int ADN_HELIUM = 17;
    int ADN_CROSSPROMOTION = 19;
    int ADN_PUB_NATIVE = 23;
    Set<Integer> C2S_ADN = new HashSet<>(Arrays.asList(ADN_HELIUM, ADN_PUB_NATIVE));
}
