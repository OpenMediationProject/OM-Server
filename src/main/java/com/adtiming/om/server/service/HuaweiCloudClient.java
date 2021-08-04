// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.service;

import com.adtiming.om.server.dto.NodeConfig;
import com.alibaba.fastjson.JSON;
import com.obs.services.ObsClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * Used to upload files to Huaweicloud
 */
public class HuaweiCloudClient implements CloudClient {

    private static final Logger LOG = LogManager.getLogger();

    private final ObsClient client;
    private final Config cfg;

    private static class Config {
        public String ak, sk, endpoint, bucket;
    }

    public HuaweiCloudClient(NodeConfig nc) {
        cfg = JSON.parseObject(nc.getCloudConfig(), Config.class);
        client = new ObsClient(cfg.ak, cfg.sk, cfg.endpoint);
        LOG.info("init Huaweicloud ObsClient, endpoint: {}", cfg.endpoint);
    }

    public void putObject(String key, File file) {
        try {
            long start = System.currentTimeMillis();
            LOG.info("obs putObject start, endpoint:{}, bucket:{}, {} to {}", cfg.endpoint, cfg.bucket, file, key);
            client.putObject(cfg.bucket, key, file);
            LOG.info("obs putObject finished, cost: {}, endpoint:{}, bucket:{}, {} to {}",
                    System.currentTimeMillis() - start, cfg.endpoint, cfg.bucket, file, key);
        } catch (Exception e) {
            LOG.error("obs putObject error, endpoint:{}, bucket:{}, {} to {}", cfg.endpoint, cfg.bucket, file, key, e);
        }
    }

    public boolean isEnabled() {
        return client != null;
    }

}
