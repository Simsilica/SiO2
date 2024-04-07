/*
 * $Id$
 *
 * Copyright (c) 2024, Simsilica, LLC
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

import com.simsilica.es.base.DefaultEntityData;

/**
 *
 *
 *  @author    Paul Speed
 */
public class EntityContainerTests {

    static class GenericParameterTest extends GroovyTestCase {
        public void testSimple() {
            def container = new SimpleEntityContainer(new DefaultEntityData());
            def array = container.getArray();
            assert array.getClass().getComponentType() == String.class;
        }

        public void testSubclassesParameterization() {
            def container = new UtilityUsingContainer(new DefaultEntityData());
            def array = container.getArray();
            assert array.getClass().getComponentType() == String.class;
        }
    }

    private static class SimpleEntityContainer extends EntityContainer<String> {
        public SimpleEntityContainer( EntityData ed ) {
            super(ed);
        }

        protected String addObject( Entity e ) {
            return "foo";
        }

        protected void updateObject( String object, Entity e ) {
        }

        protected void removeObject( String object, Entity e ) {
        }
    }

    private static abstract class UtilityContainer<S, T> extends EntityContainer<T> {
        protected UtilityContainer( EntityData ed, ComponentFilter filter, Class<? extends EntityComponent>... componentTypes ) {
            super(ed, filter, componentTypes);
        }
    }

    private static class UtilityUsingContainer extends UtilityContainer<Integer, String> {
        public UtilityUsingContainer( EntityData ed ) {
            super(ed, null);
        }

        protected String addObject( Entity e ) {
            return "foo";
        }

        protected void updateObject( String object, Entity e ) {
        }

        protected void removeObject( String object, Entity e ) {
        }
    }
}


