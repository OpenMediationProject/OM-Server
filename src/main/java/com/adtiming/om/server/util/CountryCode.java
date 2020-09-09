package com.adtiming.om.server.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Created by wuwei on 6/19/20.
 * CountryCode
 */
public abstract class CountryCode {

    private static final Logger log = LogManager.getLogger();

    private static final Map<String, String> a2a3;
    private static final Map<String, String> a3a2;
    private static final Map<String, String> numA2;
    public static final String UNKNOWN = "";

    static {
        a2a3 = new HashMap<>();
        a3a2 = new HashMap<>();
        numA2 = new HashMap<>();
        final String fileName = "iso-3166-1.csv";
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream(fileName)), UTF_8))) {
            in.lines().map(line -> line.split(",")).forEach(s -> {
                String alpha2 = s[0];
                String alpha3 = s[1];
                String num = s[2];
                a2a3.put(alpha2, alpha3);
                a3a2.put(alpha3, alpha2);
                numA2.put(num, alpha2);
            });
        } catch (Exception e) {
            log.error("load {} error", fileName, e);
        }
    }

    private CountryCode() {
    }

    /**
     * Convert country code to alpha2
     */
    public static String convertToA2(String code) {
        code = StringUtils.trimToEmpty(code).toUpperCase();
        if (code.length() == 3) {
            if (NumberUtils.isDigits(code)) {
                return numA2.getOrDefault(code, UNKNOWN);
            }
            return a3a2.getOrDefault(code, UNKNOWN);
        } else if (code.length() == 2) {
            if (code.equals("UK")) {
                // Special treatment UK to GB
                return "GB";
            }
            return a2a3.containsKey(code) ? code : UNKNOWN;
        } else {
            return UNKNOWN;
        }
    }

    /**
     * get all alpha2,alpha3
     * <p>
     * read-only
     */
    public static Map<String, String> getAllCountries() {
        return Collections.unmodifiableMap(a2a3);
    }
}
