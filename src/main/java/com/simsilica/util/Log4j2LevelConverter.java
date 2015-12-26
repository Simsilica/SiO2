/*
 * $Id$
 * 
 * Copyright (c) 2015, Simsilica, LLC
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;



/**
 *  Pulls all of the current log4j log levels and makes sure that
 *  JUL logging is configured the same way.
 *
 *  @author    Paul Speed
 */
public class Log4j2LevelConverter {

    private static final Set<java.util.logging.Logger> keepAlive = new HashSet<>();
    private static final Map<Level, java.util.logging.Level> toJul = new HashMap<>(); 
 
    static {
        toJul.put(Level.OFF, java.util.logging.Level.OFF);
        toJul.put(Level.FATAL, java.util.logging.Level.SEVERE);
        toJul.put(Level.ERROR, java.util.logging.Level.SEVERE);
        toJul.put(Level.WARN, java.util.logging.Level.WARNING);
        toJul.put(Level.INFO, java.util.logging.Level.INFO);
        toJul.put(Level.DEBUG, java.util.logging.Level.FINE);
        toJul.put(Level.TRACE, java.util.logging.Level.FINER);
        toJul.put(Level.ALL, java.util.logging.Level.FINEST);
    }
 
    public static void convert() {
        LoggerContext logContext = (LoggerContext)LogManager.getContext(false);
        Map<String, LoggerConfig> map = logContext.getConfiguration().getLoggers();
        for( LoggerConfig config : map.values() ) {
            System.out.println("logger config:" + config);
            setJulLevel(config.getName(), config.getLevel());
        }        
                
    } 

    protected static void setJulLevel( String logger, Level level ) {
        java.util.logging.Level newLevel = toJul.get(level);
        System.out.println( "Setting JUL Log:" + logger + " level to:" + newLevel );
        java.util.logging.Logger l = java.util.logging.Logger.getLogger(logger);
        if( l == null ) {
            System.err.println("WARN: No logger found for:" + logger);
            return;            
        }
        l.setLevel(newLevel);
        
        // If we don't do this then the configuration gets lost when the JUL
        // logger is garbage collected.  So awesome. :-/ 
        keepAlive.add(l);
    } 

}
