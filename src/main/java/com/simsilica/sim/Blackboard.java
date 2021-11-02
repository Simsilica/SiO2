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

package com.simsilica.sim;

import java.util.*;
import java.util.concurrent.*;

import org.slf4j.*;

/**
 *  A general place for posting standard shared global objects
 *  where services can look them up.  The blackboard is thread
 *  safe though the objects it contains might not be.
 *
 *  In general, "global variables" are bad(tm) but a blackboard can be a
 *  way to extend the service publishing model to post shared data.
 *
 *  For example, in a single-player, non-networked game, there are three
 *  possible approaches to something like an 'eye position'.
 *  1) a well-known game service that other services can ask for and
 *      get the position from that.
 *  2) broadcast event bus messages every time the position changes.
 *  3) post the position to a blackboard where any service can look at
 *      it when they need it.
 *
 *  (1) is perhaps the best over-all approach but there can be many
 *  different ways to manage a camera/eye position.  To write services
 *  that don't care how that position is managed and merely want to
 *  adjust their view accordingly, there would either need to be a
 *  more general "eye position providing" interface that all such managing
 *  services would extend or a special holder object that acts like a service
 *  just to hold the value.  Which in the end would just be a verbose way
 *  of option (3).
 *
 *  Note: that all of the normal pitfalls of global variables still
 *  apply and care must be taken.  In our example, publishing "eye position"
 *  to the blackboard already limits the game to a single eye.  Pitfalls a plenty.
 *
 *  @author    Paul Speed
 */
public class Blackboard {
    static Logger log = LoggerFactory.getLogger(Blackboard.class);

    private Map<Key, Object> index = new ConcurrentHashMap<>();

    public Blackboard() {
    }

    public <T> T get( Class<T> type ) {
        return type.cast(index.get(new Key(type)));
    }

    public <T> T get( String id, Class<T> type ) {
        return type.cast(index.get(new Key(id, type)));
    }

    public Object get( String id ) {
        return index.get(new Key(id));
    }

    public void set( String id, Object value ) {
        if( value == null ) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        set(new Key(id), value);
    }

    public <T> void set( String id, Class<? super T> type, T value ) {
        if( value == null ) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        set(new Key(id, type), value);
    }

    public <T> void set( Class<? super T> type, T value ) {
        set(new Key(type), value);
    }

    protected void set( Key key, Object value ) {
        Object existing = index.get(key);
        if( existing != null && !Objects.equals(value, existing) ) {
            throw new IllegalArgumentException("There is already a value set for:" + key);
        }
        index.put(key, value);
    }

    public void update( String id, Object value ) {
        update(new Key(id), value);
    }

    public <T> void update( String id, Class<? super T> type, T value ) {
        update(new Key(id, type), value);
    }

    public <T> void update( Class<? super T> type, T value ) {
        update(new Key(type), value);
    }

    protected void update( Key key, Object value ) {
        if( value == null ) {
            index.remove(key);
        } else {
            index.put(key, value);
        }
    }

    private static class Key {
        String id;
        Class type;

        public Key( Class type ) {
            this(type.getName(), type);
        }

        public Key( String id ) {
            this(id, Object.class);
        }

        public Key( String id, Class type ) {
            this.id = id;
            this.type = type;
        }

        public int hashCode() {
            return Objects.hash(id, type);
        }

        public boolean equals( Object o ) {
            if( o == this ) {
                return true;
            }
            if( o == null || o.getClass() != getClass() ) {
                return false;
            }
            Key other = (Key)o;
            if( !Objects.equals(other.id, id) ) {
                return false;
            }
            if( !Objects.equals(other.type, type) ) {
                return false;
            }
            return true;
        }
    }
}
