/*
 * $Id$
 * 
 * Copyright (c) 2019, Simsilica, LLC
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

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.KeyInput;

import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.TabbedPanel;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.input.FunctionId;
import com.simsilica.lemur.input.InputMapper;
import com.simsilica.lemur.props.PropertyPanel;
import com.simsilica.lemur.style.ElementId;


/**
 *
 *
 *  @author    Paul Speed
 */
public class SettingsState extends BaseAppState {
    
    public static final FunctionId F_SETTINGS = new FunctionId("Show Settings");
        
    private Container mainWindow;
    private Container mainContents; 
 
    private TabbedPanel tabs;
    
    private boolean originalCursorEventsEnabled = false;
    
    public SettingsState() {
        setEnabled(false);
    }
 
    public static void initializeDefaultMappings( InputMapper inputMapper ) {
        inputMapper.map(F_SETTINGS, KeyInput.KEY_TAB);
    }
 
    public void toggleEnabled() {
        setEnabled(!isEnabled());
    }
    
    public TabbedPanel getParameterTabs() {
        return tabs;
    }

    @Override
    protected void initialize( Application app ) {
        GuiGlobals.getInstance().getInputMapper().addDelegate(F_SETTINGS, this, "toggleEnabled");
        
        mainWindow = new Container(new BorderLayout(), new ElementId("window"), "glass");
        mainWindow.addChild(new Label("Settings", mainWindow.getElementId().child("title.label"), "glass"),
                            BorderLayout.Position.North); 
        mainWindow.setLocalTranslation(10, app.getCamera().getHeight() - 10, 0);        
 
        mainContents = mainWindow.addChild(new Container(mainWindow.getElementId().child("contents.container"), "glass"),
                                                        BorderLayout.Position.Center); 
 
        tabs = new TabbedPanel("glass");       
        mainContents.addChild(tabs);

        CharPhysics charPhys = getState(GameSystemsState.class).get(CharInputSystem.class).getCharPhysics();
        if( charPhys == null ) {
            throw new RuntimeException("Could not find global character physics object");
        }
        
        PropertyPanel phys = new PropertyPanel(null);
        phys.addBooleanField("Jump Cut", charPhys, "shortJumps");
        phys.addBooleanField("Auto-bounce", charPhys, "autoBounce");
        phys.addFloatField("Jump Force", charPhys, "jumpForce", 1, 100, 1); 
        phys.addFloatField("Ground Impulse", charPhys, "groundImpulse", 1, 500, 1); 
        phys.addFloatField("Air Impulse", charPhys, "airImpulse", 0, 500, 1);  
        phys.addFloatField("Gravity", charPhys.gravity, "y", -50, 0, 1);
    
        tabs.addTab("Char Physics", phys);
        
        PropertyPanel controls = new PropertyPanel(null);
        LockedThirdPersonState camState = getState(LockedThirdPersonState.class);
        controls.addBooleanProperty("Invert Y Look", camState, "invertY");
        controls.addBooleanProperty("Springy Y Lag", camState, "springY");
        
        tabs.addTab("Controls", controls);                 
    }
    
    @Override
    protected void cleanup( Application app ) {
        GuiGlobals.getInstance().getInputMapper().removeDelegate(F_SETTINGS, this, "toggleEnabled");
    }
    
    @Override
    protected void onEnable() {
        ((SimpleApplication)getApplication()).getGuiNode().attachChild(mainWindow);
        getState(LockedThirdPersonState.class).setEnabled(false);
        GuiGlobals.getInstance().requestCursorEnabled(this);
    }
    
    @Override
    protected void onDisable() {
        GuiGlobals.getInstance().releaseCursorEnabled(this);
        mainWindow.removeFromParent();
        getState(LockedThirdPersonState.class).setEnabled(true);
        
        // Reset the character physics to the latest settings
        CharInputSystem charInput = getState(GameSystemsState.class).get(CharInputSystem.class);
        charInput.setCharPhysics(charInput.getCharPhysics()); 
        
    }
}


