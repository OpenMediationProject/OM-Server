// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.util;

import com.adtiming.om.pb.CommonPB;
import com.adtiming.om.server.dto.AppBlockRule;
import com.adtiming.om.server.dto.Version;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class Util {

    private Util() {
    }

    public static final String COUNTRY_ALL = "00";

    public static boolean isAllowOsv(String osv, Version osvMax, Version osvMin) {
        if (StringUtils.isBlank(osv) && (osvMax != null || osvMin != null))
            return false;
        else if (StringUtils.isBlank(osv))
            return true;
        return Version.of(osv).in(osvMin, osvMax);
    }

    public static boolean isAllowPeriod(Map<Integer, Integer> cp) {
        LocalDateTime now = LocalDateTime.now();
        int week = now.getDayOfWeek().getValue() % 7;
        int allowHours = cp.getOrDefault(week, 0);
        if (allowHours == 0)
            return false;
        int h = 1 << now.getHour();
        return (allowHours & h) == h;
    }

    private static String toLower(String str) {
        if (StringUtils.isBlank(str)) {
            return "";
        }
        return str.toLowerCase();
    }

    private static boolean isMatchedRule(List<String> list, String first, String second) {
        if (list != null && !list.isEmpty()) {
            Set<String> brandModelSet = new HashSet<>(list);
            List<String> brandModelList = new ArrayList<>();
            brandModelList.add(Util.toLower(first) + ":" + Util.toLower(second));
            brandModelList.add("*:" + Util.toLower(second));
            brandModelList.add(Util.toLower(first) + ":*");
            brandModelList.add("*:*");
            brandModelSet.retainAll(brandModelList);
            return !brandModelSet.isEmpty();
        }
        return false;
    }

    public static boolean matchBlockRule(List<AppBlockRule> blockRule, String sdkv, String appv,
                                         String osv, String make, String device, String brand, String model) {
        for (AppBlockRule rule : blockRule) {
            int matchCount = 0;
            //sdk_version匹配
            if (StringUtils.isBlank(rule.getSdkVersion()) || StringUtils.equals(rule.getSdkVersion(), sdkv)) {
                matchCount++;
            }
            //app_version匹配
            if (StringUtils.isBlank(rule.getAppVersion()) || StringUtils.equals(rule.getAppVersion(), appv)) {
                matchCount++;
            }
            //os_version匹配
            if (Util.isAllowOsv(osv, rule.getOsvMax(), rule.getOsvMin())) {
                matchCount++;
            }
            if (matchCount == 3) {
                boolean matched = isMatchedRule(rule.getMakeDeviceBlacklistList(), make, device)
                        || isMatchedRule(rule.getBrandModelBlacklistList(), brand, model);
                if (matched) return true;
            }
        }

        return false;
    }

    public static <T> List<T> sortByPriority(Map<T, Integer> tWeight) {
        if (CollectionUtils.isEmpty(tWeight)) {
            return Collections.emptyList();
        } else if (tWeight.size() == 1) {
            return Collections.singletonList(tWeight.keySet().iterator().next());
        } else {
            List<Map.Entry<T, Integer>> list = new LinkedList<>(tWeight.entrySet());
            list.sort(Map.Entry.comparingByValue());
            return list.stream().filter(o -> o.getValue() > 0).map(Map.Entry::getKey).collect(Collectors.toList());
        }
    }

    public static <T> List<T> sortByPrice(Map<T, Float> tWeight) {
        if (CollectionUtils.isEmpty(tWeight)) {
            return Collections.emptyList();
        } else if (tWeight.size() == 1) {
            return Collections.singletonList(tWeight.keySet().iterator().next());
        } else {
            List<Map.Entry<T, Float>> list = new LinkedList<>(tWeight.entrySet());
            list.sort((o1, o2) -> (o2.getValue()).compareTo(o1.getValue()));
            return list.stream().filter(o -> o.getValue() > 0).map(Map.Entry::getKey).collect(Collectors.toList());
        }
    }

    /**
     * sort by ecpm, shuffle same ecpm
     */
    public static <T> List<T> sortByEcpm(Map<Float, List<T>> tEcpm) {
        List<T> sortIns = new ArrayList<>(tEcpm.size());
        List<Float> ecpmList = new ArrayList<>(tEcpm.keySet());
        ecpmList.sort(Comparator.reverseOrder());
        for (float ecpm : ecpmList) {
            // Same Ecpm processing
            List<T> ins = tEcpm.get(ecpm);
            if (ins == null) continue;
            if (ins.size() > 1) {
                Collections.shuffle(ins);
            }
            sortIns.addAll(ins);
        }
        return sortIns;
    }

    private static final Pattern REG_IOS_MODEL_TYPE = Pattern.compile("^([a-zA-Z]+)", Pattern.CASE_INSENSITIVE);

    /**
     * 解析 modleType
     *
     * @param plat  平台, 0:iOS,1:Android
     * @param model model for iOS
     * @param ua    ua for Android
     * @return modelType
     */
    public static int getModelType(int plat, String model, String ua) {
        final int phone = 0b001;// default Phone
        if (plat == CommonPB.Plat.iOS_VALUE) {
            if (StringUtils.isNotBlank(model)) {
                Matcher m = REG_IOS_MODEL_TYPE.matcher(model);
                if (m.find()) {
                    String mtype = m.group(1).toLowerCase();
                    switch (mtype) {
                        case "iphone":
                            return 0b001;
                        case "ipad":
                            return 0b010;
                        case "appletv":
                            return 0b100;
                    }
                }
            }
        } else {
            if (!StringUtils.containsIgnoreCase(ua, "Mobile")) {
                return 0b010; // Pad
            }
        }
        return phone;
    }

}
