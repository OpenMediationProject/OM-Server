package com.adtiming.om.server.service;

import java.io.File;

public interface CloudClient {

    /**
     * put local file into cloud
     *
     * @param key  cloud key, normally a path
     * @param file local File
     */
    void putObject(String key, File file);

    /**
     * check if CloudClient is Enabled
     */
    boolean isEnabled();

    CloudClient CLIENT0 = new CloudClient() {
        @Override
        public void putObject(String key, File file) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEnabled() {
            return false;
        }
    };
}
