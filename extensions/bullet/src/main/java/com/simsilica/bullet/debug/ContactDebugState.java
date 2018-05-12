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
import com.jme3.material.MatParamOverride;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.*;
import com.jme3.scene.shape.Box;
import com.jme3.scene.debug.Arrow;
import com.jme3.shader.VarType;

import com.simsilica.es.*;
import com.simsilica.lemur.GuiGlobals;

import com.simsilica.bullet.Contact;

/**
 *
 *
 *  @author    Paul Speed
 */
public class ContactDebugState extends BaseAppState {

    static Logger log = LoggerFactory.getLogger(ContactDebugState.class);

    private EntityData ed;
     
    private Node debugRoot;   

    private ContactDebugContainer contacts;
    private List<ContactView> contactViews = new ArrayList<>();

    private Material contactMaterial;    

    public ContactDebugState( EntityData ed ) {
        this.ed = ed;
        setEnabled(false);        
    }
    
    public void toggleEnabled() {
        setEnabled(!isEnabled());
    }
    
    @Override
    protected void initialize( Application app ) {
                
        this.contactMaterial = GuiGlobals.getInstance().createMaterial(ColorRGBA.Green, false).getMaterial();
        contactMaterial.getAdditionalRenderState().setWireframe(true);
        contactMaterial.getAdditionalRenderState().setDepthTest(false);
 
        this.debugRoot = new Node("contactDebugRoot");
        this.contacts = new ContactDebugContainer(ed);
    }
    
    @Override
    protected void cleanup( Application app ) {
    }
    
    @Override
    protected void onEnable() {
        ((SimpleApplication)getApplication()).getRootNode().attachChild(debugRoot);       
        contacts.start();
    }
    
    @Override 
    public void update( float tpf ) {
        contacts.update();
        updateContactViews();
    }
    
    @Override
    protected void onDisable() {
        contacts.stop();
        debugRoot.removeFromParent();
    }
 
    protected void updateContactViews() {
        // Try to reuse items when we can
        int i = 0;
        for( Contact c : contacts.getArray() ) {
            if( i >= contactViews.size() ) {
                ContactView view = new ContactView(c);
                debugRoot.attachChild(view);
                contactViews.add(view);
            } else {
                ContactView existing = contactViews.get(i);
                existing.update(c);
                debugRoot.attachChild(existing); 
            }
            i++;
        }
        
        // "Hide" any of the existing ones we aren't using anymore
        for( ; i < contactViews.size(); i++ ) {
            ContactView existing = contactViews.get(i);
            existing.removeFromParent();
        }
    }

    protected float getContactScale( Contact c ) {
        float energy = c.getEnergy();
        return 0.5f + energy/10f; 
    }
    
    protected ColorRGBA getContactColor( Contact c ) {
        float energy = c.getEnergy();
        if( energy < 1 ) {
            return ColorRGBA.Green;
        }
        if( energy < 10 ) {
            return ColorRGBA.Red;
        }
        if( energy < 60 ) {
            return ColorRGBA.Yellow;
        }
        return ColorRGBA.White;        
    }
     
    private class ContactView extends Node {
        private Contact contact;
        private Geometry arrowGeom;
        private Arrow arrow;
        private MatParamOverride arrowColor = new MatParamOverride(VarType.Vector4, "Color", ColorRGBA.Red);
 
        public ContactView( Contact contact ) {
            super("Contact:" + contact.getEntity1() + " -> " + contact.getEntity2());            
            setQueueBucket(Bucket.Translucent);
            
            arrowGeom = new Geometry(getName() + ":arrow");
            attachChild(arrowGeom);
            arrowGeom.setMaterial(contactMaterial);
            arrowGeom.addMatParamOverride(arrowColor);
 
            this.contact = contact;
            this.arrow = new Arrow(contact.getNormal().mult(0.5f * getContactScale(contact)));
            arrowGeom.setMesh(arrow);
            arrowGeom.updateModelBound();
            arrowColor.setValue(getContactColor(contact));
                
            setLocalTranslation(contact.getLocation());
        }
        
        public void update( Contact contact ) {
            this.contact = contact;
            arrow.setArrowExtent(contact.getNormal().mult(0.5f * getContactScale(contact)));
            arrowGeom.updateModelBound();
            arrowColor.setValue(getContactColor(contact));
 
            setLocalTranslation(contact.getLocation());
        }
    }
    
    private class ContactDebugContainer extends EntityContainer<Contact> {
        public ContactDebugContainer( EntityData ed ) {
            super(ed, Contact.class);
        }
 
        @Override       
        public Contact[] getArray() {
            return super.getArray();
        }
        
        @Override
        protected Contact addObject( Entity e ) {
            return e.get(Contact.class);
        }

        @Override
        protected void updateObject( Contact contact, Entity e ) {            
        }
        
        @Override
        protected void removeObject( Contact object, Entity e ) {
        }               
    }
}
