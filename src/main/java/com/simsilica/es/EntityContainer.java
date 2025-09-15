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

package com.simsilica.es;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Manages a set of entities with implementation-specific
 *  management of some 'view' object associated with each managed
 *  entity.  This codifies a pretty standard set of logic for dealing
 *  with views of entities.
 *
 *  <p>Subclasses must implement the parameterized addObject(), updateObject(), and
 *  removeObject() methods to handle implementation specific details for
 *  the container's view.  For example, addObject() might load a model or
 *  create a 2D  icon while updateObject() would move it to an appropriate location
 *  or change its displayed model, etc..</p>
 *
 *  <p>It is up to the caller to call start() and stop() as desired for
 *  the lifecycle of the container.  The caller MUST call update() periodically
 *  for the addObject(), updateObject(), and removeObject() implementation
 *  methods to be called.</p>
 *
 *  @param <T> Denotes the type of 'view' object this container manages.
 *
 *  @author    Paul Speed
 */
public abstract class EntityContainer<T> {

    static Logger log = LoggerFactory.getLogger(EntityContainer.class);

    private EntityData ed;
    private EntityCriteria criteria;
    private EntitySet entities;
    private T[] array;
    private Map<EntityId, T> objects = new HashMap<>();
    private Class parameter;

    @SuppressWarnings("unchecked")
    @SafeVarargs
    protected EntityContainer( EntityData ed, Class<? extends EntityComponent>... componentTypes ) {
        this(ed, null, componentTypes);
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    protected EntityContainer( EntityData ed, ComponentFilter filter, Class<? extends EntityComponent>... componentTypes ) {
        this(ed, new EntityCriteria().set(filter, componentTypes));
    }

    protected EntityContainer( EntityData ed, EntityCriteria criteria ) {
        this.ed = ed;
        this.criteria = criteria;
        this.parameter = findParameterType(getClass(), new HashMap<>());
        if( parameter == null ) {
            parameter = Object.class;
            log.warn("Element parameter type not found for:" + getClass() + "  Using Object.class.");
        }
    }

    @SuppressWarnings("unchecked")
    private static Class findParameterType( Class c, Map<String, Type> parameterMap ) {
        if( log.isTraceEnabled() ) {
            log.trace("findParameterType(" + c + ") parameters:" + Arrays.asList(c.getTypeParameters()));
        }
        for( Type t = c; t != null; ) {
            if( log.isTraceEnabled() ) {
                log.trace("  checking:" + t);
            }
            if( t instanceof ParameterizedType ) {
                ParameterizedType pt = (ParameterizedType)t;
                Class rawType = (Class)pt.getRawType();
                Type[] types = pt.getActualTypeArguments();
                TypeVariable<Class>[] vars = rawType.getTypeParameters();
                for( int i = 0; i < types.length; i++ ) {
                    if( log.isTraceEnabled() ) {
                        log.trace("    " + vars[i] + " = " + types[i] + "  class:" + types[i].getClass());
                    }
                    if( types[i] instanceof Class ) {
                        parameterMap.put(vars[i].getName(), types[i]);
                    }
                }
                if( pt.getRawType() == EntityContainer.class ) {
                    if( pt.getActualTypeArguments()[0] instanceof ParameterizedType ) {
                        return (Class)((ParameterizedType)pt.getActualTypeArguments()[0]).getRawType();
                    } else {
                        Type arg = pt.getActualTypeArguments()[0];
                        if( arg instanceof Class ) {
                            return (Class)arg;
                        }
                        if( arg instanceof TypeVariable ) {
                            Type result = parameterMap.get(((TypeVariable)arg).getName());
                            if( result != null ) {
                                return (Class)result;
                            }
                        }
                        // We don't know what to do with it
                        log.warn("Unhandled arg type:" + arg);
                    }
                }
            }
            // Else see if there is another generic superclass (probably not)
            if( t instanceof Class ) {
                if( log.isTraceEnabled() ) {
                    log.trace("    class type parameters:" + Arrays.asList(((Class)t).getTypeParameters()));
                }
                t = ((Class)t).getGenericSuperclass();
            } else {
                t = null;
            }
        }
        Class superClass = c.getSuperclass();
        if( superClass != null && superClass != Object.class ) {
            return findParameterType(superClass, parameterMap);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected void setFilter( ComponentFilter filter ) {

        // To match the original contract of this method, we need to completely
        // clear any existing filters and set the new one... but only if it
        // wasn't already set.  First check is easy but not straight-forward
        int count = 0;
        boolean found = false;
        for( ComponentFilter f : criteria.getFilters() ) {
            if( f == null ) {
                continue;
            }
            if( filter == f ) {
                found = true;
            }
            count++;
        }
        if( count == 1 && found ) {
            return;
        }

        criteria.clearFilters();
        criteria.setFilter(filter.getComponentType(), filter);
        if( entities != null ) {
            entities.resetEntityCriteria(criteria);
        }
    }

    protected void setCriteria( EntityCriteria criteria ) {
        // Good chance that if we were passed in our same criteria that we would
        // still need to reset.
        this.criteria = criteria;
        if( entities != null ) {
            entities.resetEntityCriteria(criteria);
        }
    }

    protected EntityCriteria getCriteria() {
        return criteria;
    }

    protected EntityData getEntityData() {
        return ed;
    }

    @SuppressWarnings("unchecked")
    protected void addComponentTypes( Class<? extends EntityComponent>... add ) {
        criteria.add(add);
    }

    public int size() {
        return entities.size();
    }

    public T getObject( EntityId id ) {
        return objects.get(id);
    }

    // BREAKING-CHANGE: raw component types array is no longer available and
    // would have to be recreated every time.  This probably negates any
    // callers benefits of using the direct-array version... but any
    // for-each style loop will still work with this change.
    //protected Class<? extends EntityComponent>[] getComponentTypes() {
    //    return componentTypes;
    //}
    protected Set<Class<? extends EntityComponent>> getComponentTypes() {
        return criteria.getComponentTypes();
    }

    @SuppressWarnings("unchecked")
    protected T[] getArray() {
        if( array != null ) {
            return array;
        }
        array = (T[])Array.newInstance(parameter, objects.size());
        array = objects.values().toArray(array);
        return array;
    }

    protected abstract T addObject( Entity e );

    protected abstract void updateObject( T object, Entity e );

    protected abstract void removeObject( T object, Entity e );

    protected void addObjects( Set<Entity> set ) {
        if( set.isEmpty() ) {
            return;
        }
        for( Entity e : set ) {
            T object = addObject(e);
            objects.put(e.getId(), object);
        }
        array = null;
    }

    protected void updateObjects( Set<Entity> set ) {
        if( set.isEmpty() ) {
            return;
        }
        for( Entity e : set ) {
            T object = objects.get(e.getId());
            if( object == null ) {
                log.warn("Update: No matching object for entity:" + e);
                continue;
            }
            updateObject(object, e);
        }
    }

    protected void removeObjects( Set<Entity> set ) {
        if( set.isEmpty() ) {
            return;
        }
        for( Entity e : set ) {
            T object = objects.remove(e.getId());
            if( object == null ) {
                log.warn("Remove: No matching object for entity:" + e);
                continue;
            }
            removeObject(object, e);
        }
        array = null;
    }

    public void start() {
        this.entities = ed.getEntities(criteria);
        entities.applyChanges();
        addObjects(entities);
    }

    public boolean update() {
        if( entities.applyChanges() ) {
            removeObjects(entities.getRemovedEntities());
            addObjects(entities.getAddedEntities());
            updateObjects(entities.getChangedEntities());
            return true;
        }
        return false;
    }

    public void stop() {
        if( entities == null ) {
            // We were never started or startup failed
            return;
        }
        removeObjects(entities);
        this.entities.release();
        this.entities = null;
    }

    public boolean isStarted() {
        return entities != null;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[components=" + criteria.getComponentTypes() + "]";
    }
}
