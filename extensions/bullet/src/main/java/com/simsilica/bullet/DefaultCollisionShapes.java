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

package com.simsilica.bullet;

import java.util.*;
import java.util.concurrent.*;

import org.slf4j.*;

import com.google.common.base.Function;

import com.jme3.bullet.collision.shapes.CollisionShape;

import com.simsilica.es.*;

/**
 *  Default CollisionShapes implementation that uses an internal index
 *  and can be easily extended to handle custom loading of registry misses.
 *
 *  @author    Paul Speed
 */
public class DefaultCollisionShapes implements CollisionShapes {
    static Logger log = LoggerFactory.getLogger(DefaultCollisionShapes.class);

    private EntityData ed;
    private Map<Integer, CollisionShape> shapeIndex = new ConcurrentHashMap<>();    
 
    private Function<ShapeInfo, CollisionShape> loadFunction;
 
    /**
     *  Creates a CollisionShapes implementation that keeps a simple internal
     *  thread-safe registry of collision shapes.  An optional EntityData instance
     *  can be specified to provide better logging.
     */   
    public DefaultCollisionShapes( EntityData ed ) {
        this.ed = ed;
    } 

    /**
     *  Creates a CollisionShapes implementation that keeps a simple internal
     *  thread-safe registry of collision shapes.  An optional EntityData instance
     *  can be specified to provide better logging.
     */   
    public DefaultCollisionShapes( EntityData ed, Function<ShapeInfo, CollisionShape> loadFunction ) {
        this.ed = ed;
        this.loadFunction = loadFunction;
    } 

    /**
     *  Sets the function that will be used to load collision shapes if they
     *  aren't already registered.  Defaults to null which means all requested shapes
     *  must be preloaded.
     */
    public void setLoadFunction( Function<ShapeInfo, CollisionShape> loadFunction ) {
        this.loadFunction = loadFunction;
    }
 
    /**
     *  Returns the current load function or null if none has been set.
     */   
    public Function<ShapeInfo, CollisionShape> getLoadFunction() {
        return loadFunction;
    } 

    @Override
    public CollisionShape register( ShapeInfo info, CollisionShape shape ) {
        shapeIndex.put(info.getShapeId(), shape);
        if( log.isDebugEnabled() ) {
            log.debug("register(" + info.toString(ed) + ", " + shape + ")");
        }
        return shape;
    } 
 
    @Override
    public CollisionShape getShape( ShapeInfo shape ) {
        CollisionShape result = shapeIndex.get(shape.getShapeId());
        if( result != null ) {
            if( log.isTraceEnabled() ) {
                log.trace("Reusing shape:" + result + " for:" + shape.toString(ed));
            }
            return result;
        }
        result = loadShape(shape);
        if( log.isDebugEnabled() ) {
            log.debug("Loaded shape:" + result + " for:" + shape.toString(ed));
        }
        if( result ==  null ) {
            throw new IllegalArgumentException("No shape found for:" + shape.toString(ed));
        }
        shapeIndex.put(shape.getShapeId(), result);
        return result;        
    }
 
    /**
     *  Can be overridden by subclasses to provide custom shape loading behavior
     *  though it is generally easier to just implement the load function.  By
     *  default, this will call the specified load function or return null if no
     *  function exists.
     */
    protected CollisionShape loadShape( ShapeInfo shape ) {
        if( log.isDebugEnabled() ) {
            log.debug("Loading shape for:" + shape.toString(ed));
        }
        return loadFunction == null ? null : loadFunction.apply(shape);
    }   
}


