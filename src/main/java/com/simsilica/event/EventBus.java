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

package com.simsilica.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *  Dispatches events to listeners and provides a way for listeners
 *  to register themselves.  This is similar in concept to Guava's 
 *  EventBus with some noteable differences:
 *  <ul>
 *  <li>Type and event objects are separate which means the same event
 *  class can be used for multiple event types.
 *  <li>The event bus is singleton which provides a single central
 *  place for event dispatch and registration.
 *  <li>Listeners can be added with an EventListener interface OR by
 *  using reflective methods (similar to Guava's event bus).
 *  </ul>
 *
 *  @author    Paul Speed
 */
public class EventBus {
    
    static Logger log = LoggerFactory.getLogger(EventBus.class);

    private static final EventBus instance = new EventBus();

    private final ListenerList all = new ListenerList();
    private final Map<EventType,ListenerList> listenerMap = new ConcurrentHashMap<>(); 
    private final Lock lock = new ReentrantLock();    

    protected EventBus() {
    } 
    
    /**
     *  Publishes the specified event to the event bus, delivering it to 
     *  all listeners registered for the particular type.
     *
     *  <p>This is the same as calling EventBus.getInstance().publishEvent(type, event)</p>    
     */
    public static <E> void publish( EventType<E> type, E event ) {
        getInstance().publishEvent(type, event); 
    }

    /**
     *  Adds a generic listener that will have its events delivered through
     *  reflection based on the event names.  For example, if the expected
     *  event type is ErrorEvent.fatalError then the event type name is
     *  "FatalError" and the expected method name will be either "onFatalError" or
     *  "fatalError" with a single argument that is the event.  (Note: It is ok if the
     *  method is not public if the current security settings allow overriding
     *  method accessibility.)
     *
     *  <p>This is the same as calling EventBus.getInstance().addListenerMethods(listener, types)</p>    
     *
     *  @throws IllegalArgumentException is any of the dispatch methods is
     *           missing.
     */
    public static void addListener( Object listener, EventType... types ) {
        getInstance().addListenerMethods(listener, types);
    }

    /**
     *  Reverses the generic addListener() method by removing the specified
     *  listener from any of the specified registered types.
     *
     *  <p>This is the same as calling EventBus.getInstance().renmoveListenerMethods(listener, types)</p>    
     */   
    public static void removeListener( Object listener, EventType... types ) {
        getInstance().removeListenerMethods(listener, types);
    }

    /**
     *  Adds a listener object that will be notified about events of the specified
     *  type.
     *
     *  <p>This is the same as calling EventBus.getInstance().addEventListener(type, listener)</p>    
     */
    public static <E> void addListener( EventType<E> type, EventListener<E> listener ) {
        getInstance().addEventListener(type, listener);
    }
    
    /**
     *  Removes a listener object that was previously registered with addListener().
     *
     *  <p>This is the same as calling EventBus.getInstance().removeEventListener(type, listener)</p>    
     */
    public static <E> void removeListener( EventType<E> type, EventListener<E> listener ) {
        getInstance().removeEventListener(type, listener);
    }
 
    /**
     *  Publishes the specified event to the event bus, delivering it to 
     *  all listeners registered for the particular type.
     */
    public <E> void publishEvent( EventType<E> type, E event ) {
    
        if( log.isTraceEnabled() ) {
            log.trace("publishEvent(" + type + ", " + event + ")");
        }

        // Deliver to any global listeners first and we don't factor
        // them into the delivery check.  The global list is usually used
        // for things like lifecycle logging and not actual event handling.
        deliver(null, event, all);
        
        //System.out.println("listeners for type:" + type + " = " + getListeners(type));
        boolean delivered = deliver(type, event, getListeners(type));
        /*for( EventListener l : getListeners(type).getArray() ) {
            try {
                l.newEvent(type, event);
                delivered = true;
            } catch( Throwable t ) {
                log.error("Error handling event:" + event + " for type:" + type + "  in handler:" + l, t);
                if( type != ErrorEvent.dispatchError ) {
                    publishEvent(ErrorEvent.dispatchError, new ErrorEvent(t, type, event));
                } 
            }
        }*/
        if( !delivered ) {
            log.debug("Undelivered event type:" + type + "  Event:" + event);
        }
    }
    
    protected <E> boolean deliver( EventType<E> type, E event, ListenerList listeners ) {
    
        if( listeners.isEmpty() ) {
            return false;
        }
    
        boolean delivered = false;
        for( EventListener l : listeners.getArray() ) {
            try {
                l.newEvent(type, event);
                delivered = true;
            } catch( Throwable t ) {
                log.error("Error handling event:" + event + " for type:" + type + "  in handler:" + l, t);
                if( type != ErrorEvent.dispatchError ) {
                    publishEvent(ErrorEvent.dispatchError, new ErrorEvent(t, type, event));
                } 
            }
        }
        return delivered;
    }
 
    /**
     *  Returns the singleton EventBus instance.
     */   
    public static EventBus getInstance() {
        return instance;
    }

    /**
     *  Returns the list of event listeners for a particular event type,
     *  creating the list if necessary.
     */    
    protected ListenerList getListeners( EventType type ) {
        ListenerList list = listenerMap.get(type);
        if( list == null ) {
            // Now we need to get the lock so we are the only one
            // writing.
            lock.lock();
            try {
                // Check again to see if the list was created while we
                // waited to get the lock.  (safe double-checked locking)
                list = listenerMap.get(type);
                if( list != null ) {
                    return list;
                }
                list = new ListenerList();
                listenerMap.put(type, list);
            } finally {
                lock.unlock();
            }
        }
        return list;            
    }

    public <E> void addEventListener( EventType<E> type, EventListener<E> listener ) {
        getListeners(type).add(listener);
    }
    
    public <E> void removeEventListener( EventType<E> type, EventListener<E> listener ) {
        getListeners(type).remove(listener);
    }  
 
    /**
     *  Adds a special listener that will receive _all_ event notifications.  This
     *  can be used to do logging or debugging on a global level and is generally
     *  not useful for normal event delivery.
     */
    public void addDispatchListener( EventListener listener ) {
        all.add(listener);   
    }
 
    /**
     *  Removes a listener previously registered with the addDispatchListener() method.
     */   
    public void removeDispatchListener( EventListener listener ) {
        all.remove(listener);
    }
    
    protected Method findMethod( Class c, EventType type ) throws NoSuchMethodException {
    
        // First try the 'on' + name version
        String name1 = "on" + type.getName(); 
        try {
            return c.getDeclaredMethod(name1, type.getEventClass());
        } catch( NoSuchMethodException e ) {
            // That's ok, we handle the miss later 
        }
        
        // Else try with the direct name, lower-cased appropriately
        String name2 = type.getName();
        if( Character.isUpperCase(name2.charAt(0)) && Character.isLowerCase(name2.charAt(1)) ) {
            name2 = Character.toLowerCase(name2.charAt(0)) + name2.substring(1);
        }
        try {       
            return c.getDeclaredMethod(name2, type.getEventClass());
        } catch( NoSuchMethodException e ) {
            // That's ok, we handle the miss later 
        }
        
        if( c.getSuperclass() != null ) {
            // Try the super class
            return findMethod(c.getSuperclass(), type);
        }
               
        throw new NoSuchMethodException(c.getName() + "." + name1 + "(" + type.getEventClass().getName() + ")"
                                        + " or " + c.getName() + "." + name2 + "(" + type.getEventClass().getName() + ")");  
    }
 
    /**
     *  Adds a generic listener that will have its events delivered through
     *  reflection based on the event names.  For example, if the expected
     *  event type is ErrorEvent.fatalError then the event type name is
     *  "FatalError" and the expected method name will be either "onFatalError" or
     *  "fatalError" with a single argument that is the event.  (Note: It is ok if the
     *  method is not public if the current security settings allow overriding
     *  method accessibility.)
     *
     *  @throws IllegalArgumentException is any of the dispatch methods is
     *           missing.
     */
    public void addListenerMethods( Object listener, EventType... types ) {    
        Class c = listener.getClass();
        for( EventType type : types ) {
            try {           
                Method m = findMethod(c, type); 
                if( !m.isAccessible() ) {
                    m.setAccessible(true);
                }
                getListeners(type).add(new MethodDispatcher(listener, m));
            } catch( NoSuchMethodException e ) {
                throw new IllegalArgumentException("Event method not found for:" + type + " on object:" + listener, e);
            }
        }
    }
 
    /**
     *  Reverses the generic addListenerMethods() method by removing the specified
     *  listener from any of the specified registered types.
     */   
    public void removeListenerMethods( Object listener, EventType... types ) {
        for( EventType type : types ) {
            ListenerList listeners = getListeners(type);
            for( EventListener l : listeners.getArray() ) {
                if( !(l instanceof MethodDispatcher) ) {
                    continue;
                }
                MethodDispatcher md = (MethodDispatcher)l;
                if( md.delegate == listener || md.delegate.equals(listener) ) {
                    removeEventListener(type, md);
                } 
            }
        }
    }
 
    private class MethodDispatcher implements EventListener {
    
        private Object delegate;
        private Method method;
        
        public MethodDispatcher( Object delegate, Method m ) {
            if( m == null ) {                    
                throw new IllegalArgumentException("Method cannot be null.");
            }
            this.delegate = delegate;
            this.method = m;
        }
        
        @Override
        public void newEvent( EventType type, Object event ) {
            try {
                method.invoke( delegate, event );
            } catch( IllegalAccessException | InvocationTargetException ex ) {
                throw new RuntimeException("Error calling:" + method + " for event:" + event, ex);
            }
        }    
    }
    
    private class ListenerList {
        
        private final List<EventListener> list = new ArrayList<>();
        private volatile EventListener[] array = null;  
 
        public ListenerList() {
            resetArray();
        }
        
        public boolean isEmpty() {
            // Relatively safe here because we know a little about how
            // ArrayList is implemented and our constant usage of a volatile
            // variable means that this threads view of the data structure is
            // likely to be up to date.
            return list.isEmpty();
        }
        
        protected final void resetArray() {
            // Presumes we already have the lock
            //array = list.toArray(array != null ? array : new EventListener[list.size()]);
            // Can't reuse the array or we create the exact threading problem we
            // were trying to avoid.
            array = list.toArray(new EventListener[list.size()]);
            
        }
        
        protected final EventListener[] getArray() {
            return array;
        }
    
        public void add( EventListener listener ) {
            // Go ahead and grab the central lock
            lock.lock();
            try {
                list.add(listener);
                
                // Implement 'copy on write'
                resetArray();
            } finally {
                lock.unlock();
            }
        }
        
        public void remove( EventListener listener ) {
            // Go ahead and grab the central lock
            lock.lock();
            try {
                list.remove(listener);
                
                // Implement 'copy on write'
                resetArray();
            } finally {
                lock.unlock();
            }
        }
    }   
}

