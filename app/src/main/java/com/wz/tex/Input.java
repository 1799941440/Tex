package com.wz.tex;

import android.annotation.SuppressLint;
import android.os.IBinder;
import android.os.IInterface;

import com.wz.tex.wrappers.InputManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressLint("PrivateApi,DiscouragedPrivateApi")
public final class Input {

    private final Method getServiceMethod;
    private InputManager inputManager;
    private static final Input input = new Input();

    public static Input getInstance() {
        return input;
    }

    private Input() {
        try {
            getServiceMethod = Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", String.class);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private IInterface getService(String service, String type) {
        try {
            IBinder binder = (IBinder) getServiceMethod.invoke(null, service);
            Method asInterfaceMethod = Class.forName(type + "$Stub").getMethod("asInterface", IBinder.class);
            return (IInterface) asInterfaceMethod.invoke(null, binder);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public InputManager getInputManager() {
        if (inputManager == null) {
            try {
                Method getInstanceMethod = android.hardware.input.InputManager.class.getDeclaredMethod("getInstance");
                android.hardware.input.InputManager im = (android.hardware.input.InputManager) getInstanceMethod.invoke(null);
                inputManager = new InputManager(im);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new AssertionError(e);
            }
        }
        return inputManager;
    }

}
