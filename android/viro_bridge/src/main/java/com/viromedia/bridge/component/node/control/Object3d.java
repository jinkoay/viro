/**
 * Copyright © 2016 Viro Media. All rights reserved.
 */
package com.viromedia.bridge.component.node.control;

import android.net.Uri;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.viro.renderer.jni.AsyncObjListener;
import com.viro.renderer.jni.ExecutableAnimationJni;
import com.viro.renderer.jni.MaterialJni;
import com.viro.renderer.jni.NodeJni;
import com.viro.renderer.jni.ObjectJni;
import com.viromedia.bridge.component.Component;
import com.viromedia.bridge.component.ManagedAnimation;
import com.viromedia.bridge.utility.ViroEvents;
import com.viromedia.bridge.utility.Helper;
import com.viromedia.bridge.utility.ViroLog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Object3d extends Control {
    private static final String TAG = ViroLog.getTag(Object3d.class);

    private static class Object3dAnimation extends ManagedAnimation {
        private NodeJni mNode;
        private String mAnimationKey;

        public Object3dAnimation(ReactApplicationContext context, Object3d parent) {
            super(context, parent);
            mNode = parent.getNodeJni();
        }

        public void setAnimationKey(String key) {
            mAnimationKey = key;
            updateAnimation();
        }

        @Override
        public ExecutableAnimationJni loadAnimation() {
            if (mAnimationKey != null) {
                return new ExecutableAnimationJni(mNode, mAnimationKey);
            }
            else {
                return null;
            }
        }
    }

    private ObjectJni mNative3dObject;
    private Object3dAnimation mManagedAnimation = null;
    private String mAnimationName = null;
    private Uri mSource;
    private List<String> mResources = null;
    private boolean mObjLoaded = false;
    private boolean mSourceChanged = false;

    public Object3d(ReactApplicationContext reactContext) {
        super(reactContext);
        mManagedAnimation = new Object3dAnimation(reactContext, this);
        mManagedAnimation.setNode(this);
    }

    @Override
    public void onTearDown() {
        if (isTornDown()) {
            return;
        }
        if (mNative3dObject != null){
            mNative3dObject.destroy();
            mNative3dObject = null;
        }
        super.onTearDown();
    }

    public void setSource(String source) {
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("source is a required prop for Viro3DObject");
        }
        mSource = Helper.parseUri(source, mReactContext);
        mSourceChanged = true;
    }

    public void setAnimation(ReadableMap animation) {
        mManagedAnimation.parseFromMap(animation);
        if (animation.hasKey("name")) {
            mAnimationName = animation.getString("name");
        }
        else {
            mAnimationName = null;
        }
        updateAnimation();
    }

    public void setResources(List<String> resources) {
        mResources = resources;
    }

    @Override
    public void setMaterials(List<MaterialJni> materials) {
        if (mObjLoaded) {
            super.setMaterials(materials);
        }
        mMaterials = materials;
    }

    @Override
    protected void onPropsSet() {
        if (mSource == null || !mSourceChanged) {
            return;
        }

        super.onPropsSet();
        ObjectJni oldObject3d = mNative3dObject;
        loadDidStart();

        AsyncObjListener listener = new AsyncObjListener() {
            @Override
            public void onObjLoaded() {
                if (isTornDown()) {
                    return;
                }
                mObjLoaded = true;
                if (mMaterials != null) {
                    // set materials on the node after it's finished loading OBJ
                    setMaterials(mMaterials);
                }
                loadDidEnd();
            }

            @Override
            public void onObjAttached() {
                updateAnimation();
            }

            @Override
            public void onObjFailed(String error) {
                if (isTornDown()) {
                    return;
                }
                onError(error);
            }
        };

        boolean isFBX = true;
        if (mSource.getPath().toLowerCase().endsWith("obj")) {
            isFBX = false;
        }

        // if the source is from resources, then pass in the resources it depends on (if any)
        if (mSource.getScheme().equals("res")) {
            Map<String, String> resourceMap = null;
            if (mResources != null) {
                resourceMap = new HashMap<>();
                for (String resource : mResources) {
                    Uri uri = Helper.parseUri(resource, getContext());
                    resourceMap.put(resource, uri.toString());
                }
            }

            mNative3dObject = new ObjectJni(mSource, isFBX, listener, resourceMap);
        } else {
            mNative3dObject = new ObjectJni(mSource, isFBX, listener);
        }
        setGeometry(mNative3dObject);

        if (oldObject3d != null) {
            oldObject3d.destroy();
        }
        mSourceChanged = false;
    }

    private void loadDidStart() {
        mReactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                getId(),
                ViroEvents.ON_LOAD_START,
                null
        );
    }

    private void loadDidEnd() {
        mReactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                getId(),
                ViroEvents.ON_LOAD_END,
                null
        );
    }

    private void updateAnimation() {
        /*
         Get all the animations loaded from the object.
         */
        Set<String> animationKeys = getNodeJni().getAnimationKeys();
        Log.w(TAG, "Keys : " + animationKeys);
        if (animationKeys.isEmpty()) {
            return;
        }

        /*
         If an animation to run was specified (animation.name), then run that animation;
         otherwise just run the first animation.
         */
        String key = null;
        if (mAnimationName == null) {
            key = animationKeys.iterator().next();
        } else {
            if (animationKeys.contains(mAnimationName)) {
                key = mAnimationName;
            } else {
                ViroLog.warn(TAG, "Animation " + mAnimationName + " cannot be run: was not found on object!");
            }
        }
        mManagedAnimation.setAnimationKey(key);
    }
}
