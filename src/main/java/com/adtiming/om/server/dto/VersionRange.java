package com.adtiming.om.server.dto;

import com.adtiming.om.pb.CrossPromotionPB;
import org.apache.commons.lang3.StringUtils;

public class VersionRange {
    private Version min;
    private Version max;
    private boolean minType;//true: open, false: close
    private boolean maxType;//true: open, false: close
    public VersionRange(CrossPromotionPB.Range range) {
        if (range != null) {
            if (StringUtils.isNotBlank(range.getMax())) {
                max = Version.of(range.getMax());
            }
            if (StringUtils.isNotBlank(range.getMin())) {
                min = Version.of(range.getMin());
            }
            minType = range.getMinType();
            maxType = range.getMaxType();
        }
    }

    public boolean isInRange(String version) {
        if (StringUtils.isBlank(version)) {
            return min == null && max == null;
        }
        Version ver = Version.of(version);
        boolean matchMin = min == null || (minType ? ver.gt(min) : ver.ge(min));
        boolean matchMax = max == null || (maxType ? ver.lt(max) : ver.le(max));
        return matchMin && matchMax;
    }
}
