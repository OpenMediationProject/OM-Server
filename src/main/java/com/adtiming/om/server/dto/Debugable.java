package com.adtiming.om.server.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public abstract class Debugable<T> {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private List<CharSequence> debug;

    public List<CharSequence> getDebug() {
        return debug;
    }

    public void setDebug(List<CharSequence> debug) {
        this.debug = debug;
    }

    @JsonIgnore
    public boolean isDebugEnabled() {
        return debug != null;
    }

    public T addDebug(String str) {
        if (isDebugEnabled()) {
            debug.add(LocalDateTime.now().format(FMT) + " - " + str);
        }
        //noinspection unchecked
        return (T) this;
    }

    public T addDebug(String str, Object... args) {
        if (isDebugEnabled()) {
            debug.add(LocalDateTime.now().format(FMT) + " - " + String.format(str, args));
        }
        //noinspection unchecked
        return (T) this;
    }
}
