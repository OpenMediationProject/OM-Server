// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.web;

import com.adtiming.om.server.dto.NodeConfig;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@WebFilter(urlPatterns = "/*", filterName = "headFilter")
public class HeadFilter implements Filter {

    @Resource
    private NodeConfig nc;
    private String hv;

    @PostConstruct
    private void init() {
        hv = String.format("%d-%d", nc.getDcenter(), nc.getId());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse res = (HttpServletResponse) response;
        res.setHeader("x-id", hv);
        chain.doFilter(request, response);
    }

}
