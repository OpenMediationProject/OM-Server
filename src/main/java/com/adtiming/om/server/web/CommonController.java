package com.adtiming.om.server.web;

import com.adtiming.om.server.service.LogService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @Desc
 * @Author Summer
 * @Date 2020/4/13 13:30
 */
@RestController
public class CommonController {

    private final LogService logService;
    private static final Logger LOG = LogManager.getLogger();

    public CommonController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping("/s3/exec/{year}/{month}/{day}/{startHour}/{endHour}")
    public ResponseEntity<String> execS3Push(@PathVariable Integer year,
                                             @PathVariable Integer month,
                                             @PathVariable Integer day,
                                             @PathVariable Integer startHour,
                                             @PathVariable Integer endHour,
                                             String secret
    ) {
        if (!"qpJUqiK4j8Dtxb".equals(secret)) {
            return ResponseEntity.ok().body("执行失败S");
        }
        LocalDateTime startTime = LocalDateTime.of(year, month, day, startHour, 0);
        LocalDateTime endTime = LocalDateTime.of(year, month, day, endHour, 0);
        LOG.info("startTime:{}", startTime);
        LOG.info("endTime:{}", endTime);
        while (startTime.compareTo(endTime) <= 0) {
            logService.awsS3Push(startTime);
            startTime = startTime.plusHours(1);
        }
        return ResponseEntity.ok().body("执行完成");
    }
}
