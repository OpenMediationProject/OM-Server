// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.adtiming.om.server.dto;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/**
 * version
 */
public class Version implements Comparable<Version> {

    private ArtifactVersion v;

    private Version(String version) {
        this.v = new DefaultArtifactVersion(version);
    }

    public static Version of(String version) {
        return new Version(version);
    }

    /**
     * greater than
     */
    public boolean gt(Version other) {
        return compareTo(other) > 0;
    }

    /**
     * greater than or equals
     */
    public boolean ge(Version other) {
        return compareTo(other) >= 0;
    }

    /**
     * less than
     */
    public boolean lt(Version other) {
        return compareTo(other) < 0;
    }

    /**
     * less than or equals
     */
    public boolean le(Version other) {
        return compareTo(other) <= 0;
    }

    /**
     * equals
     */
    public boolean eq(Version other) {
        return compareTo(other) == 0;
    }

    /**
     * not equals
     */
    public boolean ne(Version other) {
        return compareTo(other) != 0;
    }

    /**
     * in close range[], from & to are nullable
     */
    public boolean in(Version from, Version to) {
        if (from != null && lt(from))
            return false;
        if (to != null && gt(to))
            return false;
        return true;
    }

    @Override
    public int compareTo(Version o) {
        return v.compareTo(o.v);
    }

}
