/*
 * $Id$
 *
 * Copyright (c) 2025, Simsilica, LLC
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

package com.simsilica.net;

import java.io.Serializable;
import java.util.Objects;

import org.slf4j.*;

import com.google.common.base.MoreObjects;

/**
 *  Encapsulates the data from a chat message.
 *
 *  @author    Paul Speed
 */
public class ChatMessage implements Serializable {
    static Logger log = LoggerFactory.getLogger(ChatMessage.class);

    public static final int TYPE_JOINED = -1;
    public static final int TYPE_LEFT = -2;
    public static final int TYPE_MESSAGE = 0;

    private int type;
    private int clientId;
    private String playerName;
    private String message;

    public ChatMessage( int type, int clientId, String playerName, String message ) {
        this.type = type;
        this.clientId = clientId;
        this.playerName = playerName;
        this.message = message;
    }

    public ChatMessage( int clientId, String playerName, String message ) {
        this(TYPE_MESSAGE, clientId, playerName, message);
    }

    public ChatMessage( int type, int clientId, String playerName ) {
        this(type, clientId, playerName, null);
    }

    public static ChatMessage joined( int clientId, String playerName ) {
        return new ChatMessage(TYPE_JOINED, clientId, playerName);
    }

    public static ChatMessage left( int clientId, String playerName ) {
        return new ChatMessage(TYPE_LEFT, clientId, playerName);
    }

    public int getType() {
        return type;
    }

    public int getClientId() {
        return clientId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getMessage() {
        return message;
    }

    public void deliver( ChatSessionListener l ) {
        switch( type ) {
            case TYPE_JOINED:
                l.playerJoined(clientId, playerName);
                break;
            case TYPE_LEFT:
                l.playerLeft(clientId, playerName);
                break;
            default:
                l.newMessage(clientId, playerName, message);
                break;
        }
    }

    public boolean equals( Object o ) {
        if( o == this ) {
            return true;
        }
        if( o == null || o.getClass() != getClass() ) {
            return false;
        }
        ChatMessage other = (ChatMessage)o;
        if( other.clientId != clientId ) {
            return false;
        }
        if( !Objects.equals(other.playerName, playerName) ) {
            return false;
        }
        if( !Objects.equals(other.message, message) ) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass().getSimpleName())
            .add("type", type)
            .add("clientId", clientId)
            .add("playerName", playerName)
            .add("message", message)
            .toString();
    }
}
