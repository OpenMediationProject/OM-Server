// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.cp.dto;

import com.adtiming.om.pb.CrossPromotionPB.CpCreative;
import com.adtiming.om.pb.CrossPromotionPB.CpMaterial;
import com.adtiming.om.pb.CrossPromotionPB.H5Template;

import java.util.ArrayList;
import java.util.List;

public class MatchedCreative {

    public CpCreative creative;
    public CpMaterial materialIcon, materialVideo;
    public List<CpMaterial> materialImgs;
    public H5Template template, endcardTemplate;

    public void addImg(CpMaterial img) {
        if (materialImgs == null) {
            materialImgs = new ArrayList<>(5);
        }
        materialImgs.add(img);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MatchedCreative that = (MatchedCreative) o;

        return creative.getId() == that.creative.getId();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode((int) creative.getId());
    }
}
