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

import com.google.common.base.Function;

import org.slf4j.*;

import com.jme3.bullet.*;
import com.jme3.bullet.collision.*;
import com.jme3.bullet.PhysicsSpace.BroadphaseType;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.math.*;
import com.jme3.util.SafeArrayList;

import com.simsilica.es.*;
import com.simsilica.sim.*;


/**
 *  Adapts a bullet physics space to an SiO2 GameSystem using Zay-ES 
 *  entities to represent physical game objects.
 *
 *  @author    Paul Speed
 */
public class BulletSystem extends AbstractGameSystem {

    static Logger log = LoggerFactory.getLogger(BulletSystem.class);

    private Thread baseThread;

    private PhysicsSpace pSpace;
    private CollisionDispatcher collisionDispatcher = new CollisionDispatcher();
    
    private EntityData ed;
    private CollisionShapes shapes;

    private BroadphaseType broadphaseType = BroadphaseType.DBVT;
    private Vector3f worldMin = new Vector3f(-10000f, -10000f, -10000f);
    private Vector3f worldMax = new Vector3f(10000f, 10000f, 10000f);
    private float speed = 1;

    private BodyContainer bodies;
    private GhostContainer ghosts;

    private EntitySet impulses;
 
    // Keeps track of just the bodies that are non-kinematic rigid bodies   
    private SafeArrayList<EntityPhysicsObject> mobs = new SafeArrayList<>(EntityPhysicsObject.class);

    // Keeps track of just the bodies that have control drivers
    private SafeArrayList<EntityRigidBody> driverBodies = new SafeArrayList<>(EntityRigidBody.class); 
    
    private SafeArrayList<PhysicsObjectListener> objectListeners = new SafeArrayList<>(PhysicsObjectListener.class); 

    private SafeArrayList<EntityCollisionListener> collisionListeners = new SafeArrayList<>(EntityCollisionListener.class);
    private CollisionFilter collisionFilter = new DefaultCollisionFilter();
    
    private ConcurrentLinkedQueue<ObjectSetup> pendingSetup = new ConcurrentLinkedQueue<>();

    public BulletSystem() {
    }

public PhysicsSpace getSpace() {
    return pSpace;
}

    /**
     *  Initializes an EntityPhysicsObjects using the specified function.  This
     *  is useful for two reasons: 1) it can be called before the object actually
     *  exists and will be called when the object shows up (beware leaks), 2) it
     *  will always be called on the same thread that the physics simulation is running
     *  on.
     */
    public void setupObject( EntityId objectId, Function<EntityPhysicsObject, ?> setup ) {
        pendingSetup.add(new ObjectSetup(objectId, setup));
    }     

    /**
     *  Sets the ControlDriver for a physics object.  This delegates to setupObject()
     *  so will succeed even if the entity's rigid body hasn't been created quite yet.
     */
    public void setControlDriver( EntityId objectId, final ControlDriver driver ) {
        setupObject(objectId, new Function<EntityPhysicsObject, Void>() {
                @Override
                public Void apply( EntityPhysicsObject object ) {
                    EntityRigidBody body = (EntityRigidBody)object;
                    body.setControlDriver(driver);
                    if( driver != null ) {
                        driverBodies.add(body);
                    } else {
                        driverBodies.remove(body);
                    }
                    return null;
                }  
            }); 
    }

    /**
     *  Adds a listener that will be notified about all physics objects changes.
     *  These listeners are called several times per frame and should be efficient and few.
     *  Note: this method is not thread safe and should only be called by the simulation
     *  thread once the game manager has been started. 
     */
    public void addPhysicsObjectListener( PhysicsObjectListener l ) {
        objectListeners.add(l);
    }

    public void removePhysicsObjectListener( PhysicsObjectListener l ) {
        objectListeners.remove(l);
    }

    /**
     *  Adds a collision listener that will be notified collisions between entity-backed physical
     *  objects and other physics bodies (whether entity-base dor not).
     *  Note: this method is not thread safe and should only be called by the simulation
     *  thread once the game manager has been started. 
     */
    public void addEntityCollisionListener( EntityCollisionListener l ) {
        collisionListeners.add(l);
    }

    public void removeEntityCollisionListener( EntityCollisionListener l ) {
        collisionListeners.remove(l);
    }

    /**
     *  Sets a filter that can cause collisions to be skipped before being passed
     *  to the collision listeners.
     */
    public void setCollisionFilter( CollisionFilter collisionFilter ) {
        this.collisionFilter = collisionFilter;
    }
    
    public CollisionFilter getCollisionFilter() {
        return collisionFilter;
    } 


    /**
     *  Sets the EntityData that this system will use to detect new
     *  physics entities.  Will be looked up in the GameSystemManager if
     *  not set.  Note: the EntityData must be set before the system has
     *  been initialized for it to take effect.  The method will throw
     *  an IllegalStateException if called after initialization.
     */
    public void setEntityData( EntityData ed ) {
        if( isInitialized() ) {
            throw new IllegalStateException("System is already initialized");
        }
        this.ed = ed;
    }
    
    public EntityData getEntityData() {
        return ed;
    }

    /**
     *  Sets the collision shapes registry that will be used to find 
     *  collision shapes for physics entities.  This will be looked up in the
     *  GameSystemManager if not set.  Note: the CollisionShapes registry must
     *  be set before the system has been initialized for it to take effect.
     *  The method will throw an IllegalStateException if called after initialization.
     */
    public void setCollisionShapes( CollisionShapes shapes ) {
        if( isInitialized() ) {
            throw new IllegalStateException("System is already initialized");
        }
        this.shapes = shapes;
    }
    
    public CollisionShapes getCollisionShapes() {
        return shapes;
    }     

    @Override
    protected void initialize() {
        if( ed == null ) {
            ed = getSystem(EntityData.class, true);
        }
        if( shapes == null ) {
            shapes = getSystem(CollisionShapes.class, true);
        }
 
        baseThread = Thread.currentThread();
        
        pSpace = new PhysicsSpace(worldMin, worldMax, broadphaseType);
        pSpace.addCollisionListener(collisionDispatcher);

        bodies = new BodyContainer(ed);
        ghosts = new GhostContainer(ed);
    }
    
    @Override
    protected void terminate() {        
        pSpace.destroy();
    }

    @Override
    public void start() {
        super.start();        
        bodies.start();
        ghosts.start();
        
        impulses = ed.getEntities(ShapeInfo.class, Mass.class, Impulse.class);        
    }

    @Override
    public void update( SimTime time ) {
    
        if( baseThread != Thread.currentThread() ) {
            throw new IllegalStateException("The bullet system must be updated from the same thread it was initialized."
                                            + " initialized from:" + baseThread + " updated from:" + Thread.currentThread());
        }
    
        super.update(time);
        
        startFrame(time);
         
        bodies.update();
        ghosts.update();

        // Run setup after we have the latest bodies and ghosts
        // We'll run it every time because when there are no pending setup
        // objects, it's essentially free... and when there are we don't 
        // know if the entity is ready or not yet.
        runPendingSetup();
                
        impulses.applyChanges();
        if( !impulses.isEmpty() ) {
            // We don't really care if the set changed or not, we will
            // always iterate over all items until they are removed.
            // The applyImpulses() method will clear the current impulse
            // if the body exists for the entity.
            applyImpulses(impulses);
        }       

        float t = (float)(time.getTpf() * speed);
        if( t != 0 ) {
        
            for( EntityRigidBody b : driverBodies.getArray() ) {
                b.getControlDriver().update(time, b);
            }
         
            pSpace.update(t);        
            pSpace.distributeEvents();
            
            for( EntityPhysicsObject o : mobs.getArray() ) {
                
                if( o instanceof EntityGhostObject ) {
                    // The only reason its in the mobs array is because
                    // is has a parent
                    EntityGhostObject g = (EntityGhostObject)o;
                    EntityRigidBody parent = g.getParent();
                    if( parent == null ) {
                        // May not have resolved yet
                        g.setParent(parent = bodies.getObject(g.getParentId()));                       
                    }
                    g.updateToParent();
                }
            
                objectUpdated(o);
                
                if( o instanceof EntityRigidBody ) {
                    ((EntityRigidBody)o).updateLastVelocity();
                }
            }
            
            // Distribute updates for bodies that have drivers but are not
            // normal mobs.
            for( EntityRigidBody b : driverBodies.getArray() ) {
                if( b.getMass() == 0 ) {
                    objectUpdated(b);
                }
            }
        }
 
        endFrame();        
    }

    @Override
    public void stop() {
        impulses.release();
        ghosts.stop();
        bodies.stop();
        super.stop();
    }
    
    protected void runPendingSetup() {
        ObjectSetup setup = null;
        while( (setup = pendingSetup.poll()) != null ) {
            if( !setup.execute() ) {
                // Add it back to the queue
                pendingSetup.add(setup);
                
                // If there is a lot of delay, this is horribly inefficient and it
                // would be better to peek and then remove... we hope that in general
                // the object exists already.
            }
        }
    }    

    protected void applyImpulses( Set<Entity> impulses ) {
        for( Entity e : impulses ) {
            EntityRigidBody body = bodies.getObject(e.getId());
            if( body == null ) {
                // Skipping... we may not have created it yet.
                // Note: we need to be careful not to leak objects by
                //       partially destroying them such that the impulse
                //       entities still exist but the physics entities don't.
                log.warn("Missing body for:" + e.getId());                
                continue; 
            }
            
            // Apply the impulse
            Impulse imp = e.get(Impulse.class);
            if( imp.getLinearVelocity() != null ) {
                body.getObject().setLinearVelocity(imp.getLinearVelocity());
            }
            if( imp.getAngularVelocity() != null ) {
                body.getObject().setAngularVelocity(imp.getAngularVelocity());
            }
            
            // Remove the impulse component so the entity drops out of this
            // set
            ed.removeComponent(e.getId(), Impulse.class); 
        }
    }
 
    private void startFrame( SimTime time ) {
        for( PhysicsObjectListener l : objectListeners.getArray() ) {
            l.startFrame(time);
        }
    }
    
    private void endFrame() {
        for( PhysicsObjectListener l : objectListeners.getArray() ) {
            l.endFrame();
        }
    }
    
    private void objectAdded( EntityPhysicsObject o ) {
        for( PhysicsObjectListener l : objectListeners.getArray() ) {
            l.added(o);
        }
    }
    
    private void objectUpdated( EntityPhysicsObject o ) {
        for( PhysicsObjectListener l : objectListeners.getArray() ) {
            l.updated(o);
        }
    }
    
    private void objectRemoved( EntityPhysicsObject o ) {
        for( PhysicsObjectListener l : objectListeners.getArray() ) {
            l.removed(o);
        }
    }

    private EntityPhysicsObject toEntityPhysicsObject( Object o ) {
        if( !(o instanceof EntityPhysicsObject) ) {
            return null;
        }
        return (EntityPhysicsObject)o;
    }
 
    private class BodyContainer extends EntityContainer<EntityRigidBody> {

        public BodyContainer( EntityData ed ) {
            super(ed, SpawnPosition.class, ShapeInfo.class, Mass.class);
        }
 
        @Override       
        public EntityRigidBody[] getArray() {
            return super.getArray();
        }
 
        @Override
        protected EntityRigidBody addObject( Entity e ) {

            Mass mass = e.get(Mass.class);
            CollisionShape shape = shapes.getShape(e.get(ShapeInfo.class));           
            EntityRigidBody result = new EntityRigidBody(e.getId(), shape, mass);

            // Update the physics location from the SpawnPosition
            SpawnPosition pos = e.get(SpawnPosition.class);
            result.setPhysicsLocation(pos.getLocation());
            result.setPhysicsRotation(pos.getOrientation());

            if( log.isTraceEnabled() ) {
                log.trace("pSpace.adding:" + result);
            } 
            pSpace.add(result);
 
            objectAdded(result);
            if( mass.getMass() > 0 ) {
                mobs.add(result);
            } else {
                // We will also need to send the update
                objectUpdated(result);
            }
            
            return result;
        }

        @Override
        protected void updateObject( EntityRigidBody object, Entity e ) {
            Mass mass = e.get(Mass.class);
            if( mass.getMass() == 0 ) {
                // See if it's the spawn position that has moved
                SpawnPosition pos = e.get(SpawnPosition.class);
                if( log.isTraceEnabled() ) {
                    log.trace("Moving " + object + "  to:" + pos);
                }            
                object.setPhysicsLocation(pos.getLocation());
                object.setPhysicsRotation(pos.getOrientation());
                objectUpdated(object);
            }            
        }
        
        @Override
        protected void removeObject( EntityRigidBody object, Entity e ) {
            if( log.isTraceEnabled() ) {
                log.trace("pSpace.removing:" + object);
            } 
            pSpace.remove(object);
            
            // Could be optimized to check if it's a mob first
            mobs.remove(object);
             
            objectRemoved(object);
        }    
    }

    private class GhostContainer extends EntityContainer<EntityGhostObject> {

        public GhostContainer( EntityData ed ) {
            super(ed, SpawnPosition.class, ShapeInfo.class, Ghost.class);
        }
 
        @Override       
        public EntityGhostObject[] getArray() {
            return super.getArray();
        }
 
        @Override
        protected EntityGhostObject addObject( Entity e ) {
            Ghost ghost = e.get(Ghost.class);
            CollisionShape shape = shapes.getShape(e.get(ShapeInfo.class));           
            EntityGhostObject result = new EntityGhostObject(e.getId(), shape, ghost.getCollisionMask());
            SpawnPosition pos = e.get(SpawnPosition.class);
            if( ghost.getParentEntity() != null ) {
                // See if the parent body is already created
                EntityRigidBody parent = bodies.getObject(ghost.getParentEntity());
                
                // Either way, setup the rest of the stuff for the parent
                result.setParent(ghost.getParentEntity(), parent, pos); 
            } else { 
                // Update the physics location from the SpawnPosition
                result.setPhysicsLocation(pos.getLocation());
                result.setPhysicsRotation(pos.getOrientation());
            }

            pSpace.add(result);
 
            objectAdded(result);
            if( ghost.getParentEntity() != null ) {
                // Then we update it like a mob
                mobs.add(result);
            } else {
                // We will also need to send the update since no more
                // will be coming.
                objectUpdated(result);
            }
            
            return result;
        }

        @Override
        protected void updateObject( EntityGhostObject object, Entity e ) {
            // Could allow offset adjustment through spawn position changes
        }
        
        @Override
        protected void removeObject( EntityGhostObject object, Entity e ) {
            pSpace.remove(object);
            
            // Could be optimized to check if it's a mob first
            mobs.remove(object);
             
            objectRemoved(object);
        }   
    }
    
    private class ObjectSetup {
        EntityId objectId;
        Function<EntityPhysicsObject, ?> function;
        int tries = 0;
        
        public ObjectSetup( EntityId objectId, Function<EntityPhysicsObject, ?> function ) {
            this.objectId = objectId;
            this.function = function;
        }
        
        public boolean execute() {
            EntityRigidBody body = bodies.getObject(objectId);
            if( body != null ) {
                function.apply(body);
                return true;
            }
            // Don't know what this would be used for but it's easy to support
            EntityGhostObject ghost = ghosts.getObject(objectId);
            if( ghost != null ) {
                function.apply(ghost);
                return true;
            }
            
            tries++;
            if( tries > 100 ) {
                log.warn("Object setup for:" + objectId + " exceeded 100 tries waiting for object.  Aborting setup.");
                return true;
            }
            return false;
        }
    }
    
    private class CollisionDispatcher implements PhysicsCollisionListener { 
        public void collision( PhysicsCollisionEvent event ) {
            EntityPhysicsObject a = toEntityPhysicsObject(event.getObjectA());
            EntityPhysicsObject b = toEntityPhysicsObject(event.getObjectB());
            if( a == null && b == null ) {
                // Nothing to deliver
                return;
            }
            
            if( collisionFilter != null && collisionFilter.filterCollision(a, b, event) ) {
                return;
            }
 
            /*log.info("A:" + event.getObjectA() 
                    + " B:" + event.getObjectB()
                    + "\n    type:" + event.getType() 
                    + " A wp:" + event.getPositionWorldOnA()
                    + " B wp:" + event.getPositionWorldOnB()
                    + "\n    B wn:" + event.getNormalWorldOnB()
                    + " dist:" + event.getDistance1());*/
            
            for( EntityCollisionListener l : collisionListeners ) {
                l.collision(a, b, event);
            }
            
            // Now deliver it to any control drivers if needed
            if( a.getControlDriver() != null ) {
                a.getControlDriver().addCollision(b, event);
            }
            if( b.getControlDriver() != null ) {
                b.getControlDriver().addCollision(a, event);
            }
        }
    }
    
}

