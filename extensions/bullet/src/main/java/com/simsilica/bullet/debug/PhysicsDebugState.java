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

package com.simsilica.bullet.debug;

import java.util.*;

import org.slf4j.*;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.util.DebugShapeFactory;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.*;
import com.jme3.scene.shape.Box;
import com.jme3.scene.debug.Arrow;
import com.jme3.util.SafeArrayList;

import com.simsilica.es.*;
import com.simsilica.lemur.GuiGlobals;

import com.simsilica.bullet.CollisionShapes;
import com.simsilica.bullet.ShapeInfo;



/**
 *  Draws wireframe representations of the physics objects managed by the
 *  ES.  This does not show any objects directly injected into the physics
 *  space.  The application needs to add the DebugPhysicsListener to the
 *  BulletSystem for this state to show anything.
 *
 *  @author    Paul Speed
 */
public class PhysicsDebugState extends BaseAppState {

    static Logger log = LoggerFactory.getLogger(PhysicsDebugState.class);

    private EntityData ed;
    private CollisionShapes shapes;
    private PositionAdapter positionAdapter;
    
    private Map<CollisionShape, Spatial> debugShapeCache = new HashMap<>();    
 
    private Node debugRoot;   
    private BodyDebugContainer bodies;

    private ColorRGBA inactiveColor = new ColorRGBA(0, 0, 1, 0.5f);
    private Material inactive;
    
    private ColorRGBA activeColor = new ColorRGBA(1, 1, 0, 0.5f);
    private Material active;
        
    private ColorRGBA kinematicColor = new ColorRGBA(0.5f, 0.5f, 0.5f, 0.5f);
    private Material kinematic;

    private ColorRGBA ghostColor = new ColorRGBA(0.1f, 0.5f, 0.5f, 0.25f);
    private Material ghost;

    private SafeArrayList<BodyDebugView> views = new SafeArrayList<>(BodyDebugView.class);
    private float clipUpdateInterval = 1;
    private float clipUpdateTimer = 0;

    private float clipDistance = 50; //15;

    /**
     *  Creates a PhysicsDebugState that will look for physics entities in the
     *  specified EntityData using the specified PositionAdapter to figure out what
     *  component to so search for and how to apply position state.  The CollisionShapes 
     *  registry will be used to construct collisions shapes which in turn will be used to 
     *  render debug versions of those shapes.     
     */
    public PhysicsDebugState( EntityData ed, CollisionShapes shapes, PositionAdapter positionAdapter ) {
        setEnabled(false);
        this.ed = ed;
        this.shapes = shapes;
        this.positionAdapter = positionAdapter;
    }
 
    public void setClipDistance( float dist ) {
        if( clipDistance == dist ) {
            return;
        }
        this.clipDistance = dist;        
    }
    
    public float getClipDistance() {
        return clipDistance;
    }
    
    public void toggleEnabled() {
        setEnabled(!isEnabled());
    }
    
    @Override
    protected void initialize( Application app ) {
        
        this.inactive = GuiGlobals.getInstance().createMaterial(inactiveColor, false).getMaterial();
        inactive.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        inactive.getAdditionalRenderState().setWireframe(true);
        //inactive.getAdditionalRenderState().setDepthTest(false);
        this.active = GuiGlobals.getInstance().createMaterial(activeColor, false).getMaterial();
        active.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        active.getAdditionalRenderState().setWireframe(true);
        active.getAdditionalRenderState().setDepthTest(false);
        this.kinematic = GuiGlobals.getInstance().createMaterial(kinematicColor, false).getMaterial();
        //this.kinematic = new Material(app.getAssetManager(), "MatDefs/UnshadedFog.j3md");
        //kinematic.setColor("Color", kinematicColor);
        //kinematic.setFloat("MaxFogDistance", clipDistance);
        kinematic.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        kinematic.getAdditionalRenderState().setWireframe(true);
        //kinematic.getAdditionalRenderState().setDepthTest(true);

        this.ghost = GuiGlobals.getInstance().createMaterial(ghostColor, false).getMaterial();
        ghost.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        ghost.getAdditionalRenderState().setWireframe(true);
        
        this.debugRoot = new Node("debugRoot");
        this.bodies = new BodyDebugContainer(ed);
    }
    
    @Override
    protected void cleanup( Application app ) {
    }
    
    @Override
    protected void onEnable() {
        ((SimpleApplication)getApplication()).getRootNode().attachChild(debugRoot);       
        bodies.start();
    }        
    
    @Override 
    public void update( float tpf ) {
        bodies.update();
 
        clipUpdateTimer -= tpf;
        if( clipUpdateTimer < 0 ) {
            clipUpdateTimer = clipUpdateInterval;
            updateClipping();
        }       
    }
    
    @Override
    protected void onDisable() {
        bodies.stop();
        debugRoot.removeFromParent();
    }
 
    protected void updateClipping() {
        float threshold = clipDistance * 1.33f; //20;
        Vector3f pos = getApplication().getCamera().getLocation();
        for( BodyDebugView view : views.getArray() ) {
            float distance = view.spatial.getWorldBound().distanceToEdge(pos);
            if( distance > threshold ) {
                view.spatial.setCullHint(Spatial.CullHint.Always);
            } else {
                view.spatial.setCullHint(Spatial.CullHint.Inherit);
            }
        }
    }
 
    private Spatial getDebugShape( CollisionShape shape, boolean create ) {
        Spatial result = debugShapeCache.get(shape);
        if( result == null && create ) {
            result = DebugShapeFactory.getDebugShape(shape);
            debugShapeCache.put(shape, result); 
        }
        return result.clone();
    }     
     
    private class BodyDebugView {
        private Entity entity;
        private CollisionShape shape;
        private Spatial spatial;
        private int status = BodyDebugStatus.INACTIVE;
 
        public BodyDebugView( Entity entity ) {
            this.entity = entity;
            this.shape = shapes.getShape(entity.get(ShapeInfo.class));           
            this.spatial = getDebugShape(shape, true); 
            spatial.setMaterial(inactive);
            spatial.setQueueBucket(Bucket.Translucent);            
            
            debugRoot.attachChild(spatial);
            update();
            
            views.add(this);
        }
        
        public void update() {
            spatial.setLocalTranslation(positionAdapter.getLocation(entity));
            spatial.setLocalRotation(positionAdapter.getOrientation(entity)); 
            BodyDebugStatus status = entity.get(BodyDebugStatus.class);
            setStatus(status.getStatus());
        }
        
        protected void setStatus( int status ) {
            if( this.status == status ) {
                return;
            }
            this.status = status;
            switch( status ) {
                case BodyDebugStatus.STATIC:
                    spatial.setMaterial(kinematic);
                    spatial.setQueueBucket(Bucket.Transparent);
                    break;
                case BodyDebugStatus.ACTIVE:
                    spatial.setMaterial(active);
                    spatial.setQueueBucket(Bucket.Translucent);
                    break;
                case BodyDebugStatus.INACTIVE:               
                    spatial.setMaterial(inactive);
                    spatial.setQueueBucket(Bucket.Transparent);
                    break;
                case BodyDebugStatus.GHOST:
                    spatial.setMaterial(ghost);
                    spatial.setQueueBucket(Bucket.Transparent);
                    break;
            }  
        }
        
        public void release() {
            spatial.removeFromParent();
            views.remove(this);
        }
    }
    
    private class BodyDebugContainer extends EntityContainer<BodyDebugView> {
    
        public BodyDebugContainer( EntityData ed ) {
            super(ed, positionAdapter.getComponentType(), ShapeInfo.class, BodyDebugStatus.class);
        }
 
        @Override       
        public BodyDebugView[] getArray() {
            return super.getArray();
        }
 
        @Override
        protected BodyDebugView addObject( Entity e ) {
            return new BodyDebugView(e);
        }

        @Override
        protected void updateObject( BodyDebugView object, Entity e ) {
            object.update();
        }
        
        @Override
        protected void removeObject( BodyDebugView object, Entity e ) {
            object.release();
        }
    }
}
