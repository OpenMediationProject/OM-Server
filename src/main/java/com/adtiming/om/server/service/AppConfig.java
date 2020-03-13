// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    private static final Logger LOG = LogManager.getLogger();

    public enum Env {
        prod, dev
    }

    private Env env;

    /**
     * om-server data center id
     */
    private int dcenter;

    /**
     * om-server node id
     */
    private int snode;

    /**
     * dtask server host, required
     * Used to get startup configuration and sync cache PB files
     */
    private String dtask;

    @PostConstruct
    private void init() {
        LOG.info("OM-Server init, snode: {}, dc: {}, dtask: {}", snode, dcenter, dtask);
    }

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
