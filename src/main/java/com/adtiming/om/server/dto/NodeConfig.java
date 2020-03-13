package com.adtiming.om.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NodeConfig {

    @JsonProperty("kafka_status")
    public int kafkaStatus;

    @JsonProperty("kafka_servers")
    public String kafkaServers;

    @JsonProperty("s3_status")
    public int s3Status;

    @JsonProperty("s3_region")
    public String s3Region;

    @JsonProperty("s3_bucket")
    public String s3Bucket;

    @JsonProperty("s3_access_key_id")
    public String s3AccessKeyId;

    @JsonProperty("s3_secret_access_key")
    public String s3SecretAccessKey;

    @Override
    public String toString() {
        return "NodeConfig{" +
                "kafkaStatus=" + kafkaStatus +
                ", kafkaServers='" + kafkaServers + '\'' +
                ", s3Status=" + s3Status +
                ", s3Region='" + s3Region + '\'' +
                ", s3Bucket='" + s3Bucket + '\'' +
                ", s3AccessKeyId='" + s3AccessKeyId + '\'' +
                ", s3SecretAccessKey='" + s3SecretAccessKey + '\'' +
                '}';
    }
}
