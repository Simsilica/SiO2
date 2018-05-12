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
import com.jme3.app.*;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.bullet.*;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl; 
import com.jme3.bullet.collision.shapes.*;
import com.jme3.bullet.util.*;
import com.jme3.input.KeyInput;
import com.jme3.light.*;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.Filter;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.DepthOfFieldFilter;
import com.jme3.post.filters.FXAAFilter;
import com.jme3.post.filters.LightScatteringFilter;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.math.ColorRGBA;
import com.jme3.material.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.scene.control.*;
import com.jme3.scene.shape.Box;
import com.jme3.shader.VarType;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.jme3.util.SafeArrayList;

import com.simsilica.lemur.*;
import com.simsilica.lemur.geom.*;
import com.simsilica.lemur.input.*;
import com.simsilica.lemur.style.*;

import com.simsilica.es.*;
import com.simsilica.es.base.DefaultEntityData;
import com.simsilica.es.common.*;
import com.simsilica.mathd.*;
import com.simsilica.sim.common.*;

import com.simsilica.bullet.*;
import com.simsilica.bullet.debug.*;

/**
 *
 *
 *  @author    Paul Speed
 */
public class Main extends SimpleApplication {

    public static final String TITLE = "SiO2 Bullet Extension Demo";

    static Logger log = LoggerFactory.getLogger(Main.class);

    private DirectionalLight sun;
    
    private BulletSystem bullet;
    private EntityData ed; 
    private GameSystemsState systems;

    public static void main( String... args ) throws Exception {
        Main main = new Main();

        // Set some defaults that will get overwritten if
        // there were previously saved settings from the last time the user
        // ran.
        AppSettings settings = new AppSettings(true);
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setVSync(true);

        settings.load(TITLE);
        settings.setTitle(TITLE);

        main.setSettings(settings);
        main.start();
    }

    public Main() {
        super(new StatsAppState(), new DebugKeysAppState(), new BasicProfilerState(false),
              new GameSystemsState(),
              new ModelViewState(),
              //new DebugViewState(),
              //new DebugContactState(),
              new FlyCamAppState(),
              new ScreenshotAppState("", System.currentTimeMillis()));
    }

    protected void setupGameSystems() {
        
        this.systems = stateManager.getState(GameSystemsState.class);
 
        this.ed = systems.register(EntityData.class, new DefaultEntityData());
 
        // We'll add the decay system first so that things that need to will
        // get cleaned up at the beginning of update.       
        systems.addSystem(new DecaySystem() {
                protected void destroyEntity( Entity e ) {
                    super.destroyEntity(e);
                    //log.info("Destroyed:" + e.getId());
                }                
            });
        
        
        ModelFactory models = systems.register(ModelFactory.class, 
                                               new SimpleModelFactory(ed, assetManager));       
        CollisionShapes shapes = systems.register(CollisionShapes.class, 
                                                  new DefaultCollisionShapes(ed));
 
        bullet = new BulletSystem();
        bullet.addPhysicsObjectListener(new PositionPublisher(ed));
        bullet.addPhysicsObjectListener(new DebugPhysicsListener(ed));
        bullet.addEntityCollisionListener(new DefaultContactPublisher(ed) {
                /**
                 *  Overridden to give some extra contact decay time so the
                 *  debug visualization always has a chance to see them. 
                 */
                @Override
                protected EntityId createEntity( Contact c ) {
                    EntityId result = ed.createEntity();
                    ed.setComponents(result, c,
                                     Decay.duration(systems.getStepTime().getTime(),
                                                    systems.getStepTime().toSimTime(0.1))
                                     );  
                    return result;                
                }               
            });
        systems.register(BulletSystem.class, bullet);
 
        systems.addSystem(new WanderSystem());
 
        // Add a state we can use to visualize debugging information
        stateManager.attach(new PhysicsDebugState(ed, shapes, new PositionAdapterImpl()));
        stateManager.attach(new ContactDebugState(ed));
 
        // Register some collision shapes we'll use
        shapes.register(ShapeInfo.create("floor", ed),
                        new BoxCollisionShape(new Vector3f(20, 0.25f, 20))); 
        shapes.register(ShapeInfo.create("box", ed),
                        new BoxCollisionShape(new Vector3f(1, 1, 1))); 
        shapes.register(ShapeInfo.create("sphere", ed),
                        new SphereCollisionShape(1)); 
        shapes.register(ShapeInfo.create("wall", ed),
                        new BoxCollisionShape(new Vector3f(10, 1, 0.25f))); 
        shapes.register(ShapeInfo.create("bigSphere", ed),
                        new SphereCollisionShape(3)); 
        shapes.register(ShapeInfo.create("avatar", ed),
                        new CapsuleCollisionShape(0.3f, 1.4f));
        shapes.register(ShapeInfo.create("avatarPerception", ed),
                        new CapsuleCollisionShape(0.7f, 0.6f));

 
        // Create a floor
        EntityId floor = ed.createEntity();
        ed.setComponents(floor,
                         ModelInfo.create("floor", ed),
                         ShapeInfo.create("floor", ed),
                         new SpawnPosition(0, -0.25, 0),
                         new Mass(0));

        // Create a wall or two
        EntityId wall = ed.createEntity();
        ed.setComponents(wall,
                         ModelInfo.create("wall", ed),
                         ShapeInfo.create("wall", ed),
                         new SpawnPosition(10, 1, -20),
                         new Mass(0));

        wall = ed.createEntity();
        ed.setComponents(wall,
                         ModelInfo.create("wall", ed),
                         ShapeInfo.create("wall", ed),
                         new SpawnPosition(-10, 1, -20),
                         new Mass(0));

        wall = ed.createEntity();
        ed.setComponents(wall,
                         ModelInfo.create("wall", ed),
                         ShapeInfo.create("wall", ed),
                         new SpawnPosition(new Vector3f(20, 1, -10), FastMath.HALF_PI),
                         new Mass(0));

        wall = ed.createEntity();
        ed.setComponents(wall,
                         ModelInfo.create("wall", ed),
                         ShapeInfo.create("wall", ed),
                         new SpawnPosition(new Vector3f(20, 1, 10), FastMath.HALF_PI),
                         new Mass(0));
                         
        wall = ed.createEntity();
        ed.setComponents(wall,
                         ModelInfo.create("wall", ed),
                         ShapeInfo.create("wall", ed),
                         new SpawnPosition(10, 1, 20),
                         new Mass(0));

        wall = ed.createEntity();
        ed.setComponents(wall,
                         ModelInfo.create("wall", ed),
                         ShapeInfo.create("wall", ed),
                         new SpawnPosition(-10, 1, 20),
                         new Mass(0));

        wall = ed.createEntity();
        ed.setComponents(wall,
                         ModelInfo.create("wall", ed),
                         ShapeInfo.create("wall", ed),
                         new SpawnPosition(new Vector3f(-20, 1, -10), FastMath.HALF_PI),
                         new Mass(0));

        wall = ed.createEntity();
        ed.setComponents(wall,
                         ModelInfo.create("wall", ed),
                         ShapeInfo.create("wall", ed),
                         new SpawnPosition(new Vector3f(-20, 1, 10), FastMath.HALF_PI),
                         new Mass(0));                         

        // Create a static test box                         
        EntityId testBox = ed.createEntity();
        ed.setComponents(testBox,                           
                         ModelInfo.create("box", ed),
                         ShapeInfo.create("box", ed),
                         new SpawnPosition(5, 1, 5),
                         new Mass(0));
                         
        // Create a static ghost object
        EntityId ghost = ed.createEntity();                         
        ed.setComponents(ghost,                           
                         ShapeInfo.create("bigSphere", ed),
                         //ModelInfo.create("bigSphere", ed),  // just useful for debugging
                         new SpawnPosition(-5, 1, 5),
                         new Ghost());
                         
 
        int wandererCount = 1;
        Random rand = new Random(0);
        for( int i = 0; i < wandererCount; i++ ) {
        
            float x = rand.nextFloat() * 30 - 15;
            float z = rand.nextFloat() * 30 - 15;
            
                                       
            // Create a wanderer
            EntityId wanderer = ed.createEntity();
            ed.setComponents(wanderer,                         
                            ShapeInfo.create("avatar", ed),
                            ModelInfo.create("avatar", ed),
                            new SpawnPosition(x, 1, z),
                            new Mass(30),
                            new Wander());
            EntityId wandererGhost = ed.createEntity();
            ed.setComponents(wandererGhost,
                            ShapeInfo.create("avatarPerception", ed),
                            new SpawnPosition(0, 0.05f, 0.4f),
                            new Ghost(wanderer));
        }
    }

    public void simpleInitApp() {

        if( flyCam != null ) {
            flyCam.setMoveSpeed(4.5f);
        }

        GuiGlobals.initialize(this);
        BaseStyles.loadGlassStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");
        GuiGlobals.getInstance().setCursorEventsEnabled(false);

        setupGameSystems();

        ColorRGBA lightColor = ColorRGBA.White;
        sun = new DirectionalLight();
        sun.setColor(lightColor);
        sun.setDirection(new Vector3f(-0.4f, -1, -0.3f).normalizeLocal());
        rootNode.addLight(sun);

        AmbientLight ambient = new AmbientLight();
        ambient.setColor(new ColorRGBA(0.75f, 0.75f, 0.75f, 1));
        rootNode.addLight(ambient);

        setupCamera();
        setupPostProcessing();
        
        FunctionId bulletDebug = new FunctionId("Toggle Physics Debug");
        GuiGlobals.getInstance().getInputMapper().map(bulletDebug, KeyInput.KEY_F7);
        GuiGlobals.getInstance().getInputMapper().addDelegate(bulletDebug, 
                                                              stateManager.getState(PhysicsDebugState.class), 
                                                              "toggleEnabled");
                                                              
        FunctionId contactDebug = new FunctionId("Toggle Contact Debug");
        GuiGlobals.getInstance().getInputMapper().map(contactDebug, KeyInput.KEY_F7, KeyInput.KEY_LSHIFT);
        GuiGlobals.getInstance().getInputMapper().map(contactDebug, KeyInput.KEY_F7, KeyInput.KEY_RSHIFT);
        GuiGlobals.getInstance().getInputMapper().addDelegate(contactDebug, 
                                                              stateManager.getState(ContactDebugState.class), 
                                                              "toggleEnabled");
        
        FunctionId shootBall = new FunctionId("Shoot Ball");        
        GuiGlobals.getInstance().getInputMapper().map(shootBall, com.simsilica.lemur.input.Button.MOUSE_BUTTON1);
        GuiGlobals.getInstance().getInputMapper().addDelegate(shootBall, this, "shootBall");
        
        FunctionId shootCube = new FunctionId("Shoot Cube");        
        GuiGlobals.getInstance().getInputMapper().map(shootCube, com.simsilica.lemur.input.Button.MOUSE_BUTTON2);
        GuiGlobals.getInstance().getInputMapper().addDelegate(shootCube, this, "shootCube");
    }
 
    public void simpleUpdate( float tpf ) {
    }
        
    protected void setupCamera() {
        cam.setLocation(new Vector3f(-2.735122f, 6.2377f, 18.758024f));
        cam.setRotation(new Quaternion(0.046084855f, 0.95375186f, -0.20081197f, 0.21887925f));    
    }
 
    protected void setupPostProcessing() {

        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        getViewPort().addProcessor(fpp);

        // See if sampling is enabled
        int samples = getContext().getSettings().getSamples();
        boolean aa = samples != 0;
        if( aa ) {
            fpp.setNumSamples(samples);
        }

        DirectionalLightShadowFilter shadows = new DirectionalLightShadowFilter(assetManager, 4096, 4);
        shadows.setShadowIntensity(0.5f);
        //shadows.setEdgeFilteringMode(EdgeFilteringMode.PCFPOISSON);                
        //shadows.setEdgesThickness(15);
        shadows.setLight(sun);        
        shadows.setEnabled(true);        
        fpp.addFilter(shadows);

        // Then SSAO
        //--------------------------------------
        SSAOFilter ssao = new SSAOFilter();
        ssao.setEnabled(false);
        fpp.addFilter(ssao);

        // Setup FXAA only if regular AA is off
        //--------------------------------------
        if( !aa ) {
            FXAAFilter fxaa = new FXAAFilter();
            fxaa.setEnabled(true);
            fpp.addFilter(fxaa);
        }
    }
    
    public void shootCube() {
 
        Vector3f dir = cam.getDirection();
        
        EntityId box = ed.createEntity();
        ed.setComponents(box,                           
                         ModelInfo.create("box", ed),
                         ShapeInfo.create("box", ed),
                         new SpawnPosition(cam.getLocation()),
                         new Impulse(dir.mult(10)),
                         new Mass(10));
        
    }
    
    public void shootBall() {

        Vector3f dir = cam.getDirection();
        
        EntityId ball = ed.createEntity();
        ed.setComponents(ball,                           
                         ModelInfo.create("sphere", ed),
                         ShapeInfo.create("sphere", ed),
                         new SpawnPosition(cam.getLocation()),
                         new Impulse(dir.mult(10)),
                         new Mass(5));
    }

}


