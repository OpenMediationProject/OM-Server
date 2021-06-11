// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.service;

import com.adtiming.om.server.dto.NodeConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Producer
 */
@Service
public class KafkaService {

    private static final Logger LOG = LogManager.getLogger();

    @Resource
    private NodeConfig nc;

    private KafkaProducer<String, String> producer;

    @PostConstruct
    private void init() {
        if (nc.kafkaStatus == 1) {
            Map<String, Object> props = new HashMap<>();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, nc.kafkaServers);
            props.put(ProducerConfig.ACKS_CONFIG, "all");
            props.put(ProducerConfig.RETRIES_CONFIG, 3);
            props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
            props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
            props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
            props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 60 * 1000);
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
            producer = new KafkaProducer<>(props);

            LOG.info("init kafka producer, servers: {}", nc.kafkaServers);
        }
    }

    public void send(ProducerRecord<String, String> r) {
        if (isEnabled()) {
            producer.send(r);
        }
    }

    public boolean isEnabled() {
        return producer != null;
    }

}
