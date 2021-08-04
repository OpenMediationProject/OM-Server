package com.adtiming.om.server.dto;

public class NodeConfig {

    private int id;
    private int dcenter;
    private int kafkaStatus;
    private String kafkaServers;
    private String redisServers;
    private String cloudType;
    private String cloudConfig;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDcenter() {
        return dcenter;
    }

    public void setDcenter(int dcenter) {
        this.dcenter = dcenter;
    }

    public int getKafkaStatus() {
        return kafkaStatus;
    }

    public void setKafkaStatus(int kafkaStatus) {
        this.kafkaStatus = kafkaStatus;
    }

    public String getKafkaServers() {
        return kafkaServers;
    }

    public void setKafkaServers(String kafkaServers) {
        this.kafkaServers = kafkaServers;
    }

    public String getRedisServers() {
        return redisServers;
    }

    public void setRedisServers(String redisServers) {
        this.redisServers = redisServers;
    }

    public String getCloudType() {
        return cloudType;
    }

    public void setCloudType(String cloudType) {
        this.cloudType = cloudType;
    }

    public String getCloudConfig() {
        return cloudConfig;
    }

    public void setCloudConfig(String cloudConfig) {
        this.cloudConfig = cloudConfig;
    }

    @Override
    public String toString() {
        return "NodeConfig{" +
                "id=" + id +
                ", dcenter=" + dcenter +
                ", kafkaStatus=" + kafkaStatus +
                ", kafkaServers='" + kafkaServers + '\'' +
                ", redisServers='" + redisServers + '\'' +
                ", cloudType=" + cloudType +
                ", cloudConfig=" + cloudConfig +
                '}';
    }
}
