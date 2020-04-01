// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.service;

import com.adtiming.om.server.dto.NodeConfig;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;

/**
 * Used to upload files to AWS s3 every hour
 */
@Service
public class AwsClient {

    private static final Logger LOG = LogManager.getLogger();

    @Resource
    private NodeConfig nc;

    private AmazonS3 s3Client;

    @PostConstruct
    private void init() {
        if (nc.s3Status == 1) {
            AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                    .withRegion(Regions.fromName(nc.s3Region));
            if (StringUtils.isNotEmpty(nc.s3AccessKeyId)) {
                builder.withCredentials(new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials(nc.s3AccessKeyId, nc.s3SecretAccessKey)));
            } else {
                builder.withCredentials(new EC2ContainerCredentialsProviderWrapper());
            }
            s3Client = builder.build();
            LOG.info("init amazonS3, region: {}", nc.s3Region);
        }
    }

    public void putObject(String key, File file) throws SdkClientException {
        if (s3Client == null)
            return;
        try {
            long start = System.currentTimeMillis();
            LOG.info("s3 putObject start, region:{}, bucket:{}, {} to {}", nc.s3Region, nc.s3Bucket, file, key);
            s3Client.putObject(nc.s3Bucket, key, file);
            LOG.info("s3 putObject finished, cost: {}, region:{}, bucket:{}, {} to {}",
                    System.currentTimeMillis() - start, nc.s3Region, nc.s3Bucket, file, key);
        } catch (Exception e) {
            LOG.error("s3 putObject error, region:{}, bucket:{}, {} to {}", nc.s3Region, nc.s3Bucket, file, key, e);
        }
    }

    public boolean isEnabled() {
        return s3Client != null;
    }

}
