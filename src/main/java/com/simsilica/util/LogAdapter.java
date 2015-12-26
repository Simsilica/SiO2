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

import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import org.slf4j.LoggerFactory;

/**
 *  Forwards standard Java logging on to the slf4j logger, including
 *  configuring JUL to match the log levels configured for slf4j logging.
 *
 *  @version   $Revision: 4149 $
 *  @author    Paul Speed
 */
public class LogAdapter extends Handler {

    private final Set<String> seen = new HashSet<String>();
    private static final List<Logger> keepAlive = new ArrayList<>();

    public static void initialize() {
        Logger root = LogManager.getLogManager().getLogger("");
        
        for( Handler h : root.getHandlers() )
            {
            //System.out.println( "Handler:" + h );
            root.removeHandler(h);
            }
            
        LogAdapter adapter = new LogAdapter();
        root.addHandler(adapter);
 
        // What's the root level
        org.slf4j.Logger rootLog = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        if( rootLog != null ) {
            System.out.println("Setting root JUL log level to:" + getJulLevel(rootLog));
            root.setLevel(getJulLevel(rootLog));
        }
        
        try {
            // See if we are running log4j
            // We use the LoggerConfig to verify that we have enough log4j to
            // do what we need
            Class.forName("org.apache.logging.log4j.core.config.LoggerConfig");
            
            // I think we need to call this with reflection also
            Log4j2LevelConverter.convert();
        } catch( ClassNotFoundException ex ) {
            // That's ok
        }
    }

    @Override
    public void close() {
    }

    @Override
    public void flush() {
    }

    protected static String foldMessage( LogRecord record ) {
        String msg = record.getMessage();
        Object[] parms = record.getParameters();
        if( parms != null && parms.length > 0 )
            msg = MessageFormat.format(msg, record.getParameters());
        return msg;
    }

    protected static Level getJulLevel( org.slf4j.Logger log ) {
        if( log.isTraceEnabled() ) {
            return Level.FINER;
        } else if( log.isDebugEnabled() ) { 
            return Level.FINE;
        } else if( log.isInfoEnabled() ) { 
            return Level.INFO;
        } else if( log.isWarnEnabled() ) { 
            return Level.WARNING;
        } else if( log.isErrorEnabled() ) {
            return Level.SEVERE;
        } else {
            return Level.OFF;
        } 
    }

    @Override
    public void publish( LogRecord record ) {
        String message = foldMessage(record);
        String logger = record.getLoggerName();
        if( logger == null ) {
            logger = "";
        }
            
        Level lvl = record.getLevel();        
        //System.out.println( "*****publish: [" + logger + "][" + lvl + "]" + message );
 
        org.slf4j.Logger log = LoggerFactory.getLogger(logger);

        if( lvl == Level.OFF ) {
        } else if( lvl == Level.CONFIG ) {
            log.info(message, record.getThrown());
        } else if( lvl == Level.FINE ) {
            log.debug(message, record.getThrown());
        } else if( lvl == Level.FINER ) {
            log.trace(message, record.getThrown());
        } else if( lvl == Level.FINEST ) {
            log.trace(message, record.getThrown());
        } else if( lvl == Level.INFO ) {
            log.info(message, record.getThrown());
        } else if( lvl == Level.SEVERE ) {
            log.error(message, record.getThrown());
        } else if( lvl == Level.WARNING ) {
            log.warn(message, record.getThrown());
        } else {
            log.info(message, record.getThrown());
        }
        //System.out.println( "   DONE publish: [" + logger + "][" + lvl + "]" + message );
    }
}


