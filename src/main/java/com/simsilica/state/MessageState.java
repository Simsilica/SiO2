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
import java.util.concurrent.*;
import java.util.function.Supplier;

import org.slf4j.*;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.util.SafeArrayList;

import com.simsilica.lemur.*;
import com.simsilica.lemur.style.ElementId;

/**
 *  Pops up various types of messages from the bottom of the screen.
 *  The text lines are standard Lemur Labels with the elementId "message.label".
 *  Each line will fade over time depending on the current fade rate which by
 *  default is 15 seconds.
 *
 *  @author    Paul Speed
 */
public class MessageState extends BaseAppState {

    private static Logger log = LoggerFactory.getLogger(MessageState.class);

    public static final ElementId MESSAGE_LABEL_ID = new ElementId("console.message.label");

    private Node messageRoot;
    private Vector3f offset = new Vector3f();

    private ConcurrentLinkedQueue<Message> pendingMessages = new ConcurrentLinkedQueue<>();
    private SafeArrayList<Message> messages = new SafeArrayList<>(Message.class);

    private float fadeRate = 1/15f; // 15 seconds
    private float alphaOverride = 0;

    private Supplier<Float> maxWidth;

    private float fontScale = 1;

    private int historySize = 0;
    private int scroll = 0;

    // Keep track of the number of lines in history that will fit on the top-most
    // page so we know how far the scroll can go.
    private int lastPageCount = 0;

    public MessageState() {
    }

    public void setFontScale( Float fontScale ) {
        if( Objects.equals(this.fontScale, fontScale) ) {
            return;
        }
        this.fontScale = fontScale;
        resetFontScale();
    }

    public float getFontScale() {
        return fontScale;
    }

    public void setHistorySize( int historySize ) {
        if( this.historySize == historySize ) {
            return;
        }
        this.historySize = historySize;
        refreshLayout();
    }

    public int getHistorySize() {
        return historySize;
    }

    /**
     *  Sets how far back to scroll through history in 'number of messages'.
     */
    public void setScroll( int scroll ) {
        if( historySize > 0 ) {
            scroll = Math.min(scroll, getMaxScroll());
        }
        scroll = Math.max(0, scroll);
        if( this.scroll == scroll ) {
            return;
        }
        this.scroll = scroll;
        refreshLayout();
    }

    public int getScroll() {
        return scroll;
    }

    public int getMaxScroll() {
        if( historySize > 0 ) {
            // Add 1 to allow for at least one blank line to help show that
            // the scrolling is 'done'.
            return Math.max(0, Math.min(historySize, messages.size()) - lastPageCount) + 1;
        }
        return 0;
    }

    public void setFadeTime( float seconds ) {
        if( seconds == 0 ) {
            this.fadeRate = 0; // don't ever fade
        }
        this.fadeRate = 1/seconds;
    }

    public float getFadeTime() {
        return fadeRate == 0 ? 0 : 1/fadeRate;
    }

    /**
     *  Sets a supplier that will return the maximum width for labels.
     *  If null, this defaults to the current UI camera width.
     */
    public void setMaxWidth( Supplier<Float> maxWidth ) {
        this.maxWidth = maxWidth;
    }

    public Supplier<Float> getMaxWidth() {
        return maxWidth;
    }

    protected float calculateMaxWidth() {
        if( maxWidth != null ) {
            return maxWidth.get();
        }
        return getApplication().getCamera().getWidth();
    }

    /**
     *  Overrides the current alpha of all messages in the backlog.  Set to 0
     *  to go back to the regular fading per-message alpha.  This is really
     *  setting an alpha that will be compared to the regular alpha, the larger
     *  of the two will be used.  This means even if you animate fading of the
     *  backlog messages, newer messages will still be as bright as they should
     *  be.
     */
    public void setAlphaOverride( float alphaOverride ) {
        this.alphaOverride = alphaOverride;
    }

    public float getAlphaOverride() {
        return alphaOverride;
    }

    public Label addMessage( String message ) {
        return addMessage(new Label(message, MESSAGE_LABEL_ID));
    }

    public Label addMessage( String message, ColorRGBA color ) {
        if( log.isTraceEnabled() ) {
            log.trace("addMessage(" + message + ", " + color + ")");
        }
        Label result = new Label(message, MESSAGE_LABEL_ID);
        result.setMaxWidth(calculateMaxWidth());
        result.setColor(color);
        addMessage(result);
        return result;
    }

    public <T extends Panel> T addMessage( T label ) {
        Message msg = new Message(label);
        pendingMessages.add(msg);
        return label;
    }

    protected void displayMessage( Message msg ) {
        messages.add(0, msg);
        messageRoot.attachChild(msg.label);
        refreshLayout();
    }

    /**
     *  Moves the messages up from the bottom of the screen to allow
     *  stuff to be display below.
     */
    public void setMessageRootOffset( Vector3f offset ) {
        this.offset.set(offset);
        resetMessageRootLocation();
    }

    public Vector3f getMessageRootOffset() {
        return offset;
    }

    @Override
    protected void initialize( Application app ) {

        // We keep a root node that we can slide as new
        // messages come in.  Initially we can just pop the
        // new messages up relative to the messageRoot but eventually
        // we can also animate this node to slide up from a lower
        // position to make it look like the messages are sliding
        // up smoothly.
        messageRoot = new Node("MessageRoot");
    }

    @Override
    protected void cleanup( Application app ) {
    }

    float nextTime = 1;
    protected void addTestMessages( float tpf ) {
        nextTime -= tpf;
        if( nextTime < 0 ) {
            nextTime = (float)(Math.random() * 1.9) + 0.1f;
            int count = (int)(Math.random() * 4) + 1;
            for( int i = 0; i < count; i++ ) {
                if( (i % 2) == 0 ) {
                    addMessage("> Tick " + System.currentTimeMillis());
                } else {
                    addMessage("> Tock " + System.currentTimeMillis());
                }
            }
        }
    }

    public void update( float tpf ) {

        //addTestMessages(tpf);

        // Add any pending messages
        Message msg = null;
        while( (msg = pendingMessages.poll()) != null ) {
            displayMessage(msg);
        }

        for( Message m : messages.getArray() ) {
            m.update(tpf);
        }
    }

    @Override
    protected void onEnable() {
        Node gui = ((SimpleApplication)getApplication()).getGuiNode();
        gui.attachChild(messageRoot);
        resetMessageRootLocation();
    }

    @Override
    protected void onDisable() {
        messageRoot.removeFromParent();
    }

    protected void resetMessageRootLocation() {
        // Set a small margin for the messages
        messageRoot.setLocalTranslation(5, 5, 0);

        // And move by the current offset
        messageRoot.move(offset);
    }

    protected void resetFontScale() {
        for( Message m : messages.getArray() ) {
            m.resetFontScale();
        }
        refreshLayout();
    }

    protected void refreshLayout() {
        int height = getApplication().getCamera().getHeight();
        int width = getApplication().getCamera().getWidth();

        float y = 0;
        Message[] array = messages.getArray();
        for( int i = 0; i < array.length; i++ ) {
            Message m = array[i];
            if( i < scroll ) {
                // Off the bottom of the screen so remove it
                m.label.removeFromParent();
                continue;
            }
            Vector3f pref = m.label.getPreferredSize();
            y += pref.y;
            m.label.setLocalTranslation(0, y, 0);
            if( y > height ) {
                // Off the top of screen so remove it
                m.label.removeFromParent();
                if( i > historySize ) {
                    // Older than our history allows so remove it forever.
                    messages.remove(m);
                }
            } else if( m.label.getParent() == null ) {
                messageRoot.attachChild(m.label);
            }
        }

        if( historySize > 0 ) {
            // Calculate how many of the last lines will fit on screen
            float ySize = 0;
            lastPageCount = 0;
            for( int i = Math.min(historySize, array.length) - 1; i >= 0; i-- ) {
                Message m = array[i];
                Vector3f pref = m.label.getPreferredSize();
                ySize += pref.y;
                if( ySize > height ) {
                    break;
                }
                lastPageCount++;
            }
        }
    }

    protected class Message {
        private float alpha;
        private Panel label;
        private float fontSize;

        public Message( Panel label ) {
            this(label, 1);
        }

        public Message( Panel label, float initialAlpha ) {
            this.alpha = initialAlpha;
            this.label = label;
            if( label instanceof Label ) {
                this.fontSize = ((Label)label).getFontSize();
                resetFontScale();
            }
        }

        protected void resetFontScale() {
            if( !(label instanceof Label) ) {
                return;
            }
            Label l = (Label)label;
            l.setFontSize(fontScale * fontSize);
        }

        public void update( float tpf ) {
            alpha -= tpf * fadeRate;
            if( alpha < 0 ) {
                alpha = 0;
            }
            // Make them fade slower at first and fade faster towards the end
            float effectiveAlpha = 1 - alpha;
            effectiveAlpha *= effectiveAlpha;
            effectiveAlpha = 1 - effectiveAlpha;
            label.setAlpha(Math.max(effectiveAlpha, alphaOverride));
        }
    }
}
