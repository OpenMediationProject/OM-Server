// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.web;

import com.adtiming.om.server.util.Compressor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.protocol.HTTP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

public class BaseController {

    private static final Logger LOG = LogManager.getLogger();

    @Resource
    private ObjectMapper objectMapper;

    void response(HttpServletResponse res, Object o) throws IOException {
        if (o == null || res.isCommitted())
            return;

        res.setContentType("application/json;charset=UTF-8");
        byte[] b = objectMapper.writeValueAsBytes(o);
        if (b.length < 200) {
            try (OutputStream out = res.getOutputStream()) {
                res.setContentLength(b.length);
                out.write(b);
            }
        } else {
            try (OutputStream out = new GZIPOutputStream(res.getOutputStream())) {
                res.setHeader(HTTP.CONTENT_ENCODING, "gzip");
                out.write(b);
            }
        }

    }

    ResponseEntity<?> response(Object o) throws IOException {
        if (o == null)
            return ResponseEntity.noContent().build();

        ResponseEntity.BodyBuilder bb = ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON_UTF8);

        byte[] b = objectMapper.writeValueAsBytes(o);
        if (b.length < 200) {
            return bb.contentLength(b.length).body(b);
        }

        b = Compressor.gzip(b);
        return bb.contentLength(b.length).header(HTTP.CONTENT_ENCODING, "gzip").body(b);

    }

    @ExceptionHandler(Throwable.class)
    public void exceptionHandler(HttpServletRequest req, HttpServletResponse resp, Throwable t) throws IOException {
        if (t instanceof ServletRequestBindingException
                || t instanceof BindException
                || t instanceof MethodArgumentTypeMismatchException
                || t instanceof HttpMessageConversionException) {
            sendError(resp, 400, t.getMessage());
        } else if (t instanceof HttpMediaTypeNotSupportedException) {
            sendError(resp, 400, t.getMessage());
            LOG.warn("media type error, {}, {}?{}", t.toString(), req.getRequestURI(), req.getQueryString());
        } else {
            LOG.error("servlet error, {}?{}", req.getRequestURI(), req.getQueryString(), t.getCause() == null ? t : t.getCause());
            sendError(resp, 500, "Server Error");
        }
    }

    private void sendError(HttpServletResponse res, int sc, String msg) throws IOException {
        if (res.isCommitted()) return;
        res.sendError(sc, msg);
    }

}
