// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.service;

import com.adtiming.om.server.util.Encrypter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

public abstract class PBLoader {

    private static final Logger LOG = LogManager.getLogger();

    // store file md5, check if file has been modified
    private static final Map<String, String> FILE_MD5_MAP = new ConcurrentHashMap<>();
    protected static final File CACHE_DIR = new File("cache");

    static {
        if (!CACHE_DIR.exists() && CACHE_DIR.mkdirs())
            LOG.debug("mkdir {}", CACHE_DIR);
    }

    protected boolean load(String name, LoadFun fn) {
        return load(name, fn, false);
    }

    protected boolean load(String name, LoadFun fn, boolean forceLoad) {
        LOG.debug("load {} start", name);
        long start = System.currentTimeMillis();
        File file = new File(CACHE_DIR, name + ".gz");
        if (!file.exists()) {
            LOG.warn("load {} failed, file not exists", name);
            return false;
        }

        String fileMd5 = Encrypter.md5(file);
        String filePath = file.getPath();
        if (!forceLoad && fileMd5.equals(FILE_MD5_MAP.get(filePath))) {
            LOG.debug("skip {}, not modified", name);
            return false;
        }

        try (InputStream in = new GZIPInputStream(new FileInputStream(file))) {
            fn.read(in);
            FILE_MD5_MAP.put(filePath, fileMd5);
            LOG.info("load {} finished, cost {} ms, md5: {}", name, System.currentTimeMillis() - start, fileMd5);
            return true;
        } catch (Exception e) {
            LOG.error("load {} error", name, e);
            return false;
        }
    }

    @FunctionalInterface
    protected interface LoadFun {
        void read(InputStream in) throws Exception;
    }

    /**
     * new HashMap 使用最优大小初始化
     */
    protected <K, V> Map<K, V> newMap(Map<K, V> map, int Default, int gt) {
        return new HashMap<>((map == null || map.isEmpty()) ? Default : map.size() + gt);
    }

    /**
     * new HashSet 使用最优大小初始化
     */
    protected <T> Set<T> newSet(Set<T> set, int Default, int gt) {
        return new HashSet<>((set == null || set.isEmpty()) ? Default : set.size() + gt);
    }
}
