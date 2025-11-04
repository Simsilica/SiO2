/*
 * $Id$
 *
 * Copyright (c) 2025, Simsilica, LLC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.simsilica.util;

import java.io.PrintWriter;
import java.util.*;

import org.slf4j.*;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;


/**
 *  When guava caches are created, they can be registered here so that
 *  diagnostic code can examine their status.
 *
 *  @author    Paul Speed
 */
public class CacheTracker {
    static Logger log = LoggerFactory.getLogger(CacheTracker.class);

    private static WeakHashMap<Cache<?,?>, CacheInfo> caches = new WeakHashMap<>();

    public static void track( Cache<?,?> cache, String name, boolean reportStats ) {
        CacheInfo existing = caches.get(cache);
        if( existing != null ) {
            throw new IllegalArgumentException("Cache:" + name + " is already tracked as:" + existing.name);
        }
        CacheInfo info = new CacheInfo(name, reportStats);
        caches.put(cache, info);
    }

    public static void generateSizeReport( PrintWriter out, String indent ) {
        // Collect all of the values at once and put them in order
        Map<String, Long> sizes = new TreeMap<>();
        for( Map.Entry<Cache<?,?>, CacheInfo> entry : caches.entrySet() ) {
            Cache<?,?> key = entry.getKey();
            if( key == null ) {
                // Guard against the weak ref clearing before we read it
                continue;
            }
            sizes.put(entry.getValue().name, key.size());
        }

        for( Map.Entry<String, Long> e : sizes.entrySet() ) {
            out.println(indent + String.format("%s:%,d", e.getKey(), e.getValue()));
        }
    }

    public static void generateStatsReport( PrintWriter out, String indent ) {
        // Collect all of the values at once and put them in order
        Map<String, CombinedStats> sizes = new TreeMap<>();
        for( Map.Entry<Cache<?,?>, CacheInfo> entry : caches.entrySet() ) {
            Cache<?,?> key = entry.getKey();
            if( key == null ) {
                // Guard against the weak ref clearing before we read it
                continue;
            }
            if( entry.getValue().reportStats ) {
                sizes.put(entry.getValue().name, new CombinedStats(key.size(), key.stats()));
            }
        }

        for( Map.Entry<String, CombinedStats> entry : sizes.entrySet() ) {
            CombinedStats cs = entry.getValue();
            out.println(indent + String.format("%s:%,d stats:%s", entry.getKey(), cs.size, cs.cacheStats));
        }
    }


    protected static class CacheInfo {
        private String name;
        private boolean reportStats;

        protected CacheInfo( String name, boolean reportStats ) {
            this.name = name;
            this.reportStats = reportStats;
        }
    }

    protected static class CombinedStats {
        private long size;
        private CacheStats cacheStats;

        protected CombinedStats( long size, CacheStats cacheStats ) {
            this.size = size;
            this.cacheStats = cacheStats;
        }
    }
}



