// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.util;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Compressor, gzip
 */
public abstract class Compressor {

    private Compressor() {
    }

    public static byte[] gzip(byte[] data) {
        if (data == null || data.length == 0)
            return null;
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream(data.length);
            GZIPOutputStream gzip = new GZIPOutputStream(buf);
            gzip.write(data);
            gzip.close();
            return buf.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] gzip(String str) {
        if (str == null || str.isEmpty())
            return null;
        return gzip(str.getBytes(UTF_8));
    }

    public static ByteArrayOutputStream gunzip(InputStream in) {
        try {
            if (in == null || in.available() == 0)
                return null;
            int len = in.available();
            if (len < 10) len = 10;
            ByteArrayOutputStream buf = new ByteArrayOutputStream(len * 2);
            GZIPInputStream gzipin;
            if (in instanceof GZIPInputStream)
                gzipin = (GZIPInputStream) in;
            else
                gzipin = new GZIPInputStream(in);
            IOUtils.copy(gzipin, buf);
            gzipin.close();
            return buf;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] gunzip(byte[] data) {
        if (data == null || data.length == 0)
            return data;
        ByteArrayOutputStream buf = gunzip(new ByteArrayInputStream(data));
        return buf == null ? null : buf.toByteArray();
    }

    public static String gunzip2s(byte[] data, Charset charset) {
        if (data == null || data.length == 0)
            return null;
        ByteArrayOutputStream buf = gunzip(new ByteArrayInputStream(data));
        try {
            return buf == null ? null : buf.toString(charset.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String gunzip2s(byte[] data) {
        return gunzip2s(data, UTF_8);
    }

}
