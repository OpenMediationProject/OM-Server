package com.adtiming.om.server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * @Desc
 * @Author Summer
 * @Date 2020/2/10 14:58
 */
public class RequestParamsUtil {
    private static Logger log = LoggerFactory.getLogger(RequestParamsUtil.class);

    private static final String[] HEADERS_TO_TRY = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR",
            "REMOTE_ADDR",
            "HTTP_X_FORWARDED",
            "HTTP_FORWARDED_FOR",
            "X-Real-IP",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_FORWARDED",
            "HTTP_VIA"
    };

    /***     * 获取客户端ip地址(可以穿透代理)     * @param request     * @return     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = null;
        for (String header : HEADERS_TO_TRY) {
            log.debug(request.getHeader(header));
        }
        for (String header : HEADERS_TO_TRY) {
            ip = request.getHeader(header);
            if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                break;
            }
        }
        //"***.***.***.***".length() = 15
        if (ip != null && ip.length() > 15) {
            if (ip.indexOf(",") > 0) {
                ip = ip.substring(0, ip.indexOf(","));
            }
        }
        return ip == null ? request.getRemoteAddr() : ip;
    }

}
