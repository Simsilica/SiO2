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

package com.simsilica.event;

import com.google.common.base.MoreObjects;
import com.jme3.network.HostedConnection;
import com.simsilica.es.EntityId;

/**
 *  A standard event for notifying listeners about player entity
 *  events.  In a game that uses the ES, some kind of entity
 *  representing a player will almost always exist.  It is also
 *  quite common to want to do special processing when a new player
 *  entity is created or has joined/left the game.  This is also
 *  often useful to have done in a standardized way for add-on
 *  components to be able to hook into these events without lots
 *  of extra wiring on the part of the developer.
 *
 *  <p>It is up to the application developer to publish these
 *  events at the appropriate time as needed by their game or the
 *  add-on systems that they are using.  In general, it's easy and
 *  a good idea to publish all of them at the appropriate times
 *  as then any SiO2-based add-ons will be sure to function properly.</p>
 *
 *  <p>PlayerEntityEvents fired on the server in a multiplayer game
 *  will have additional information about the connection firing the
 *  event.  In a single player game, this information will be null.</p> 
 *
 *  @author    Paul Speed
 */
public class PlayerEntityEvent {

    /**
     *  Signals that a new player entity has been created.  Applications
     *  can publish this event whenever they consider the entity to be
     *  "created".  This is an application-specific concept as one application
     *  might create a simple player entity and publish the event while another
     *  may allow the user to customize the player entity (character creation)
     *  before signaling the "created" event.
     */
    public static EventType<PlayerEntityEvent> playerEntityCreated = EventType.create("PlayerEntityCreated", PlayerEntityEvent.class);  

    /**
     *  Signals that a player entity is about to join the game and is
     *  in the final setup stages before actually entering the world.
     *  Add-on systems may use this opportunity to add additional things
     *  to the entity or setup other 
     */
    public static EventType<PlayerEntityEvent> playerEntityJoining = EventType.create("PlayerEntityJoining", PlayerEntityEvent.class);
      
    /**
     *  Signals that a player entity has "joined" the game and is ready to
     *  play.  
     */
    public static EventType<PlayerEntityEvent> playerEntityJoined = EventType.create("PlayerEntityJoined", PlayerEntityEvent.class);  

    /**
     *  Signals that a player is about to leave the game.  In a multiplayer
     *  environment, this is the point where the player is leaving but connection
     *  related services may still be operational.
     */
    public static EventType<PlayerEntityEvent> playerEntityLeaving = EventType.create("PlayerEntityLeaving", PlayerEntityEvent.class);  
 
    /**
     *  Signals that a player has left the game and all player-related resources
     *  are being freed.
     */
    public static EventType<PlayerEntityEvent> playerEntityLeft = EventType.create("PlayerEntityLeft", PlayerEntityEvent.class);  
       
    private final EntityId player;
    private final HostedConnection connection;

    /**
     *  Creates a single player entity event (ie: not multiplayer).
     */    
    public PlayerEntityEvent( EntityId player ) {
        this(player, null);
    }
    
    /**
     *  Creates a player entity event for the specified player entity with
     *  optional multiplayer connection information.  The player connection will
     *  be null in single player games.
     */
    public PlayerEntityEvent( EntityId player, HostedConnection connection ) {
        this.player = player;
        this.connection = connection;
    }
 
    /**
     *  Returns the player entity associated with this event.
     */
    public EntityId getPlayer() {
        return player;
    }   
 
    /**
     *  On a multiplayer server, this returns the connection associated with
     *  this player entity event.  In single player, this returns null.
     */
    public HostedConnection getConnection() {
        return connection;
    }   
 
    @Override   
    public String toString() {
        return MoreObjects.toStringHelper(getClass().getSimpleName())
                .omitNullValues()
                .add("player", player)
                .add("connection", connection)
                .toString();
    }
}


