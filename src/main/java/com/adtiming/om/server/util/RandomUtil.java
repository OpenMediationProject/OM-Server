// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.util;

import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Randomly extract elements by weight
 */
public class RandomUtil {

    /**
     * Randomly extract an element based on the specified sample array and sample-weight mapping
     *
     * @param tWeight Sample-weight mapping
     */
    public static <T> T randomByWeight(Map<T, Integer> tWeight) {
        if (CollectionUtils.isEmpty(tWeight))
            throw new RuntimeException("WeightMap must not be empty!");

        if (tWeight.size() == 1)
            return tWeight.keySet().iterator().next();

        int totalWeight = 0;
        Set<Map.Entry<T, Integer>> tws = tWeight.entrySet();
        for (Map.Entry<T, Integer> tw : tws)
            totalWeight += tw.getValue();

        if (totalWeight <= 0)
            return null;

        int ruler = ThreadLocalRandom.current().nextInt(totalWeight);
        int g = 0;
        for (Map.Entry<T, Integer> tw : tws) {
            g += tw.getValue();
            if (ruler < g)
                return tw.getKey();
        }
        return null;
    }

    /**
     * Sort elements based on a specified sample array and sample-weight mapping
     *
     * @param tWeight Sample-weight mapping
     */
    public static <T> List<T> sortByWeight(Map<T, Integer> tWeight) {
        if (CollectionUtils.isEmpty(tWeight))
            return Collections.emptyList();

        if (tWeight.size() == 1) {
            return Collections.singletonList(tWeight.keySet().iterator().next());
        }

        int totalWeight = 0;
        Set<Map.Entry<T, Integer>> tws = tWeight.entrySet();
        for (Map.Entry<T, Integer> tw : tws)
            totalWeight += tw.getValue();

        if (totalWeight <= 0)
            return Collections.emptyList();

        List<Map.Entry<T, Integer>> twList = new ArrayList<>(tws);
        List<T> list = new ArrayList<>(tWeight.size());
        for (int i = 0, j = tWeight.size(); i < j; ++i) {
            if (totalWeight == 0)
                break;
            int ruler = ThreadLocalRandom.current().nextInt(totalWeight);
            int g = 0;
            for (int m = 0, n = twList.size(); m < n; ++m) {
                Map.Entry<T, Integer> tw = twList.get(m);
                g += tw.getValue();
                if (ruler < g) {
                    list.add(tw.getKey());
                    totalWeight -= tw.getValue();
                    twList.remove(m);
                    break;
                }
            }
        }
        return list;
    }

}
