// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.service;

import com.adtiming.om.pb.CommonPB.Dict;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class DictManager extends PBLoader {

    private Map<Integer, Dict> idMap = Collections.emptyMap();
    private Map<String, Dict> pathMap = Collections.emptyMap();
    private Map<Integer, List<Dict>> pidMap = Collections.emptyMap();

    void reloadCache() {
        load("om_dict", in -> {
            List<Dict> list = new ArrayList<>(100);
            while (true) {
                Dict o = Dict.parseDelimitedFrom(in);
                if (o == null) break;
                list.add(o);
            }
            setList(list);
        });
    }

    private void setList(List<Dict> list) {
        Map<Integer, Dict> idMap = new HashMap<>(list.size());
        Map<Integer, List<Dict>> pidMap = new HashMap<>();
        Map<String, Dict> pathMap = new HashMap<>(list.size());

        for (Dict d : list) {
            idMap.put(d.getId(), d);
            pidMap.computeIfAbsent(d.getPid(), k -> new ArrayList<>()).add(d);
        }

        this.idMap = idMap;
        this.pidMap = pidMap;

        for (Dict d : idMap.values()) {
            List<String> buf = new ArrayList<>(5);
            getParentPath(d, buf);
            String path = StringUtils.join(buf, '/');
            pathMap.put(path, d);
        }
        this.pathMap = pathMap;
    }

    private void getParentPath(Dict d, List<String> buf) {
        if (d.getPid() == 0) {
            buf.add(String.format("/%s", d.getName()));
            return;
        }
        getParentPath(idMap.get(d.getPid()), buf);
        buf.add(d.getName());
    }

    public Dict getDict(String path) {
        return pathMap.get(path);
    }

    public List<Dict> children(String path) {
        Dict d = getDict(path);
        if (d != null)
            return pidMap.getOrDefault(d.getId(), Collections.emptyList());
        return Collections.emptyList();
    }

    public List<String> childrenVals(String path) {
        List<Dict> list = children(path);
        if (list.isEmpty())
            return Collections.emptyList();
        return list.stream().map(Dict::getValue).collect(Collectors.toList());
    }

    public String val(String path, String Default) {
        Dict d = getDict(path);
        return d == null ? Default : d.getValue();
    }

    public String val(String path) {
        return val(path, null);
    }

    public int intVal(String path, int Default) {
        Dict d = getDict(path);
        return d == null ? Default : NumberUtils.toInt(d.getValue(), Default);
    }

    public int intVal(String path) {
        return intVal(path, 0);
    }

    public long longVal(String path, long Default) {
        Dict d = getDict(path);
        return d == null ? Default : NumberUtils.toLong(d.getValue(), Default);
    }

    public long longVal(String path) {
        return longVal(path, 0L);
    }

    public double doubleVal(String path, double Default) {
        Dict d = getDict(path);
        return d == null ? Default : NumberUtils.toDouble(d.getValue(), Default);
    }

    public double doubleVal(String path) {
        return doubleVal(path, 0.0);
    }

    public double floatVal(String path, float Default) {
        Dict d = getDict(path);
        return d == null ? Default : NumberUtils.toFloat(d.getValue(), Default);
    }

    public double floatVal(String path) {
        return floatVal(path, 0.0f);
    }

    public String[] arrayVal(String path, String separator) {
        String v = val(path, null);
        return v == null ? null : v.split(separator);
    }

    public Stream<String> streamVal(String path, String separator) {
        String v = val(path, null);
        return v == null ? Stream.empty() : Stream.of(v.split(separator));
    }

}
