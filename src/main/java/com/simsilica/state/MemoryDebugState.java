/*
 * $Id$
 * 
 * Copyright (c) 2018, Simsilica, LLC
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

package com.simsilica.state;

import org.slf4j.*;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.util.MemoryUtils;

import com.simsilica.lemur.core.VersionedHolder;

/**
 *  Uses the DebugHudState to display memory usage.
 *
 *  @author    Paul Speed
 */
public class MemoryDebugState extends BaseAppState {

    public static String HEAP = "Heap";
    public static String DIRECT = "Direct";

    private static float BYTE_TO_MB = 1f/(1024 * 1024);

    static Logger log = LoggerFactory.getLogger(MemoryDebugState.class);
    
    private DebugHudState hud;
    private DebugHudState.Location location = DebugHudState.Location.Right; 
    private boolean detailedStats;
    
    // Making direct memory usage display optional and off by default.
    // I guess some time on JDK 1.7+, the heap memory issues must have been
    // fixed because now this seems to be managed differently and will 
    // always show as 100%.  -XX:MaxDirectMemorySize no longer seems to have
    // any affect on the max size reported by the JVM.
    private boolean includeDirectMem;

    private float updateInterval;
    private float nextUpdate;

    private VersionedHolder<String> heap;
    private long lastHeapUsage;
    private long lastHeapTotal;
    
    private VersionedHolder<String> direct;
    private long lastDirectUsage;
    private long lastDirectTotal; 

    /** 
     *  Creates a MemoryDebugState that will put memory stats in the
     *  right debug panel and update them once a second.
     */
    public MemoryDebugState() {
        this(DebugHudState.Location.Right, 1, false, false);        
    }

    /**
     *  Creates a MemoryDebugState that will put memory stats  in the specified
     *  location and update them as often as the specified updateInterval in seconds.
     *  If detailedStats is true then the raw memory values (in megabytes) are displayed
     *  with the percentage.  If detailedStats is false then only the percentage is 
     *  displayed.  If includeDirectMem is true then a direct memory usage line is 
     *  displayed though this seems to be pretty unncessary on modern JVMs.
     */    
    public MemoryDebugState( DebugHudState.Location location, float updateInterval, 
                             boolean detailedStats, boolean includeDirectMem ) {
        this.location = location;
        this.updateInterval = updateInterval;
        this.detailedStats = detailedStats;
        this.includeDirectMem = includeDirectMem;
    }
    
    public void setDetailedStats( boolean detailedStats ) {
        this.detailedStats = detailedStats;
        
        // Trigger a refresh next update
        lastHeapUsage = 0;
        lastDirectUsage = 0;
        
        // Trigger an update
        nextUpdate = 0;
    }
    
    public boolean getDetailedStats() {
        return detailedStats;
    } 
    
    @Override
    protected void initialize( Application app ) {
        hud = getState(DebugHudState.class);
        if( hud == null ) {
            throw new RuntimeException("MemoryDebugState requires the DebugHudState");
        }
    }
    
    @Override
    protected void cleanup( Application app ) {
    }
    
    @Override
    protected void onEnable() {
        this.heap = hud.createDebugValue(HEAP, location);
        if( includeDirectMem ) {
            this.direct = hud.createDebugValue(DIRECT, location);
        }    
    }
 
    protected void updateMemoryStats() {
        Runtime rt = Runtime.getRuntime();
 
        long free = rt.freeMemory();       
        long total = rt.totalMemory();
        long max = rt.maxMemory();
        long used = total - free;
        
        if( lastHeapUsage != used || lastHeapTotal != max ) {
            lastHeapUsage = used;
            lastHeapTotal = max;
            float percent = 100f * used / max;
            
            String p = String.format("%.2f%%", percent); 
            if( detailedStats ) {
                String mb = String.format("%.2f/%.2f mb", used * BYTE_TO_MB, max * BYTE_TO_MB);  
                heap.setObject(p + " (" + mb + ")");
            } else {
                heap.setObject(p);
            }
        }
        
        if( includeDirectMem ) {
            long directTotal = MemoryUtils.getDirectMemoryTotalCapacity();
            long directUsage = MemoryUtils.getDirectMemoryUsage(); 
            if( lastDirectUsage != directUsage || lastDirectTotal != directTotal ) {
                lastDirectUsage = directUsage;
                lastDirectTotal = directTotal;
                float percent = 100f * directUsage / directTotal;
    
                String p = String.format("%.2f%%", percent);
                if( detailedStats ) {            
                    String mb = String.format("%.2f/%.2f mb", directUsage * BYTE_TO_MB, directTotal * BYTE_TO_MB);  
                    direct.setObject(p + " (" + mb + ")");
                    // The count seems to be unhelpful
                    //direct.setObject(p + " (" + mb + ") #" + MemoryUtils.getDirectMemoryCount());
                } else {
                    direct.setObject(p);
                }
            }
        }
        
    }
    
    @Override
    public void update( float tpf ) {
        nextUpdate -= tpf;
        if( nextUpdate <= 0 ) {
            updateMemoryStats();
            nextUpdate = updateInterval;
        }
    }
    
    @Override
    protected void onDisable() {
        hud.removeDebugValue(HEAP);
        if( includeDirectMem ) {
            hud.removeDebugValue(DIRECT);
        }
    }
}
