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

package com.simsilica.state;

import java.util.*;
import java.util.function.Consumer;

import org.slf4j.*;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.KeyInput;
import com.jme3.math.*;
import com.jme3.scene.*;

import com.simsilica.lemur.*;
import com.simsilica.lemur.core.VersionedHolder;
import com.simsilica.lemur.core.VersionedReference;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.component.TextEntryComponent;
import com.simsilica.lemur.event.KeyAction;
import com.simsilica.lemur.event.KeyActionListener;
import com.simsilica.lemur.input.FunctionId;
import com.simsilica.lemur.input.InputMapper;
import com.simsilica.lemur.style.ElementId;


/**
 *  Opens a text entry field at the button of the of the screen for command
 *  entry.  It is up to the application to setup the key mappings for what
 *  opens the console by mapping input to the CommandConsoleState.F_OPEN_CONSOLE
 *  function ID.
 *
 *  @author    Paul Speed
 */
public class CommandConsoleState extends BaseAppState {

    private static final Logger log = LoggerFactory.getLogger(CommandConsoleState.class);

    public static final FunctionId F_OPEN_CONSOLE = new FunctionId("Open Console");

    public static final ElementId CONTAINER_ID = new ElementId("console.container");
    public static final ElementId PROMPT_LABEL_ID = new ElementId("console.prompt.label");
    public static final ElementId TEXT_ENTRY_ID = new ElementId("console.textField");

    private Container entryPanel;
    private Label prompt;
    private TextField entry;
    private Map<KeyAction, KeyActionListener> actionMap = new HashMap<>();

    private CommandEntry shell = new DefaultCommandEntry();

    private boolean alwaysOpen = false;

    private VersionedHolder<Boolean> enabled = new VersionedHolder<>(false);

    public CommandConsoleState() {
        setEnabled(false);

        // Configure the default actions for return/enter and escape.
        actionMap.put(new KeyAction(KeyInput.KEY_RETURN), new NewLine());
        actionMap.put(new KeyAction(KeyInput.KEY_ESCAPE), new Escape());
    }

    public void setCommandEntry( CommandEntry commandEntry ) {
        if( commandEntry == null ) {
            commandEntry = new DefaultCommandEntry();
        }
        this.shell = commandEntry;
    }

    public CommandEntry getCommandEntry() {
        return shell;
    }

    public void toggleConsole() {
        setEnabled(!isEnabled());
    }

    public void setAlwaysOpen( boolean alwaysOpen ) {
        this.alwaysOpen = alwaysOpen;
        setEnabled(alwaysOpen);
    }

    public boolean isAlwaysOpen() {
        return alwaysOpen;
    }

    public VersionedReference<Boolean> createEnabledReference() {
        return enabled.createReference();
    }

    public Map<KeyAction,KeyActionListener> getActionMap() {
        return actionMap;
    }

    @Override
    protected void initialize( Application app ) {

        entryPanel = new Container(new SpringGridLayout(Axis.X, Axis.Y, FillMode.Last, FillMode.None),
                                   new ElementId("console.container"));
        prompt = entryPanel.addChild(new Label(">", PROMPT_LABEL_ID));
        entry = entryPanel.addChild(new TextField("", TEXT_ENTRY_ID));

        // Configure the text field with an actions from before it was created and
        // then swap our map out so callers can configure new actions if desired.
        entry.getActionMap().putAll(actionMap);
        actionMap = entry.getActionMap();

        // Register to any console open input that might be defined
        InputMapper input = GuiGlobals.getInstance().getInputMapper();
        input.addDelegate(F_OPEN_CONSOLE, this, "toggleConsole");
    }

    @Override
    protected void cleanup( Application app ) {
        InputMapper input = GuiGlobals.getInstance().getInputMapper();
        input.removeDelegate(F_OPEN_CONSOLE, this, "toggleConsole");
    }

    @Override
    protected void onEnable() {
        enabled.updateObject(true);
        Node gui = ((SimpleApplication)getApplication()).getGuiNode();
        gui.attachChild(entryPanel);

        // So it will calculate it, clear any cached preferred size
        entryPanel.setPreferredSize(null);

        // Calculate the preferred size
        Vector3f pref = entryPanel.getPreferredSize();

        // Force it to the full width of the screen
        pref.x = getApplication().getCamera().getWidth();
        entryPanel.setPreferredSize(pref);

        // Make the entry panel visible
        entryPanel.setLocalTranslation(0, pref.y, 0);

        getState(MessageState.class).setMessageRootOffset(new Vector3f(0, pref.y, 0));
        getState(MessageState.class).setAlphaOverride(1);

        GuiGlobals.getInstance().requestFocus(entry);
    }

    @Override
    protected void onDisable() {
        enabled.updateObject(false);
        GuiGlobals.getInstance().requestFocus(null);
        entryPanel.removeFromParent();
        getState(MessageState.class).setMessageRootOffset(new Vector3f(0, 0, 0));
        getState(MessageState.class).setAlphaOverride(0);
    }

    protected void enter() {
        // Clear the text and send it to the configured shell
        String text = entry.getText();
        entry.setText("");
        if( !shell.runCommand(text) ) {
            setEnabled(alwaysOpen);
        }
    }

    protected void escape() {
        entry.setText("");
        setEnabled(alwaysOpen);
    }

    protected class NewLine implements KeyActionListener {
        @Override
        public void keyAction( TextEntryComponent source, KeyAction key ) {
            enter();
        }
    }

    protected class Escape implements KeyActionListener {
        @Override
        public void keyAction( TextEntryComponent source, KeyAction key ) {
            escape();
        }
    }

    protected class DefaultCommandEntry implements CommandEntry {
        @Override
        public boolean runCommand( String cmd ) {
            getState(MessageState.class).addMessage(cmd, ColorRGBA.White);
            return false;
        }
    }
}
