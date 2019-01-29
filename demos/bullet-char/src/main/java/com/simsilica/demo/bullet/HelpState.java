/*
 * $Id$
 * 
 * Copyright (c) 2016, Simsilica, LLC
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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.*;

import com.google.common.base.Joiner;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.KeyInput;
import com.jme3.input.KeyNames;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

import com.simsilica.lemur.*;
import com.simsilica.lemur.input.Axis;
import com.simsilica.lemur.input.Button;
import com.simsilica.lemur.input.FunctionId;
import com.simsilica.lemur.input.InputMapper;
import com.simsilica.lemur.input.InputMapper.Mapping;
import com.simsilica.lemur.style.ElementId;

/**
 *  Presents a help popup to the user when they press F1.
 *
 *  @author    Paul Speed
 */
public class HelpState extends BaseAppState {

    public static final FunctionId F_HELP = new FunctionId("Help");

    static Logger log = LoggerFactory.getLogger(HelpState.class);

    private Container helpWindow;
    private boolean movementState = false; 
    
    private KeyHelp[] keyHelp = {
        new KeyHelp(F_HELP, "Opens/closes this help window."),
        new KeyHelp(PlayerMovementFunctions.F_X_ROTATE, "Rotates left/right."),
        new KeyHelp(PlayerMovementFunctions.F_Y_ROTATE, "Rotates up/down."),
        new KeyHelp(PlayerMovementFunctions.F_MOVE, "Moves forward and back."),
        new KeyHelp(PlayerMovementFunctions.F_STRAFE, "Moves side to side."),
        new KeyHelp(PlayerMovementFunctions.F_RUN, "Run."),
        new KeyHelp(PlayerMovementFunctions.F_JUMP, "Jump."),
        new KeyHelp(SettingsState.F_SETTINGS, "Opens the in-game settings panel."),
        new KeyHelp(Main.PHYSICS_DEBUG, "Toggles the physics debug view."),
        new KeyHelp(Main.CONTACT_DEBUG, "Toggles the contact debug view."),
        //new KeyHelp(Main.SHOOT_BALL, "Shoots a big ball."),
        //new KeyHelp(Main.SHOOT_CUBE, "Shoots a big cube."),
        new KeyHelp("PrtScrn", "Takes a screen shot."),
        new KeyHelp("F5", "Toggles display stats."),
        new KeyHelp("F6", "Toggles rendering frame timings.")
    };

    public HelpState() {
        setEnabled(false);
    }
 
    public static void initializeDefaultMappings( InputMapper inputMapper ) {    
        inputMapper.map(F_HELP, KeyInput.KEY_F1);
    }
 
    public void close() {
        setEnabled(false);
    }
    
    public void toggleEnabled() {
        setEnabled(!isEnabled());
    }
        
    @Override 
    protected void initialize( Application app ) {
        
        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
        inputMapper.addDelegate(F_HELP, this, "toggleEnabled");
 
        helpWindow = new Container();
        Label title = helpWindow.addChild(new Label("In-Game Help", new ElementId("title"))); 
        //title.setFontSize(24);
        title.setInsets(new Insets3f(2, 2, 0, 2));
 
        Container sub = helpWindow.addChild(new Container());
        sub.setInsets(new Insets3f(10, 10, 10, 10));
        sub.addChild(new Label("Key Bindings")); 

        Container keys = sub.addChild(new Container());
 
        Joiner commas = Joiner.on(", ");
        Joiner lines = Joiner.on("\n"); 
        for( KeyHelp help : keyHelp ) {
            help.updateKeys(inputMapper);
            String s = commas.join(help.keyNames);
            keys.addChild(new Label(s, new ElementId("help.key.label")));
            s = lines.join(help.description);
            keys.addChild(new Label(s, new ElementId("help.description.label")), 1);                     
        }       

        //helpWindow.addChild(new ActionButton(new CallMethodAction("Done", this, "close")));
                
        System.out.println("All InputMapper function mappings:");       
        for( FunctionId id : inputMapper.getFunctionIds() ) {
            System.out.println(id);
            System.out.println("  mappings:");
            for( Mapping m : inputMapper.getMappings(id) ) {
                System.out.println("    " + m);
                Object o = m.getPrimaryActivator();
                if( o instanceof Integer ) {
                    Integer keyCode = (Integer)o;
                    System.out.println("      primary:" + KeyNames.getName(keyCode));                    
                } else {
                    System.out.println("      primary:" + o);
                }
                for( Object mod : m.getModifiers() ) {
                    if( mod instanceof Integer ) {
                        Integer keyCode = (Integer)mod;
                        System.out.println("      modifier:" + KeyNames.getName(keyCode));                    
                    }
                }
            }
        }
    }
    
    @Override 
    protected void cleanup( Application app ) {
        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
        inputMapper.removeDelegate(F_HELP, this, "toggleEnabled");
    }
    
    @Override
    protected void onEnable() {
        Node gui = ((SimpleApplication)getApplication()).getGuiNode();
        
        int width = getApplication().getCamera().getWidth();
        int height = getApplication().getCamera().getHeight();
 
        // Base size and positioning off of 1.5x the 'standard scale' 
        //float standardScale = 1; 
        //helpWindow.setLocalScale(1.5f * standardScale);
        
        Vector3f pref = helpWindow.getPreferredSize();
        //pref.multLocal(1.5f * standardScale);
        
        helpWindow.setLocalTranslation(width * 0.5f - pref.x * 0.5f,
                                       height * 0.5f + pref.y * 0.5f,
                                       100);
        
        gui.attachChild(helpWindow);
        GuiGlobals.getInstance().requestFocus(helpWindow);
    }
    
    @Override
    protected void onDisable() {
        helpWindow.removeFromParent();
    }
    
    private class KeyHelp {
        FunctionId function;
        String[] keyNames;
        String[] description;
        
        public KeyHelp( FunctionId function, String... description ) {
            this.function = function;
            this.description = description;
        }
        
        public KeyHelp( String keys, String... description ) {
            this.keyNames = new String[] { keys };
            this.description = description;
        } 
        
        public void updateKeys( InputMapper inputMapper ) {
            if( function == null ) {
                return;
            }
            
            List<String> names = new ArrayList<>();
 
            // Capture all of the keys first           
            for( Mapping m : inputMapper.getMappings(function) ) {
                Object o = m.getPrimaryActivator();
 
                String primary;
                if( o instanceof Button ) {
                    continue;
                } else if( o instanceof Axis ) {
                    continue;
                } else if( o instanceof Integer ) {
                    Integer i = (Integer)o;
                    primary = KeyNames.getName(i);
                } else {
                    // Not a mapping we can deal with
                    continue;
                }
                                
                StringBuilder sb = new StringBuilder(primary);
                for( Object mod : m.getModifiers() ) {
                    if( mod instanceof Integer ) {
                        sb.append("+");
                        sb.append(KeyNames.getName((Integer)mod));
                    }
                }
                names.add(sb.toString());               
            }
 
            // Then capture axis and buttons           
            for( Mapping m : inputMapper.getMappings(function) ) {
                Object o = m.getPrimaryActivator();
 
                String primary;
                if( o instanceof Button ) {
                    primary = ((Button)o).getName();
                } else if( o instanceof Axis ) {
                    primary = ((Axis)o).getName();
                } else {
                    // Not a mapping we can deal with
                    continue;
                }
                
                StringBuilder sb = new StringBuilder(primary);
                for( Object mod : m.getModifiers() ) {
                    if( mod instanceof Integer ) {
                        sb.append("+");
                        sb.append(KeyNames.getName((Integer)mod));
                    }
                }
                names.add(sb.toString());               
            }
            keyNames = new String[names.size()];
            keyNames = names.toArray(keyNames); 
        }
    }
}
