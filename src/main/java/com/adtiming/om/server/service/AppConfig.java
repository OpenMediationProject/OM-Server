// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    public enum Env {
        prod, dev
    }

    private Env env;

    /**
     * om-server data center id
     */
    private int dcenter;

    /**
     * om-server node id, generate by dtask
     */
    private int snode;

    /**
     * dtask server host, required
     * Used to get startup configuration and sync cache PB files
     */
    private String dtask;

    public boolean isDev() {
        return env == Env.dev;
    }

    public boolean isProd() {
        return env == Env.prod;
    }

    public void setEnv(String env) {
        this.env = Env.valueOf(env);
    }

    public int getDcenter() {
        return dcenter;
    }

    public void setDcenter(int dcenter) {
        this.dcenter = dcenter;
    }

    public int getSnode() {
        return snode;
    }

    public void setSnode(int snode) {
        this.snode = snode;
    }

    public String getDtask() {
        return dtask;
    }

    public void setDtask(String dtask) {
        this.dtask = dtask;
    }
}
