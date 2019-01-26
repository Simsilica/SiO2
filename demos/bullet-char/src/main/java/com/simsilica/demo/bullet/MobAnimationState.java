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

import java.util.*;

import org.slf4j.*;

import com.jme3.anim.*;
import com.jme3.anim.tween.action.Action;
import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.util.SafeArrayList;

import com.simsilica.es.*;
import com.simsilica.mathd.filter.*;


/**
 *
 *
 *  @author    Paul Speed
 */
public class MobAnimationState extends BaseAppState {

    static Logger log = LoggerFactory.getLogger(MobAnimationState.class);

    private EntityData ed;
    private ModelViewState models;
    
    // Keep track of the mob animation objects we've already created 
    private Map<EntityId, MobAnimation> mobIndex = new HashMap<>();
    
    // Keep a list of the mob animation objects for convenient per-frame updates.
    private SafeArrayList<MobAnimation> mobs = new SafeArrayList<>(MobAnimation.class);
    
    private MobilityContainer mobStates;
    private ActionContainer mobActions;

    public MobAnimationState() {        
    }
    
    @Override
    protected void initialize( Application app ) {
        this.ed = getState(GameSystemsState.class).get(EntityData.class);
        
        this.models = getState(ModelViewState.class);
        
        this.mobStates = new MobilityContainer(ed);
        this.mobActions = new ActionContainer(ed);
    }
    
    @Override
    protected void cleanup( Application app ) {
    }
    
    @Override
    protected void onEnable() {
        mobStates.start();
        mobActions.start();
    }
    
    @Override 
    public void update( float tpf ) {
        mobStates.update();
        mobActions.update();
        
        for( MobAnimation mob : mobs.getArray() ) {
            mob.update(tpf);
        }
    }
    
    @Override
    protected void onDisable() {
        mobActions.stop();
        mobStates.stop();
    }
 
    protected MobAnimation getMobAnimation( EntityId id ) {
        return getMobAnimation(id, true);
    }
    
    protected MobAnimation getMobAnimation( EntityId id, boolean create ) {
        MobAnimation result = mobIndex.get(id);
        if( result != null || !create ) {
            return result;
        }
        result = new MobAnimation(id);
        mobIndex.put(id, result);
        mobs.add(result);        
        return result;
    }
 
    protected void removeMobAnimation( EntityId id ) {
        MobAnimation mob = mobIndex.remove(id);
        mobs.remove(mob);
    }
 
    protected void addMobility( EntityId parent, Mobility mobility ) {
        getMobAnimation(parent).addMobility(mobility);
    }
    
    protected void removeMobility( EntityId parent, Mobility mobility ) {
        getMobAnimation(parent).removeMobility(mobility);
    } 

    protected void addAction( EntityId parent, CharacterAction action ) {
        getMobAnimation(parent).addAction(action.getCharacterActionName(ed));
    }
    
    protected void removeAction( EntityId parent, CharacterAction action ) {
        getMobAnimation(parent).removeAction(action.getCharacterActionName(ed));
    }
    
    protected Spatial findAnimRoot( Spatial s ) {
        if( s.getControl(AnimComposer.class) != null ) {
            return s;
        }
        if( s instanceof Node ) {
            for( Spatial child : ((Node)s).getChildren() ) {
                Spatial result = findAnimRoot(child);
                if( result != null ) {
                    return result;
                }
            }
        }
        return null;
    } 
 
    /**
     *  Manages the animation state for a specific MOB.
     */
    private class MobAnimation {
        private EntityId id;
        private Set<Mobility> mobility = new HashSet<>();
        private Mobility primary;
        private String currentBase;
        private String action; // only one at a time right now
 
        private Spatial model;       
        private Spatial animRoot;
        private AnimComposer animComposer;
        private Action animAction;
        private double lastSpeed;
        
        private Vector3f lastLocation = new Vector3f();
        private Quaternion lastRotation = new Quaternion();
        private Vector3f velocity = new Vector3f();
        
        private CharacterGround ground;
         
        
        public MobAnimation( EntityId id ) {
            this.id = id;
        }

        protected Spatial getAnimRoot() {
            if( animRoot == null ) {
                this.model = models.getModel(id);
System.out.println("model:" + model);                
                if( model == null ) {
                    return null;  // have to wait until later I guess.
                }
                
                // Find spatial with the composer
                // For the moment, we'll guess
                animRoot = ((Node)model).getChild("Root");
                if( animRoot == null ) {
                    // Have to find it the hard way
                    animRoot = findAnimRoot(model);
                }
System.out.println("animRoot:" + animRoot);
                
                lastLocation.set(model.getWorldTranslation());
                lastRotation.set(model.getWorldRotation());
            }
            return animRoot;
        }

        protected AnimComposer getAnimComposer() {
            if( animComposer == null ) {
                Spatial s = getAnimRoot();
                animComposer = s == null ? null : s.getControl(AnimComposer.class); 
            }
            return animComposer;
        }
        
        public void addMobility( Mobility m ) {
            mobility.add(m);
            
            // Just override whatever is there for now... I don't remember why I allow
            // multiple mobilities but when we manage how we pick which one is active
            // or layered at any given time then we can also use its base speed, etc.
            primary = m;
        }
        
        public void removeMobility( Mobility m ) {
            mobility.remove(m);
            if( mobility.isEmpty() ) {
                // Remove ourselves from being managed... there is no mobility component set anymore
                removeMobAnimation(id);
            }
        }
        
        public void addAction( String a ) {
            if( Objects.equals(a, this.action) ) {
                return;
            }
            this.action = a;
            
            // Set the animation on the character
        }
        
        public void removeAction( String a ) {
            if( !Objects.equals(a, this.action) ) {
                return;
            }
            this.action = null;
            
            // Stop the animation on the character
        }
 
        protected void setBaseAnimation( String a, double speed ) {
//System.out.println("setBaseAnimation(" + a + ", " + speed + ")");        
            AnimComposer ac = getAnimComposer();
            if( ac == null ) {
                return;
            }
            if( Objects.equals(a, currentBase) ) {
                if( animAction != null ) {
                    speed = Math.round(speed * 10) / 10.0;                
                    if( speed != lastSpeed ) {
                        animAction.setSpeed(speed);
                        lastSpeed = speed;
                    }
                }
                return;
            }
            this.currentBase = a;
            this.animAction = ac.setCurrentAction(currentBase);
            animAction.setSpeed(speed);
        }
 
        private Filterd lowPass = new SimpleMovingMean(10); // 1/6th second of data        
        
        public void update( float tpf ) {
            Spatial s = getAnimRoot();
            if( s == null ) {
                return; // nothing to do
            }
            
            // Right now since we can't layer... an action will override
            // any mobility
            if( action != null ) {
                setBaseAnimation(action, 1);
                return;
            }
                       
            // See what kind of movement is happening
            velocity.set(model.getWorldTranslation()).subtractLocal(lastLocation);
            
            // We don't account for up/down right now
            velocity.y = 0;
            
            // Track the current values for next time
            lastLocation.set(model.getWorldTranslation());
            lastRotation.set(model.getWorldRotation());

            float rawSpeed = velocity.length();
            
            // Because we aren't interpolating over known good physics frames
            // like we would in a network app, we have the possibility of seeing
            // the same position in two visual frames even if the object is moving.
            // For example:
            // JME thread           Game Systems Thread
            // update               update
            //
            //                      update
            // update
            //
            //
            // update
            //                      pudate
            //
            // That would look like the object is standing still as a tiny single
            // frame blip.  We have filtering code to remove these blips but it's
            // bypassed if we 'early out' for rawSpeed = 0.
            //if( rawSpeed > 0.001 ) {
                float speed = (tpf > 0 && rawSpeed > 0) ? velocity.length() / tpf : 0;
                
                // Just a simple heuristic for now
                //if( speed > 0.01 ) {            
                    float forward = lastRotation.mult(Vector3f.UNIT_Z).dot(velocity);
                    forward = tpf > 0 ? forward / tpf : 0;
                    float left = lastRotation.mult(Vector3f.UNIT_X).dot(velocity);
                    left = tpf > 0 ? left / tpf : 0;
                    
                    double normalWalkSpeed = primary.getBaseSpeed();
                    double animSpeed = forward / normalWalkSpeed;
                    
                    // Pass the anim speed through a low-pass filter using
                    // a moving average
                    lowPass.addValue(animSpeed);
                    animSpeed = lowPass.getFilteredValue();
//System.out.println("forward:" + forward + "  left:" + left + "  animSpeed:" + animSpeed);
                     
                    if( Math.abs(animSpeed) > 0.01 ) {
                        setBaseAnimation("Walk", animSpeed);
                        return;
                    }
                //} 
            //} else {
//System.out.println("rawSpeed:" + rawSpeed);            
//            }
            
            // If nothing else set an animation then go back to idle
            setBaseAnimation("Idle", 1);
        }
    }
    
    /**
     *  Just need to keep track of the parent and value relationship
     *  so that we can properly remove the value when the tagging entity
     *  goes away.  Generally it's fields have been cleared so we don't
     *  have them anymore.
     */   
    private class ParentedComponent<T> {
        EntityId parentId;
        T value;
        
        public ParentedComponent( EntityId parentId, T value ) {
            this.parentId = parentId;
            this.value = value;
        }
    }
    
    @SuppressWarnings("unchecked")
    private class MobilityContainer extends EntityContainer<ParentedComponent<Mobility>> {
        public MobilityContainer( EntityData ed ) {
            super(ed, Mobility.class, Parent.class);
        }
 
        @Override
        protected ParentedComponent<Mobility> addObject( Entity e ) {
            Parent p = e.get(Parent.class);
            Mobility m = e.get(Mobility.class);
            addMobility(p.getParentId(), m);
            return new ParentedComponent<>(p.getParentId(), m);
        }

        @Override
        protected void updateObject( ParentedComponent<Mobility> object, Entity e ) {
        }
        
        @Override
        protected void removeObject( ParentedComponent<Mobility> object, Entity e ) {
            removeMobility(object.parentId, object.value);
        }
    }
    
    @SuppressWarnings("unchecked")
    private class ActionContainer extends EntityContainer<ParentedComponent<CharacterAction>> {
        public ActionContainer( EntityData ed ) {
            super(ed, CharacterAction.class, Parent.class);
        }
 
        @Override
        protected ParentedComponent<CharacterAction> addObject( Entity e ) {
            Parent p = e.get(Parent.class);
            CharacterAction a = e.get(CharacterAction.class);
            addAction(p.getParentId(), a);
            return new ParentedComponent<>(p.getParentId(), a);
        }

        @Override
        protected void updateObject( ParentedComponent<CharacterAction> object, Entity e ) {
        }
        
        @Override
        protected void removeObject( ParentedComponent<CharacterAction> object, Entity e ) {
            removeAction(object.parentId, object.value);
        }
    }
}


