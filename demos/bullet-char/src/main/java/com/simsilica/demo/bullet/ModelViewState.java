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

package com.simsilica.demo.bullet;

import org.slf4j.*;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.scene.*;

import com.simsilica.es.*;


/**
 *
 *
 *  @author    Paul Speed
 */
public class ModelViewState extends BaseAppState {

    static Logger log = LoggerFactory.getLogger(ModelViewState.class);

    private EntityData ed;
    private ModelFactory factory;
 
    private Node modelRoot;   
    private ModelContainer models;

    public ModelViewState() {        
    }
 
    public Spatial getModel( EntityId id ) {
        ModelView view = models.getObject(id);        
        return view == null ? null : view.spatial; 
    }
    
    @Override
    protected void initialize( Application app ) {
        this.ed = getState(GameSystemsState.class).get(EntityData.class);
        
        if( factory == null ) {
            factory =  new SimpleModelFactory(ed, app.getAssetManager());
        }
 
        this.modelRoot = ((Main)app).getRootNode();       
        this.models = new ModelContainer(ed);
    }
    
    @Override
    protected void cleanup( Application app ) {
    }
    
    @Override
    protected void onEnable() {
        models.start();
    }
    
    @Override 
    public void update( float tpf ) {
        models.update();
    }
    
    @Override
    protected void onDisable() {
        models.stop();
    }
 
    protected Spatial getModel( ModelInfo info ) {
        return factory.apply(info);
    }
 
    private class ModelView {
        private Entity entity;
        private Spatial spatial;
 
        public ModelView( Entity entity ) {
System.out.println("New modelView:" + entity);        
            this.entity = entity;
            this.spatial = getModel(entity.get(ModelInfo.class));
            modelRoot.attachChild(spatial);
            update();
        }
        
        public void update() {
            Position pos = entity.get(Position.class);
            spatial.setLocalTranslation(pos.getLocation());
            spatial.setLocalRotation(pos.getOrientation());   
        }
        
        public void release() {
            spatial.removeFromParent();
        }
    }
    
    private class ModelContainer extends EntityContainer<ModelView> {
        public ModelContainer( EntityData ed ) {
            super(ed, Position.class, ModelInfo.class);
        }
 
        @Override       
        public ModelView[] getArray() {
            return super.getArray();
        }
 
        @Override
        protected ModelView addObject( Entity e ) {
            return new ModelView(e);
        }

        @Override
        protected void updateObject( ModelView object, Entity e ) {
            object.update();
        }
        
        @Override
        protected void removeObject( ModelView object, Entity e ) {
            object.release();
        }
    }
}
