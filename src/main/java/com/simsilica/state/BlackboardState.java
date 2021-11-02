/*
 * $Id$
 *
 * Copyright (c) 2021, Simsilica, LLC
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

import java.util.function.Consumer;
import java.util.concurrent.Callable;

import org.slf4j.*;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;

import com.simsilica.sim.Blackboard;
import com.simsilica.sim.BlackboardListener;

/**
 *  Provides generic access to a blackboard object that is convenient
 *  to access from other app states.
 *
 *  @author    Paul Speed
 */
public class BlackboardState extends BaseAppState {

    static Logger log = LoggerFactory.getLogger(BlackboardState.class);

    private Blackboard blackboard;

    public BlackboardState() {
        this(new Blackboard());
    }

    /**
     *  Creates a blackboard state with the specified instance or
     *  null if this state should look up the blackboard from
     *  the GameSystemsState.
     */
    public BlackboardState( Blackboard blackboard ) {
        this.blackboard = blackboard;
    }

    public Blackboard getBlackboard() {
        return blackboard;
    }

    // For convenience, we'll copy a lot of the Blackboard methods
    // and forward them.

    public <T> T get( Class<T> type ) {
        return blackboard.get(type);
    }

    public <T> T get( String id, Class<T> type ) {
        return blackboard.get(id, type);
    }

    public Object get( String id ) {
        return blackboard.get(id);
    }

    public <T> T get( Class<T> type, Callable<T> initialValue ) {
        return blackboard.get(type, initialValue);
    }

    public <T> T get( String id, Class<T> type, Callable<T> initialValue ) {
        return blackboard.get(id, type, initialValue);
    }

    public <T> T get( String id, Callable<T> initialValue ) {
        return blackboard.get(id, initialValue);
    }

    public void set( String id, Object value ) {
        blackboard.set(id, value);
    }

    public <T> void set( String id, Class<? super T> type, T value ) {
        blackboard.set(id, type, value);
    }

    public <T> void set( Class<? super T> type, T value ) {
        blackboard.set(type, value);
    }

    public void update( String id, Object value ) {
        blackboard.update(id, value);
    }

    public <T> void update( String id, Class<? super T> type, T value ) {
        blackboard.update(id, type, value);
    }

    public <T> void update( Class<? super T> type, T value ) {
        blackboard.update(type, value);
    }

    public void addBlackboardListener( BlackboardListener l ) {
        blackboard.addBlackboardListener(l);
    }

    public void removeBlackboardListener( BlackboardListener l ) {
        blackboard.removeBlackboardListener(l);
    }

    public <T> void onInitialize( Class<T> type, Consumer<T> consumer ) {
        blackboard.onInitialize(type, consumer);
    }

    public <T> void onInitialize( String id, Class<T> type, Consumer<T> consumer ) {
        blackboard.onInitialize(id, type, consumer);
    }

    public void onInitialize( String id, Consumer consumer ) {
        blackboard.onInitialize(id, consumer);
    }

    @Override
    protected void initialize( Application app ) {
        if( blackboard == null ) {
            this.blackboard = getState(GameSystemsState.class, true).get(Blackboard.class);
        }
    }

    @Override
    protected void cleanup( Application app ) {
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }
}
