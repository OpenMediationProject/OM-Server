package com.adtiming.om.server.cp.util;

import com.adtiming.om.pb.CrossPromotionPB;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Util {

    public static String buildMaterialUrl(String cdnOrigin, CrossPromotionPB.CpMaterial m) {
        if (m == null)
            return "";
        return buildMaterialUrl(cdnOrigin, m.getUrl());
    }

    public static String buildMaterialUrl(String cdnOrigin, String url) {
        if (StringUtils.isBlank(url))
            return "";
        if (url.startsWith("http")) {
            return url;
        } else {
            return cdnOrigin + url;
        }
    }

    private static final byte[] SIGN_SECRET = {-70, 92, 99, -50, -24, 86, 58, 93, -46, 113, -106, 105, -90, 119, -111, -48};
    private static final byte[] EMPTY_BYTES = new byte[0];

    public static String buildSign(String reqid, long cid, String price, long time) {
        byte[] reqidBytes = reqid.getBytes(UTF_8);
        byte[] priceBytes = StringUtils.isBlank(price) ? EMPTY_BYTES : price.getBytes(UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(reqidBytes.length + priceBytes.length + 16 + SIGN_SECRET.length);
        buf.put(reqidBytes).putLong(cid).put(priceBytes).putLong(time).put(SIGN_SECRET);
        return DigestUtils.md5Hex(buf.array());
    }
}
