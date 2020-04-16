package com.adtiming.om.server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

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
    private static final String CDN_HEADER = "Ali-CDN-Real-IP";

    /***     * 获取客户端ip地址(可以穿透代理)     * @param request     * @return     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader(CDN_HEADER);
        if (!StringUtils.hasText(ip)) {
            for (String header : HEADERS_TO_TRY) {
                ip = request.getHeader(header);
                if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                    break;
                }
            }
        }
        //"***.***.***.***".length() = 15
        if (StringUtils.hasText(ip) && ip.length() > 15) {
            if (ip.indexOf(",") > 0) {
                ip = ip.substring(0, ip.indexOf(","));
            }
        }
        return StringUtils.hasText(ip) ? ip : request.getRemoteAddr();
    }

}
