// Copyright 2019 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.test;

import com.adtiming.om.server.util.Compressor;
import com.adtiming.om.server.util.CountryCode;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

public class TestSDKReq {

    @Test
    public void testCountryCode() {
        String[] list = {"CN", "USA", "GBR", "UK", "JP", null, "221", "Hello"};
        for (String code : list) {
            System.out.printf("%s: \t%s\n", code, CountryCode.convertToA2(code));
        }
    }

    private JSONObject buildCommonReqeust() {
        return new JSONObject(30)
                .fluentPut("fit", System.currentTimeMillis() / 1000)
                .fluentPut("flt", System.currentTimeMillis() / 1000)
                .fluentPut("ts", System.currentTimeMillis())
                .fluentPut("zo", 480)
                .fluentPut("session", UUID.randomUUID().toString())
                .fluentPut("uid", UUID.randomUUID().toString())
                .fluentPut("did", "32E205FA-3F7E-499C-A911-59C3B17D6537")
                .fluentPut("dtype", 1)
                .fluentPut("lang", "en-US")
                .fluentPut("langname", "English")
                .fluentPut("jb", 1)
                .fluentPut("bundle", "com.at.stool")
                .fluentPut("make", "samsung")
                .fluentPut("brand", "samsung")
                .fluentPut("model", "iPhone11,6")
                .fluentPut("osv", "9.0")
                .fluentPut("build", "16A366")
                .fluentPut("appv", "1.0")
                .fluentPut("width", 320)
                .fluentPut("height", 50)
                .fluentPut("lip", "192.168.100.103")
                .fluentPut("lcountry", "CN")
                .fluentPut("contype", 6)
                .fluentPut("carrier", "46002中国移动")
                .fluentPut("fm", 17799)
                .fluentPut("battery", 52)
                .fluentPut("btch", 1);
    }

    @Test
    public void testInit() throws Exception {
        String query = new ParamsBuilder(10)
                .p("v", 1)
                .p("plat", 1)
                .p("sdkv", "1.0.1")
                .p("k", "OtnCjcU7ERE0D21GRoquiQBY6YXR3YLl")
                .format();

        JSONObject ios = new JSONObject()
                .fluentPut("idfv", "32E205FA-3F7E-499C-A911-59C3B17D6573")
                .fluentPut("cpu_type", "ARM64_V8")
                .fluentPut("cpu_64bits", 1)
                .fluentPut("cpu_count", 6)
                .fluentPut("ui_width", 375)
                .fluentPut("ui_height", 812)
                .fluentPut("ui_scale", 3.0F)
                .fluentPut("hardware_name", "D21AP")
                .fluentPut("cf_network", "758.1.6")
                .fluentPut("darwin", "15.0.0")
                .fluentPut("ra", "CTRadioAccessTechnologyLTE︎")
                .fluentPut("local_id", "zh_CN")
                .fluentPut("tz_ab", "GMT+8")
                .fluentPut("tdsg", "256")
                .fluentPut("rdsg", "62.18")
                .fluentPut("time_zone", "Asia/Shanghai")
                .fluentPut("country", "中国大陆");

        JSONObject android = new JSONObject()
                .fluentPut("device", "SM-1111");

        JSONObject ps = buildCommonReqeust();
        ps.put("ios", ios);
        ps.put("android", android);
        doReq("init?" + query, ps);
    }

    @Test
    public void testWaterfall() throws Exception {
        JSONObject ps = buildCommonReqeust()
                .fluentPut("country", "US")
                .fluentPut("pid", 119)
                .fluentPut("imprTimes", 2)
                .fluentPut("act", 1)
                .fluentPut("iap", 0);

        JSONArray bid = new JSONArray()
                .fluentAdd(new JSONObject()
                        .fluentPut("iid", 203)
                        .fluentPut("price", 1.2)
                        .fluentPut("cur", "USD"));
        ps.put("bid", bid);
        doReq("hb?v=1&plat=0&sdkv=1.0.1", ps);
    }

    @Test
    public void testIap() throws Exception {
        JSONObject ps = buildCommonReqeust()
                .fluentPut("cur", "CNY")
                .fluentPut("iap", 100.0)
                .fluentPut("iapt", 1000.0);

        String query = new ParamsBuilder(10)
                .p("v", 1)
                .p("plat", 1)
                .p("sdkv", "1.0.1")
                .p("k", "OtnCjcU7ERE0D21GRoquiQBY6YXR3YLl")
                .format();
        doReq("iap?" + query, ps);
    }

    @Test
    public void testIC() throws Exception {
        JSONObject ps = buildCommonReqeust()
                .fluentPut("pid", 100)
                .fluentPut("mid", 1)
                .fluentPut("iid", 100)
                .fluentPut("scene", 0)
                .fluentPut("content", "{userId: 100001}");

        String query = new ParamsBuilder(10)
                .p("v", 1)
                .p("plat", 1)
                .p("sdkv", "1.0.1")
                .format();
        doReq("ic?" + query, ps);
    }

    @Test
    public void testLR() throws Exception {
        JSONObject ps = buildCommonReqeust()
                .fluentPut("type", 3)
                .fluentPut("pid", 100)
                .fluentPut("mid", 1)
                .fluentPut("iid", 100)
                .fluentPut("scene", 0)
                .fluentPut("act", 1);

        String query = new ParamsBuilder(10)
                .p("v", 1)
                .p("plat", 1)
                .p("sdkv", "1.0.1")
                .format();
        doReq("lr?" + query, ps);
    }

    @Test
    public void testEvent() throws Exception {
        JSONObject ps = buildCommonReqeust();

        JSONObject event = new JSONObject()
                .fluentPut("ts", System.currentTimeMillis())
                .fluentPut("eid", 307)
                .fluentPut("msg", "")
                .fluentPut("pid", 100)
                .fluentPut("mid", 1)
                .fluentPut("iid", 100)
                .fluentPut("scene", 0);
        ps.put("events", new JSONArray().fluentAdd(event));

        String query = new ParamsBuilder(10)
                .p("v", 1)
                .p("plat", 1)
                .p("sdkv", "1.0.1")
                .p("k", "OtnCjcU7ERE0D21GRoquiQBY6YXR3YLl")
                .format();
        doReq("log?" + query, ps);
    }

    @Test
    public void testErr() throws Exception {
        JSONObject ps = buildCommonReqeust()
                .fluentPut("pid", 100)
                .fluentPut("mid", 1)
                .fluentPut("iid", 100)
                .fluentPut("scene", 0)
                .fluentPut("tag", "mytag")
                .fluentPut("error", "exception of nullpoint");

        String query = new ParamsBuilder(10)
                .p("v", 1)
                .p("plat", 1)
                .p("sdkv", "1.0.1")
                .p("k", "OtnCjcU7ERE0D21GRoquiQBY6YXR3YLl")
                .format();
        doReq("err?" + query, ps);
    }

    private void doReq(String method, JSONObject ps) throws Exception {
        String url = "http://127.0.0.1:19011/";
        //String url = "https://omtest.adtiming.com/";
        HttpURLConnection con = (HttpURLConnection) new URL(url + method).openConnection();
        con.setDoInput(true);
        con.setRequestProperty("Host", "omtest.adtiming.com");
        con.setRequestProperty("Connection", "close");
        con.setRequestProperty("debug", "1"); // debug msg should return
        if (ps != null) {
            byte[] data = Compressor.gzip(ps.toJSONString());
            con.setDoOutput(true);
            con.setFixedLengthStreamingMode(data.length);
            con.setRequestProperty("Content-Type", "application/json");
            con.getOutputStream().write(data);
            con.getOutputStream().close();
        }

        System.out.println(con.getHeaderField(0));
        for (int i = 1; ; ++i) {
            String k = con.getHeaderFieldKey(i);
            String v = con.getHeaderField(i);
            if (k == null) break;
            System.out.printf("%s: %s\n", k, v);
        }
        System.out.println();
        InputStream in;
        try {
            in = con.getInputStream();
        } catch (IOException e) {
            in = con.getErrorStream();
        }
        if (in != null) {
            if (StringUtils.startsWith(con.getContentEncoding(), "gzip"))
                in = new GZIPInputStream(in);
            IOUtils.copy(in, System.out);
        }
        con.disconnect();
    }
}
