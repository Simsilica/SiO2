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

    private PhysicsSpace pSpace;
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

    private ConcurrentLinkedQueue<ObjectSetup> pendingSetup = new ConcurrentLinkedQueue<>();

    public BulletSystem() {
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
     */
    public void addPhysicsObjectListener( PhysicsObjectListener l ) {
        objectListeners.add(l);
    }

    public void removePhysicsObjectListener( PhysicsObjectListener l ) {
        objectListeners.remove(l);
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
        
        pSpace = new PhysicsSpace(worldMin, worldMax, broadphaseType);

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
        }
        
        @Override
        protected void removeObject( EntityRigidBody object, Entity e ) {
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
            EntityGhostObject result = new EntityGhostObject(e.getId(), shape);
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
}

/*

package sb.phys;

import java.util.*;
import java.util.concurrent.*;

import org.slf4j.*;

import com.jme3.bullet.*;
import com.jme3.bullet.PhysicsSpace.BroadphaseType;
import com.jme3.bullet.collision.*;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.objects.PhysicsGhostObject;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.PhysicsSpace.BroadphaseType;
import com.jme3.math.*;
import com.jme3.util.SafeArrayList;

import com.simsilica.es.*;
import com.simsilica.es.common.Decay;
import com.simsilica.sim.*;

import sb.es.*;

public class BulletSystem extends AbstractGameSystem {

    static Logger log = LoggerFactory.getLogger(BulletSystem.class);

    private PhysicsSpace pSpace;
    private EntityData ed;
    private CollisionShapes shapes;

    private Map<EntityId, ControlDriver> drivers = new ConcurrentHashMap<>();
    private List<EntityId> driverPending = new CopyOnWriteArrayList<>(); // not the fastest, but safe
    private ControlDriver nullDriver = new DefaultControlDriver();
    
    // A list we can easily iterate over to update drivers
    private SafeArrayList<Body> driverBodies = new SafeArrayList<>(Body.class);

    private BroadphaseType broadphaseType = BroadphaseType.DBVT;
    private Vector3f worldMin = new Vector3f(-10000f, -10000f, -10000f);
    private Vector3f worldMax = new Vector3f(10000f, 10000f, 10000f);
    private float speed = 1;
 
    private BodyContainer bodies;
    private SafeArrayList<AbstractBody> mobs = new SafeArrayList<>(AbstractBody.class);

    private GhostContainer ghosts;
    
    private EntitySet impulses;
    
    private Tick tick = new Tick();
    private CollisionDispatcher collisionDispatcher = new CollisionDispatcher();

    public BulletSystem() {
    }

    public PhysicsSpace getPhysicsSpace() {
        return pSpace;
    }

    public void setDriver( EntityId id, ControlDriver driver ) {
        drivers.put(id, driver == null ? nullDriver : driver);
        driverPending.add(id);
    }
 
    public ControlDriver getDriver( EntityId id ) {
        return drivers.get(id);
    }
    
    @Override
    protected void initialize() {
log.info("BulletSystem.initialize()");    
        pSpace = new PhysicsSpace(worldMin, worldMax, broadphaseType);
        pSpace.addTickListener(tick);
        pSpace.addCollisionListener(collisionDispatcher);
        
        ed = getSystem(EntityData.class);
        shapes = getSystem(CollisionShapes.class);
        
        bodies = new BodyContainer(ed);
        ghosts = new GhostContainer(ed);        
    }
    
    @Override
    protected void terminate() {
        pSpace.removeCollisionListener(collisionDispatcher);
        pSpace.removeTickListener(tick);
        pSpace.destroy();
    }

    @Override
    public void start() {
log.info("BulletSystem.start()");    
        super.start();
        bodies.start();
        ghosts.start();
        
        impulses = ed.getEntities(ShapeInfo.class, Mass.class, Impulse.class);
    }


    @Override
    public void update( SimTime time ) {
//log.info("-------------BulletSystem.update()");    
        super.update(time);

        bodies.update();
        ghosts.update();
        impulses.applyChanges();
        if( !impulses.isEmpty() ) {
            // We don't really care if the set changed or not, we will
            // always iterate over all items until they are removed.
            applyImpulses(impulses);
        }       
 
        if( !driverPending.isEmpty() ) {
            for( Iterator<EntityId> it = driverPending.iterator(); it.hasNext(); ) {
                EntityId id = it.next();
                Body b = bodies.getObject(id);
                if( b == null ) {
                    continue;
                }
                ControlDriver driver = drivers.get(id);
                b.setDriver(driver == nullDriver ? null : driver);
                driverPending.remove(id);
            }
        }
        
        for( Body b : driverBodies.getArray() ) {
            b.updateDriver(time);
        }

//log.info("----pSpace.update()");        
        pSpace.update((float)(time.getTpf() * speed));        
        //pSpace.update(active ? tpf * speed : 0);
//log.info("----distributeEvents()");        
        pSpace.distributeEvents();
 
//log.info("----publish");        
        for( AbstractBody b : mobs ) {
            b.publish();
        }        
    }

    @Override
    public void stop() {
        impulses.release();
        bodies.stop();
        ghosts.stop();       
        super.stop();
    }    
 
    protected void applyImpulses( Set<Entity> impulses ) {
        for( Entity e : impulses ) {
            Body body = bodies.getObject(e.getId());
            if( body == null ) {
                // Skipping... we may not have created it yet.
                // Note: we need to be careful not to leak objects by
                //       partially destroying them such that the impulse
                //       entities still exist but the physics entities don't.
                log.warn("Missing body for:" + e.getId());                
                continue; 
            }
            
            // Apply the impulse
            body.applyImpulse(e.get(Impulse.class));
            
            // Remove the impulse component so the entity drops out of this
            // set
            ed.removeComponent(e.getId(), Impulse.class); 
        }
    }
    
    private EntityBody toEntityBody( PhysicsCollisionObject b ) {
        if( b instanceof EntityBody ) {
            return (EntityBody)b;
        }
        return null;
    } 
    
    private EntityId toEntityId( PhysicsCollisionObject b ) {
        if( b instanceof EntityRigidBody ) {
            return ((EntityRigidBody)b).body.entity.getId();
        }
        if( b instanceof EntityGhostObject ) {
            return ((EntityGhostObject)b).body.entity.getId();
        }
        return null;
    }

    private int getCollisionFlags( PhysicsCollisionObject b ) {
        if( b instanceof EntityRigidBody ) {
            return CollisionFlags.COLLIDE_ALL;
        }
        if( b instanceof EntityGhostObject ) {
            return ((EntityGhostObject)b).body.collisionFlags;
        }
        return CollisionFlags.COLLIDE_ALL;
    }

    private int getColliderType( PhysicsCollisionObject b ) {
        if( b instanceof EntityRigidBody ) {
            return ((EntityRigidBody)b).body.colliderType;
        }
        if( b instanceof EntityGhostObject ) {
            return ((EntityGhostObject)b).body.colliderType;
        }
        return 0;
    }
    
    private class Tick implements PhysicsTickListener {
        public void prePhysicsTick( PhysicsSpace space, float f ) {
            //log.info("prePhysicsTick()");
        }

        public void physicsTick( PhysicsSpace space, float f ) {
            //log.info("physicsTick()");
        }
    }
 
    private class CollisionDispatcher implements PhysicsCollisionListener { 
        public void collision( PhysicsCollisionEvent event ) {
//            log.info("A:" + event.getObjectA() 
//                    + " B:" + event.getObjectB()
//                    + "\n    type:" + event.getType() 
//                    + " A wp:" + event.getPositionWorldOnA()
//                    + " B wp:" + event.getPositionWorldOnB()
//                    + "\n    B wn:" + event.getNormalWorldOnB()
//                    + " dist:" + event.getDistance1()
//                    + "\n    impulse:" + event.getAppliedImpulse()
//                    + " friction:" + event.getCombinedFriction()
//                    + " restitution:" + event.getCombinedRestitution());
            
            EntityBody body1 = toEntityBody(event.getObjectA());
            EntityBody body2 = toEntityBody(event.getObjectB()); 
            // If neither are entity bodies then we don't care about publishing this contact.
            if( body1 == null && body2 == null ) {
                return;
            }
            
            EntityId id1 = body1.getId(); //toEntityId(event.getObjectA()); 
            EntityId id2 = body2.getId(); //toEntityId(event.getObjectB());
 
            // If both IDs are null then this is not a contact we care about
            //if( id1 == null && id2 == null ) {
            //    return;
            //}
 
            // If there are two ends and one of them is a ghost object
            // then see if we publish
            if( body1 != null && body2 != null ) {
            
                // If one of the parents it the other entity then we don't
                // care about this contact
                if( Objects.equals(body1.getParentId(), id2) 
                    || Objects.equals(body2.getParentId(), id1) ) {
                    return;
                }  
            
                int flags = body1.getCollisionFlags();
                if( flags < CollisionFlags.COLLIDE_ALL ) {
                    // It's a ghost object and doesn't collide with everything
                    int type = body2.getColliderType();
//System.out.println("type:" + Integer.toBinaryString(type) + "  flags:" + Integer.toBinaryString(flags));                    
                    if( (type & flags) == 0 ) {
//System.out.println("Skipping publishing:" + id1 + " -> " + id2 + " contact");                    
                        return;
                    }
                }                     
                flags = body2.getCollisionFlags(); 
                if( flags < CollisionFlags.COLLIDE_ALL ) {
                    int type = body1.getColliderType();
//System.out.println("type:" + Integer.toBinaryString(type) + "  flags:" + Integer.toBinaryString(flags));                    
                    if( (type & flags) == 0 ) {
//System.out.println("Skipping publishing:" + id1 + " -> " + id2 + " contact");                    
                        return;
                    }                     
                } 
            }
            
//            if( event.isLateralFrictionInitialized() ) { 
//log.info(id1 + " -> " + id2
//        + "  initialized:" + event.isLateralFrictionInitialized()  
//        + "  latFric1:" + event.getLateralFrictionDir1() 
//        + "  latFric2:" + event.getLateralFrictionDir2()
//        + "  imp1:" + event.getAppliedImpulseLateral1()    
//        + "  imp2:" + event.getAppliedImpulseLateral2());
//            } else {
//log.info("No lateral friction:" + id1 + " -> " + id2);            
//            }
            
            Vector3f wp = event.getPositionWorldOnB().clone(); 
            Vector3f normal = event.getNormalWorldOnB().clone();
            float lateralImpulse = 0;
            Vector3f lateralFriction = null;
            
            // We want id2 to always be the null one if there is one so
            // see if need to swap. (Not sure bullet will ever give them to us like that)
            if( id1 == null ) {
                id1 = id2;
                body1 = body2;
                wp.set(event.getPositionWorldOnA());
                normal.negateLocal();
                if( event.isLateralFrictionInitialized() ) {
                    lateralImpulse = event.getAppliedImpulseLateral2(); 
                    lateralFriction = event.getLateralFrictionDir2().clone();
                }  
            } else if( event.isLateralFrictionInitialized() ) {
                lateralImpulse = event.getAppliedImpulseLateral1();
                lateralFriction = event.getLateralFrictionDir1().clone();  
            }
 
            // Create a contact entity           
            Contact c = new Contact(id1, body1 instanceof EntityGhostObject, 
                                    id2, body2 instanceof EntityGhostObject,
                                    wp, normal, event.getAppliedImpulse(), lateralImpulse,
                                    event.getCombinedFriction(), lateralFriction, 
                                    event.getCombinedRestitution());

            // I think impulse is always positive?            
            //if( c.getImpulse() > 25 ) {
            //    System.out.println("*** impact impulse:" + c.getImpulse());
            //}
                                    
            EntityId contactEntity = ed.createEntity();
            ed.setComponents(contactEntity, c, new Decay(0, 0)); // live one frame
            ////log.info(contactEntity + " -> contact:" + c);
 
            // See if we should also create an impact component
            // We may decide to fold these together in the future
            if( body1 != null && body2 != null && !(body1 instanceof EntityGhostObject) && !(body2 instanceof EntityGhostObject) ) {
//System.out.println("contact:" + c);
                Vector3f v1 = body1.getLastVelocity();
                Vector3f v2 = body2.getLastVelocity();
                //Vector3f relative = p2.subtract(p1);
                float dot1 = -normal.dot(v1);
                float dot2 = normal.dot(v2);
                float energy = dot2 + dot1;
                if( Math.abs(energy) > 1 ) { //0.5 ) {
                    energy = Math.max(0, energy);
 
                    Impact imp = new Impact(id1, body1.getMassType(),
                                            id2, body2.getMassType(),
                                            wp, normal, energy);
                    ed.setComponents(contactEntity, imp);

log.info("added impact:" + imp);                    
                    
                    //System.out.println("velocity dot1:" + dot1 + "  dot2:" + dot2 + "   energy:" + energy);
                    //System.out.println("   entity1:" + id1 + "  entity2:" + id2); 
                    //System.out.println("   normal:" + normal);
                    //System.out.println("   v1:" + v1 + " len:" + v1.length() + "  v2:" + v2 + " len:" + v2.length());
                    //System.out.println("   lastV1:" + body1.getLastVelocity() + "  lastV2:" + body2.getLastVelocity());
                    
                }
            }
 
 
            if( body1 != null ) {
                body1.getBody().addContact(c);
            }           
            if( body2 != null ) {
                body2.getBody().addContact(c);
            }
        }
    }
 
    private interface EntityBody {
        public EntityId getId();
    
        public EntityId getParentId();
    
        public AbstractBody getBody();

        public int getCollisionFlags();
        
        public int getColliderType();

        public int getMassType(); 
        
        public Vector3f getLinearVelocity();

        public Vector3f getLastVelocity();
        
        public Vector3f getPhysicsLocation(Vector3f target);
    }
 
    private class EntityRigidBody extends PhysicsRigidBody implements EntityBody {
        private Body body;
        private Vector3f velocity = new Vector3f();
        private Vector3f lastVelocity = new Vector3f();
        
        public EntityRigidBody( Body body, CollisionShape shape, float mass ) {
            super(shape, mass);
            this.body = body;
        }
 
        public EntityId getId() {
            return body.entity.getId();
        }
 
        public EntityId getParentId() {
            return null;
        }
        
        public int getCollisionFlags() {
            return CollisionFlags.COLLIDE_ALL;
        }
        
        public int getColliderType() {
            return body.colliderType;
        }
 
        public AbstractBody getBody() {
            return body;
        }
 
        public void refreshVelocity() {
            lastVelocity.set(getLinearVelocity());
            //velocity.set(getLinearVelocity());
            //if( body.entity.getId().getId() >= 172 ) {
            //    System.out.println("velocity:" + velocity + "  lastVelocity:" + lastVelocity);
            //}
        }
        
        public Vector3f getLastVelocity() {
            return lastVelocity;
        }        
        
        @Override
        public int getMassType() {
            return body.getMassType();
        }
        
        public String toString() {
            return "EntityRigidBody[" + body.entity.getId() + "]";
        }
    }
 
    private class EntityGhostObject extends PhysicsGhostObject implements EntityBody {
        private GhostBody body;
        
        public EntityGhostObject( GhostBody body, CollisionShape shape ) {
            super(shape);
            this.body = body;
        }
 
        public EntityId getId() {
            return body.entity.getId();
        }

        public EntityId getParentId() {
            return body.parentEntity;
        }
        
        public int getCollisionFlags() {
            return body.collisionFlags;
        }
        
        public int getColliderType() {
            return CollisionFlags.COLLIDE_GHOST;
        }
        
        public AbstractBody getBody() {
            return body;
        }
        
        @Override
        public int getMassType() {
            return Mass.TYPE_INTANGIBLE;
        }
        
        public Vector3f getLinearVelocity() {
            return Vector3f.ZERO;
        }
        
        public Vector3f getLastVelocity() {
            return Vector3f.ZERO;
        }
        
        public String toString() {
            return "EntityGhostObject[" + body.entity.getId() + "]";
        }
    }
 
    private abstract class AbstractBody {
 
        public abstract void publish();
        
        public abstract ControlDriver getDriver();
        
        public abstract void addContact( Contact c );
        
        public abstract int getMassType();
    }
 
    private class Body extends AbstractBody {
        Entity entity;
        EntityRigidBody rigidBody;
        Vector3f vTemp = new Vector3f();
        Quaternion qTemp = new Quaternion();
        boolean kinematic;
        int lastStatus = -1;
        ControlDriver driver;
        int colliderType;
        int massType;
        
        public Body( Entity entity ) {
            this.entity = entity;
            Mass mass = entity.get(Mass.class);
            CollisionShape shape = shapes.loadShape(entity.get(ShapeInfo.class));

            this.rigidBody = new EntityRigidBody(this, shape, mass.getMass());
//log.info("entity:" + entity.getId() + " body:" + rigidBody);        
            
            if( mass.getMass() != 0 ) {
                mobs.add(this);
                this.colliderType = CollisionFlags.COLLIDE_MOBILE;
            } else {
                this.kinematic = true;
                this.colliderType = CollisionFlags.COLLIDE_KINEMATIC;
            }
            this.massType = mass.getType();
 
            // Need to set the body's initial position/orientation
            update();            
            
            pSpace.add(rigidBody);
            
            // Need a better way to set variable gravity but for now this
            // will work for today
            if( massType == Mass.TYPE_PLASMA ) {
                // Less affected by gravity
                rigidBody.setGravity(rigidBody.getGravity().mult(0.1f));
            }            
        }

        @Override
        public int getMassType() {
            return massType;
        }
 
        public void setDriver( ControlDriver driver ) {
            this.driver = driver;
            if( driver != null ) {
                driver.initialize(entity.getId(), rigidBody);
                driverBodies.add(this);
            } else {
                driverBodies.remove(this);
            }            
        }
 
        @Override
        public ControlDriver getDriver() {
            return driver;
        }
 
        public void addContact( Contact c ) {            
            if( driver != null ) {
                driver.addContact(c, rigidBody);
            }                                    
        }
 
        public void updateDriver( SimTime time ) {
            driver.update(time, rigidBody);   
        }
 
        private int calculateStatus() {
            if( kinematic ) {
                return BodyDebugStatus.STATIC; 
            }
            return rigidBody.isActive() ? BodyDebugStatus.ACTIVE : BodyDebugStatus.INACTIVE;
        }
 
        **
         *  Publishes the current state back to the ES.
         *
        @Override
        public void publish() {
        
            Position current = ed.getComponent(entity.getId(), Position.class);
 
            rigidBody.getPhysicsLocation(vTemp);
            rigidBody.getPhysicsRotation(qTemp);
            rigidBody.refreshVelocity();
 
            int newStatus = calculateStatus();
            if( newStatus != lastStatus ) {
                lastStatus = newStatus;
                ed.setComponent(entity.getId(), new BodyDebugStatus(lastStatus));
            }
            
            Position next = current != null ? current.change(vTemp, qTemp) : new Position(vTemp.clone(), qTemp.clone());
            if( current != next ) {
                ed.setComponent(entity.getId(), next);                
            }
        }
        
        public void update() { 
//System.out.println("Updating position for:" + entity.getId());            
            // Update the physics location from the SpawnPosition
            Position pos = entity.get(SpawnPosition.class).getPosition();
            rigidBody.setPhysicsLocation(pos.getLocation());
            rigidBody.setPhysicsRotation(pos.getOrientation());
                        
            if( kinematic ) {
                publish();
            }
        }
 
        public void applyImpulse( Impulse imp ) {
            if( imp.getLinearVelocity() != null ) {
//System.out.println("Applying linear velocity:" + imp.getLinearVelocity());             
                rigidBody.setLinearVelocity(imp.getLinearVelocity());
            }
            if( imp.getAngularVelocity() != null ) {
//System.out.println("Applying angular velocity:" + imp.getAngularVelocity());             
                rigidBody.setAngularVelocity(imp.getAngularVelocity());
            }
        }
        
        public void release() {
            pSpace.remove(rigidBody);
        }
    }
    
    private class BodyContainer extends EntityContainer<Body> {

        public BodyContainer( EntityData ed ) {
            super(ed, SpawnPosition.class, ShapeInfo.class, Mass.class);
        }
 
        @Override       
        public Body[] getArray() {
            return super.getArray();
        }
 
        @Override
        protected Body addObject( Entity e ) {
            return new Body(e);
        }

        @Override
        protected void updateObject( Body object, Entity e ) {
            object.update();
        }
        
        @Override
        protected void removeObject( Body object, Entity e ) {
            object.release();
        }
    
    }

    private class GhostBody extends AbstractBody {
        Entity entity;
        PhysicsGhostObject ghostObject;
        Vector3f vTemp = new Vector3f();
        Quaternion qTemp = new Quaternion();
        int colliderType = CollisionFlags.COLLIDE_GHOST;
        int collisionFlags;
        Ghost ghost;
        EntityId parentEntity;        
        PhysicsRigidBody parent;
        Body parentBody;
        Vector3f relativePosition;
        Quaternion relativeRotation;
        
        public GhostBody( Entity entity ) {
            this.entity = entity;
            CollisionShape shape = shapes.loadShape(entity.get(ShapeInfo.class));

            this.ghostObject = new EntityGhostObject(this, shape);
            this.ghost = entity.get(Ghost.class); 
            this.collisionFlags = ghost.getCollisionFlags();
//log.info("entity:" + entity.getId() + " ghost:" + ghostObject);        
 
            // Need to set the body's initial position/orientation
            update();            
            
            pSpace.add(ghostObject);            
        }
 
        @Override
        public int getMassType() {
            return Mass.TYPE_INTANGIBLE;
        }
        
        @Override
        public ControlDriver getDriver() {
            return parentBody == null ? null : parentBody.getDriver();
        }
        
        public void addContact( Contact c ) {            
            if( parentBody != null ) {
                parentBody.addContact(c);
            }                                    
        }
 
        protected boolean updateToParent() {
            if( parent == null ) {
                this.parentBody = bodies.getObject(parentEntity);
                if( parentBody == null ) {
                    return false; // no parent yet
                }
                this.parent = parentBody.rigidBody; 
            }
 
            parent.getPhysicsLocation(vTemp);
            parent.getPhysicsRotation(qTemp);
            
            ghostObject.setPhysicsLocation(vTemp.add(qTemp.mult(relativePosition)));
            ghostObject.setPhysicsRotation(qTemp.mult(relativeRotation));
 
            return true;           
        }
 
        **
         *  Publishes the current state back to the ES.
         *
        public void publish() {
 
            if( parentEntity != null ) {
                if( !updateToParent() ) {
                    return;
                } 
            }
        
            Position current = ed.getComponent(entity.getId(), Position.class);
 
            ghostObject.getPhysicsLocation(vTemp);
            ghostObject.getPhysicsRotation(qTemp);
 
            ed.setComponent(entity.getId(), new BodyDebugStatus(BodyDebugStatus.GHOST));
            
            Position next = current != null ? current.change(vTemp, qTemp) : new Position(vTemp.clone(), qTemp.clone());
            if( current != next ) {
                ed.setComponent(entity.getId(), next);                
            }
        }
        
        public void update() { 
//System.out.println("Updating ghost position for:" + entity.getId());            
            if( ghost.getParentEntity() != null ) {
                this.parentEntity = ghost.getParentEntity();
                Position pos = entity.get(SpawnPosition.class).getPosition();
                this.relativePosition = pos.getLocation();
                this.relativeRotation = pos.getOrientation();
                mobs.add(this);  
            } else {            
                // Update the physics location from the SpawnPosition
                Position pos = entity.get(SpawnPosition.class).getPosition();
                ghostObject.setPhysicsLocation(pos.getLocation());
                ghostObject.setPhysicsRotation(pos.getOrientation());
                mobs.remove(this);
            }                        
            publish();
        }
 
        public void release() {
            pSpace.remove(ghostObject);
        }
    }
    
    private class GhostContainer extends EntityContainer<GhostBody> {

        public GhostContainer( EntityData ed ) {
            super(ed, SpawnPosition.class, ShapeInfo.class, Ghost.class);
        }
 
        @Override       
        public GhostBody[] getArray() {
            return super.getArray();
        }
 
        @Override
        protected GhostBody addObject( Entity e ) {
            return new GhostBody(e);
        }

        @Override
        protected void updateObject( GhostBody object, Entity e ) {
            object.update();
        }
        
        @Override
        protected void removeObject( GhostBody object, Entity e ) {
            object.release();
        }
    
    }
    
}
*/
