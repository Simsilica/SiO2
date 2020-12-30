/*
 * $Id$
 * 
 * Copyright (c) 2020, Simsilica, LLC
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

import org.slf4j.*;

import com.jme3.app.*;
import com.jme3.app.state.BaseAppState;
import com.jme3.light.*;
import com.jme3.material.*;
import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.*;
import com.jme3.scene.shape.Box;


/**
 *  A simple self-contained colored cube test scene with lighting, etc..
 *  Useful for adding to an app to get something to display initially
 *  without cutting and pasting the "blue cube" code.
 *
 *  @author    Paul Speed
 */
public class CubeSceneState extends BaseAppState {

    static Logger log = LoggerFactory.getLogger(CubeSceneState.class);

    private Node scene;
    private ColorRGBA color;
    private boolean includeLights;
    private DirectionalLight light;
    private AmbientLight ambient;
    private PointLight pointLight;
    private SpotLight spotLight;

    public CubeSceneState() {
        this(ColorRGBA.Blue, true);
    }        

    public CubeSceneState( ColorRGBA color, boolean includeLights ) {
        this.scene = new Node("Cube Scene");
        this.color = color;
        this.includeLights = includeLights;
        if( includeLights ) {
            light = new DirectionalLight();
            light.setDirection(new Vector3f(-0.2f, -1, -0.3f).normalizeLocal());

            ambient = new AmbientLight();
            ambient.setColor(new ColorRGBA(0.25f, 0.25f, 0.25f, 1));

            pointLight = new PointLight(new Vector3f(-2,0,-2), 4);

            spotLight = new SpotLight(new Vector3f(3,3,-3),new Vector3f(-1f, -1, 1f).normalizeLocal(), 10, ColorRGBA.White.mult(2));
            spotLight.setSpotOuterAngle(FastMath.DEG_TO_RAD * 15);
            spotLight.setSpotInnerAngle(FastMath.DEG_TO_RAD * 10);
        }
    }

    public Node getScene() {
        return scene;
    }

    public boolean getIncludeLights() {
        return includeLights;
    }

    public DirectionalLight getLight() {
        return light;
    }
    
    public AmbientLight getAmbient() {
        return ambient;
    }
    
    public PointLight getPointLight() {
        return pointLight;
    }
    
    public SpotLight getSpotLight() {
        return spotLight;
    }
 
    public Geometry createCube( float xExtent, float yExtent, float zExtent, ColorRGBA color ) {
        Box b = new Box(xExtent, yExtent, zExtent);
        Geometry geom = new Geometry("Box", b);
        Material mat = new Material(getApplication().getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
        mat.setColor("Diffuse", color);
        mat.setColor("Ambient", color);
        mat.setBoolean("UseMaterialColors", true);
        geom.setMaterial(mat);
        return geom;
    }
    
    protected Node getRoot() {
        return ((SimpleApplication)getApplication()).getRootNode();
    }
    
    @Override
    protected void initialize( Application app ) {
        Geometry geom = createCube(1, 1, 1, color);
        geom.setShadowMode(ShadowMode.CastAndReceive);
        scene.attachChild(geom);
    }
    
    @Override
    protected void cleanup( Application app ) {
    }
    
    @Override
    protected void onEnable() {
        Node rootNode = getRoot();
        rootNode.attachChild(scene);
        
        if( includeLights ) {
            rootNode.addLight(light);
            rootNode.addLight(ambient);
            rootNode.addLight(pointLight);
            rootNode.addLight(spotLight);
        }
    }
    
    @Override
    protected void onDisable() {
        scene.removeFromParent();
        if( includeLights ) {
            Node rootNode = getRoot();
            rootNode.removeLight(light);
            rootNode.removeLight(ambient);
            rootNode.removeLight(pointLight);
            rootNode.removeLight(spotLight);
        }
    }
}

