// Copyright 2019 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.cp.util;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wuwei on 16/05/2017.
 * ParamsBuilder
 */
public class ParamsBuilder implements Cloneable {

    private List<NameValuePair> ps;

    public ParamsBuilder() {
        this.ps = new ArrayList<>();
    }

    public ParamsBuilder(int initialCapacity) {
        this.ps = new ArrayList<>(initialCapacity);
    }

    public ParamsBuilder(List<NameValuePair> ps) {
        this.ps = ps;
    }

    public ParamsBuilder p(String name, Object value) {
        ps.add(new BasicNameValuePair(name, value == null ? "" : value.toString()));
        return this;
    }

    public String format(Charset charset) {
        return URLEncodedUtils.format(ps, charset);
    }

    public String format() {
        return URLEncodedUtils.format(ps, StandardCharsets.UTF_8);
    }

    public UrlEncodedFormEntity toEntity(Charset charset) {
        return new UrlEncodedFormEntity(ps, charset);
    }

    public UrlEncodedFormEntity toEntity() {
        return new UrlEncodedFormEntity(ps, StandardCharsets.UTF_8);
    }

    public List<NameValuePair> params() {
        return ps;
    }

    public void clear() {
        if (ps != null) ps.clear();
    }

    @Override
    public ParamsBuilder clone() {
        return new ParamsBuilder(ps == null ? null : new ArrayList<>(ps));
    }
}
