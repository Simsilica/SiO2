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

package com.simsilica.state;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.*;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.util.SafeArrayList;

import com.simsilica.lemur.*;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.core.*;
import com.simsilica.lemur.input.FunctionId;
import com.simsilica.lemur.style.ElementId;

import com.simsilica.post.DefaultSceneProcessor;

/**
 *  This app state provides easy access to put dynamic debug information
 *  on the screen.  The screen is divided into sections that can be
 *  accessed by creating versioned string objects to which other code can
 *  publish changes.  These are then reflected in the HUD as they change.
 *
 *  @author    Paul Speed
 */
public class DebugHudState extends BaseAppState {

    static Logger log = LoggerFactory.getLogger(DebugHudState.class);

    public enum Location { Top, Left, Bottom, Right };

    public static final FunctionId HUD_TOGGLE = new FunctionId("Show Debug HUD");
 
    public static final ElementId SCREEN_ID = new ElementId("debug.screen.container");
    public static final ElementId CONTAINER_ID = new ElementId("debug.container");
    public static final ElementId NAME_ID = new ElementId("debug.name.label");
    public static final ElementId VALUE_ID = new ElementId("debug.value.label");
 
    private Node hudRoot;   
    private Container screen;
    private float zOffset = 0;

    private ReshapeListener reshapeListener = new ReshapeListener();
    private boolean sizeInvalid;
    
    private Container top;
    private Container bottom;
    private Container left;
    private Container right;

    private final Map<String, DebugView> viewIndex = new HashMap<>();
    private final SafeArrayList<DebugView> views = new SafeArrayList<>(DebugView.class); 

    public DebugHudState() {
    }
 
    /**
     *  Creates a view of an existing VersionedObject that when the 
     *  caller changes the values they will show up on the debug HUD.
     */
    public void createDebugValue( String name, Location location, VersionedReference<String> value ) {
        DebugView view = new DebugView(name, location, value);
        views.add(view);
        viewIndex.put(name, view);
 
        switch( location ) {
            case Top:
                view.nameLabel.setTextHAlignment(HAlignment.Right);
                top.addChild(view.nameLabel);
                top.addChild(view.label);                
                break;
            case Bottom:
                view.nameLabel.setTextHAlignment(HAlignment.Right);                
                bottom.addChild(view.nameLabel);
                bottom.addChild(view.label);                
                break;
            case Right:
                right.addChild(view.nameLabel);
                right.addChild(view.label, 1);
                break;
            case Left:
            default:
                left.addChild(view.nameLabel);
                left.addChild(view.label, 1);
                break;
        }        
        resetScreenSize();        
    }
 
    /**
     *  Creates a VersionedHolder to which the caller can provide new
     *  values and they will show up on the debug HUD.
     */
    public VersionedHolder<String> createDebugValue( String name, Location location ) {
        VersionedHolder<String> result = new VersionedHolder<>("");
        createDebugValue(name, location, result.createReference());
        return result;
    }
 
    public void removeDebugValue( String name ) {
        DebugView view = viewIndex.remove(name);
        if( view == null ) {
            return;
        }
        views.remove(view);
        switch(view.location) {
            case Top:
                top.removeChild(view.nameLabel);
                top.removeChild(view.label);                
                break;
            case Bottom:
                bottom.removeChild(view.nameLabel);
                bottom.removeChild(view.label);                
                break;
            case Right:
                right.removeChild(view.nameLabel);
                right.removeChild(view.label);
                break;
            case Left:
            default:
                left.removeChild(view.nameLabel);
                left.removeChild(view.label);
                break;
        }       
    }
 
    /**
     *  Sets the optional root node to use for the HUD.  This is useful
     *  if the application manages multiple GUI viewports.  If this is
     *  not set then the default GUI node is retrieved from the application.
     */
    public void setHudRoot( Node root ) {
        this.hudRoot = root;
    }
    
    public Node getHudRoot() {
        if( hudRoot != null ) {
            return hudRoot;
        }
        return ((SimpleApplication)getApplication()).getGuiNode();
    }
         
    protected ViewPort getHudViewPort() {
        return getApplication().getGuiViewPort();
    }
         
    public void setZOffset( float z ) {
        this.zOffset = z;
    }
    
    public float getZOffset() {
        return zOffset;
    }

    @Override
    protected void initialize( Application app ) {
        
        screen = new Container(new BorderLayout(), SCREEN_ID);
        screen.setBackground(null); 
 
        ElementId containerId = CONTAINER_ID;       
        top = screen.addChild(new Container(new SpringGridLayout(Axis.X, Axis.Y), containerId), 
                              BorderLayout.Position.North);
        bottom = screen.addChild(new Container(new SpringGridLayout(Axis.X, Axis.Y), containerId), 
                                BorderLayout.Position.South);
 
        // For now we'll just do this but I think we need a sub container to 
        // really make things lay out right
        left = screen.addChild(new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Last), containerId), BorderLayout.Position.West);        
        right = screen.addChild(new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Last), containerId), BorderLayout.Position.East);
    }

    @Override
    protected void cleanup( Application app ) {
    }

    protected void resetScreenSize() {
        Camera cam = getApplication().getCamera();
        screen.setPreferredSize(null); // to make sure it can calculate it
        Vector3f pref = screen.getPreferredSize();
 
        if( log.isTraceEnabled() ) {       
            log.trace("resetScreenSize()  cam:" + cam.getWidth() + ", " + cam.getHeight());
        }
        
        // Not sure why set size is not working but preferred size will.  It just
        // means we need to recalculate it in the case that z might change.
        //screen.setSize(new Vector3f(cam.getWidth(), cam.getHeight(), pref.z));
        screen.setPreferredSize(new Vector3f(cam.getWidth(), cam.getHeight(), pref.z));
        screen.setLocalTranslation(0, cam.getHeight(), zOffset);        
        
        sizeInvalid = false;
    }

    @Override
    protected void onEnable() {
        Camera cam = getApplication().getCamera();
        screen.setLocalTranslation(0, cam.getHeight(), zOffset);        
        getHudRoot().attachChild(screen);
        getHudViewPort().addProcessor(reshapeListener);
        resetScreenSize();        
    }

    @Override
    public void update( float tpf ) {
        if( sizeInvalid ) {
            resetScreenSize();
        }
        for( DebugView view : views.getArray() ) {
            view.update();
        }
    }

    @Override
    protected void onDisable() {
        screen.removeFromParent();
        getHudViewPort().removeProcessor(reshapeListener);
    }
    
    private class DebugView {
        String name;
        VersionedReference<String> ref;
        Label nameLabel;
        Label label;
        Location location;
        
        public DebugView( String name, Location location, VersionedReference<String> ref ) {
            this.name = name;
            this.location = location;
            this.ref = ref;
            this.nameLabel = new Label(name + ": ", new ElementId("debug.name.label"));
            this.label = new Label(ref.get(), new ElementId("debug.value.label"));
        }
        
        public void update() {
            if( ref.update() ) {            
                label.setText(ref.get());
            }
        }
    }

    /**
     *  The only way I've found in JME to listen for reshape events
     *  is to hook in a scene processor.
     */    
    private class ReshapeListener extends DefaultSceneProcessor {
        @Override
        public void reshape( ViewPort vp, int w, int h ) {
            if( log.isTraceEnabled() ) {
                log.trace("reshape(" + vp + ", " + w + ", " + h + ")");
            }
            sizeInvalid = true;
        }            
    }
}


