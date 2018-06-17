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

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.renderer.Camera;


/**
 *  Wraps a camera (the app camera by default) and provides independently
 *  settable properties for things like near/far, fov, etc..
 *
 *  @author    Paul Speed
 */
public class CameraState extends BaseAppState {

    private Camera cam;
    private float fieldOfView;
    private float near;
    private float far;

    /**
     *  Creates a camera state that will initialize the application
     *  camera to a 45 degree fov, 0.1 near plane, and 1000 far plane.
     */      
    public CameraState() {
        this(null, 45, 0.1f, 1000); // 45 is the default JME fov
    }
 
    /**
     *  Creates a camera state that will initialize the application
     *  camera to the specified parameters.
     */   
    public CameraState( float fov, float near, float far ) {
        this(null, fov, near, far);
    }

    /**
     *  Creates a camera state that will initialize the specified
     *  camera to the specified parameters.  If the specified camera is
     *  null then the application's main camera will be used.
     */   
    public CameraState( Camera cam, float fov, float near, float far ) {
        this.cam = cam;
        this.fieldOfView = fov;
        this.near = near;
        this.far = far;
    }
 
    /**
     *  Sets the camera to which this app state applies its settings.  Set to
     *  null to use the application's default main camera.
     */   
    public void setCamera( Camera cam ) {
        if( this.cam == cam ) {
            return;
        }
        this.cam = cam;
        resetCamera();
    }
    
    public Camera getCamera() {
        if( cam != null ) {
            return cam;
        }
        if( getApplication() != null ) {
            return getApplication().getCamera();
        }
        return null;
    }
    
    public void setFieldOfView( float f ) {
        if( this.fieldOfView == f ) {
            return;
        }
        this.fieldOfView = f;
        resetCamera();
    }
    
    public float getFieldOfView() {
        return fieldOfView;
    }
    
    public void setNear( float f ) {
        if( this.near == f ) {
            return;
        }
        this.near = f;
        resetCamera();
    }
    
    public float getNear() {
        return near;
    }
    
    public void setFar( float f ) {
        if( this.far == f ) {
            return;
        }
        this.far = f;
        resetCamera();
    }

    public float getFar() {
        return far;
    }    

    @Override
    protected void initialize( Application app ) {        
    }

    protected void resetCamera() {
        if( isEnabled() ) {
            Camera camera = getCamera();
            float aspect = (float)camera.getWidth() / (float)camera.getHeight(); 
            camera.setFrustumPerspective(fieldOfView, aspect, near, far);
        }    
    }
    
    @Override
    protected void cleanup( Application app ) {
    }

    @Override
    protected void onEnable() {
        resetCamera();
    }

    @Override
    protected void onDisable() {
    }
}

