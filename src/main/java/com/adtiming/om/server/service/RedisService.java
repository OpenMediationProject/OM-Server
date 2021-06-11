package com.adtiming.om.server.service;

import com.adtiming.om.server.dto.NodeConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class RedisService {

    private JedisCluster jedis;

    @Resource
    private NodeConfig nc;

    private static final Logger LOG = LogManager.getLogger();

    @PostConstruct
    private void init() {
        if (StringUtils.isNoneBlank(nc.redisServers)) {
            GenericObjectPoolConfig<?> cfg = new GenericObjectPoolConfig<>();
            cfg.setMaxWaitMillis(-1);
            cfg.setMaxTotal(1000);
            cfg.setMinIdle(3);
            cfg.setMaxIdle(20);
            String[] rs = nc.redisServers.split(",");
            jedis = new JedisCluster(Stream.of(rs).map(HostAndPort::parseString).collect(Collectors.toSet()), cfg);
            LOG.info("init jedis cluster, servers: {}", nc.redisServers);
        }
    }


    public List<String> hmget(String key, String... fields) {
        if (jedis != null) {
            return jedis.hmget(key, fields);
        }
        return Collections.emptyList();
    }

    public String get(String key) {
        if (jedis != null) {
            return jedis.get(key);
        }
        return null;
    }

    public Long hincrBy(String key, String field, long value) {
        if (jedis != null) {
            return jedis.hincrBy(key, field, value);
        }
        return 0L;
    }

    public Long decrBy(String key, long value) {
        if (jedis != null) {
            return jedis.decrBy(key, value);
        }
        return 0L;
    }

    public Long expire(String key, int seconds) {
        if (jedis != null) {
            return jedis.expire(key, seconds);
        }
        return 0L;
    }
}
