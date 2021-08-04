// Copyright 2021 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.service;

import com.adtiming.om.server.dto.NodeConfig;
import com.alibaba.fastjson.JSON;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * Used to upload files to AWS s3
 */
public class AwsCloudClient implements CloudClient {

    private static final Logger LOG = LogManager.getLogger();

    private final AmazonS3 client;
    private final Config cfg;

    private static class Config {
        public String ak, sk, region, bucket;
    }

    public AwsCloudClient(NodeConfig nc) {
        cfg = JSON.parseObject(nc.getCloudConfig(), Config.class);
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .withRegion(Regions.fromName(cfg.region));
        if (StringUtils.isNotEmpty(cfg.ak)) {
            builder.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(cfg.ak, cfg.sk)));
        } else {
            builder.withCredentials(new EC2ContainerCredentialsProviderWrapper());
        }
        client = builder.build();
        LOG.info("init amazonS3, region: {}", cfg.region);
    }

    public void putObject(String key, File file) {
        try {
            long start = System.currentTimeMillis();
            LOG.info("s3 putObject start, region:{}, bucket:{}, {} to {}", cfg.region, cfg.bucket, file, key);
            client.putObject(cfg.bucket, key, file);
            LOG.info("s3 putObject finished, cost: {}, region:{}, bucket:{}, {} to {}",
                    System.currentTimeMillis() - start, cfg.region, cfg.bucket, file, key);
        } catch (Exception e) {
            LOG.error("s3 putObject error, region:{}, bucket:{}, {} to {}", cfg.region, cfg.bucket, file, key, e);
        }
    }

    public boolean isEnabled() {
        return client != null;
    }

}
