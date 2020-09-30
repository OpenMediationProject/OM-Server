// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.service;

import com.adtiming.om.server.dto.NodeConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * server data log service
 * Write log to file via log4j
 * If kafka is enabled, send logs to kafka at the same time
 * If AWS s3 is enabled, upload files to s3 every hour
 *
 * @see KafkaService
 * @see AwsClient
 */
@Service
public class LogService {

    private static final Logger LOG = LogManager.getLogger();
    private static final String[] LOG_NAMES = {"om.lr", "om.event", "om.iap", "om.ic", "om.error"};
    private static final Map<String, Logger> LOG_MAP = new HashMap<>();

    static {
        for (String name : LOG_NAMES) {
            LOG_MAP.put(name, LogManager.getLogger(name));
        }
    }

    @Resource
    private KafkaService kafkaService;

    @Resource
    private AwsClient awsClient;

    @Resource
    private NodeConfig nodeConfig;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * compress access log after rolling file
     */
    @Scheduled(cron = "0 1 * * * ?")
    private void gzipAccessLogHourly() {
        LOG.info("gzip access log start");
        try {
            String[] cmd = {"bash", "-c", "cd log; ls access.*.log|xargs gzip"};
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            IOUtils.copy(p.getInputStream(), System.out);
            LOG.info("gzip access log finished {}", p.waitFor());
        } catch (Exception e) {
            LOG.error("gzip access log error", e);
        }
    }

    /**
     * enforce log4j2 to rolling file
     */
    @Scheduled(cron = "1 0 * * * ?")
    private void enforceRollingLogFile() {
        LOG.info("cutLogTail");
        LOG_MAP.values().forEach(l -> l.info("{\"__ignore\":\"cutLogTail\"}"));
    }

    /**
     * put log to aws s3 bucket hourly
     */
    @Scheduled(cron = "0 5 * * * ?")
    public void awsS3Push() {
        if (!awsClient.isEnabled())
            return;
        LOG.info("awsS3Push start");
        final long allStart = System.currentTimeMillis();

        final LocalDateTime lastHour = LocalDateTime.now().plusHours(-1);
        final String keyTimePath = DateTimeFormatter.ofPattern("yyyy/MM/dd/HH").format(lastHour);
        final String fileNameTimePart = DateTimeFormatter.ofPattern("yyyy-MM-dd.HH").format(lastHour);

        final File dir = new File("data");
        final String[] fileNames = dir.list();
        if (fileNames == null) {
            LOG.debug("data empty: {}", dir);
            return;
        }
        for (String name : LOG_NAMES) {
            final String fileNamePrefix = name + '.' + fileNameTimePart;
            for (String fileName : fileNames) {
                if (!(fileName.startsWith(fileNamePrefix) && fileName.endsWith(".log.gz"))) {
                    continue;
                }
                File file = new File(dir, fileName);
                if (!file.exists()) {
                    LOG.warn("file not found: {}", file);
                    continue;
                }
                if (file.length() < 22) {
                    LOG.debug("file empty: {}", file);
                    continue;
                }
                String key = name.replace('.', '/') + '/' + keyTimePath + '/'
                        + fileName.replace(".log.gz", ".") + nodeConfig.id + ".log.gz";
                awsClient.putObject(key, file);
            }

        }

        LOG.info("awsS3Push finished, cost: {}", System.currentTimeMillis() - allStart);

    }

    public String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void write(String name, Object o) {
        String data = toJson(o);
        LOG_MAP.get(name).info(data);
        if (kafkaService.isEnabled())
            kafkaService.send(new ProducerRecord<>(name, data));
    }
}
