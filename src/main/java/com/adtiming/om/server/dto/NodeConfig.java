package com.adtiming.om.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NodeConfig {

    @JsonProperty("id")
    public int id;

    @JsonProperty("dcenter")
    public int dcenter;

    @JsonProperty("kafka_status")
    public int kafkaStatus;

    @JsonProperty("kafka_servers")
    public String kafkaServers;

    @JsonProperty("redis_servers")
    public String redisServers;

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
                "id=" + id +
                ", dcenter=" + dcenter +
                ", kafkaStatus=" + kafkaStatus +
                ", kafkaServers='" + kafkaServers + '\'' +
                ", redisServers='" + redisServers + '\'' +
                ", s3Status=" + s3Status +
                ", s3Region='" + s3Region + '\'' +
                ", s3Bucket='" + s3Bucket + '\'' +
                ", s3AccessKeyId='" + s3AccessKeyId + '\'' +
                ", s3SecretAccessKey='" + s3SecretAccessKey + '\'' +
                '}';
    }
}
