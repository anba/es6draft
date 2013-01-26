/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.throwTypeError;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.CompletePropertyDescriptor;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.FromPropertyDescriptor;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.ToPropertyDescriptor;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.IsCompatiblePropertyDescriptor;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.BuiltinBrand;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * <h1>8 Types</h1>
 * <ul>
 * <li>8.5 Proxy Object Internal Methods and Internal Data Properties
 * </ul>
 */
public class ExoticProxy implements Scriptable {
    private Realm realm;

    /**
     * [[ProxyHandler]]
     */
    private Scriptable proxyHandler;

    /**
     * [[ProxyTarget]]
     */
    private Scriptable proxyTarget;

    public ExoticProxy(Realm realm, Scriptable handler, Scriptable target) {
        this.realm = realm;
        this.proxyHandler = handler;
        this.proxyTarget = target;
    }

    @Override
    public Scriptable newInstance(Realm realm) {
        throw new IllegalStateException();
    }

    /**
     * [[BuiltinBrand]]
     */
    @Override
    public BuiltinBrand getBuiltinBrand() {
        return null;
    }

    private static boolean __hasOwnProperty(Scriptable target, Object propertyKey) {
        if (propertyKey instanceof String) {
            return target.hasOwnProperty((String) propertyKey);
        } else {
            assert propertyKey instanceof Symbol;
            return target.hasOwnProperty((Symbol) propertyKey);
        }
    }

    private static Property __getOwnProperty(Scriptable target, Object propertyKey) {
        if (propertyKey instanceof String) {
            return target.getOwnProperty((String) propertyKey);
        } else {
            assert propertyKey instanceof Symbol;
            return target.getOwnProperty((Symbol) propertyKey);
        }
    }

    private static boolean __defineOwnProperty(Scriptable target, Object propertyKey,
            PropertyDescriptor desc) {
        if (propertyKey instanceof String) {
            return target.defineOwnProperty((String) propertyKey, desc);
        } else {
            assert propertyKey instanceof Symbol;
            return target.defineOwnProperty((Symbol) propertyKey, desc);
        }
    }

    private static boolean __hasProperty(Scriptable target, Object propertyKey) {
        if (propertyKey instanceof String) {
            return target.hasProperty((String) propertyKey);
        } else {
            assert propertyKey instanceof Symbol;
            return target.hasProperty((Symbol) propertyKey);
        }
    }

    private static Object __get(Scriptable target, Object propertyKey, Object receiver) {
        if (propertyKey instanceof String) {
            return target.get((String) propertyKey, receiver);
        } else {
            assert propertyKey instanceof Symbol;
            return target.get((Symbol) propertyKey, receiver);
        }
    }

    private static boolean __set(Scriptable target, Object propertyKey, Object value,
            Object receiver) {
        if (propertyKey instanceof String) {
            return target.set((String) propertyKey, value, receiver);
        } else {
            assert propertyKey instanceof Symbol;
            return target.set((Symbol) propertyKey, value, receiver);
        }
    }

    private static boolean __delete(Scriptable target, Object propertyKey) {
        if (propertyKey instanceof String) {
            return target.delete((String) propertyKey);
        } else {
            assert propertyKey instanceof Symbol;
            return target.delete((Symbol) propertyKey);
        }
    }

    private static Object maskNull(Object val) {
        return (val != null ? val : NULL);
    }

    private static Object unmaskNull(Object jsval) {
        return (jsval != NULL ? jsval : null);
    }

    /**
     * 8.5.1 [[GetInheritance]] ( )
     */
    @Override
    public Scriptable getPrototype() {
        Scriptable handler = proxyHandler;
        Scriptable target = proxyTarget;
        Callable trap = GetMethod(realm, handler, "getPrototypeOf");
        if (trap == null) {
            return target.getPrototype();
        }
        Object handlerProto = trap.call(handler, target);
        Scriptable targetProto = target.getPrototype();
        if (!SameValue(handlerProto, maskNull(targetProto))) {
            throw throwTypeError(realm, "");
        }
        assert (Type.isNull(handlerProto) || Type.isObject(handlerProto));
        return (Scriptable) unmaskNull(handlerProto);
    }

    /**
     * 8.5.2 [[SetInheritance]] (V)
     */
    @Override
    public boolean setPrototype(Scriptable prototype) {
        assert prototype == null || Type.isObject(prototype);
        Scriptable handler = proxyHandler;
        Scriptable target = proxyTarget;
        Callable trap = GetMethod(realm, handler, "setPrototypeOf");
        if (trap == null) {
            return target.setPrototype(prototype);
        }
        boolean trapResult = ToBoolean(trap.call(handler, target, maskNull(prototype)));
        Callable getProtoTrap = GetMethod(realm, handler, "getPrototypeOf");
        if (getProtoTrap == null) {
            return trapResult;
        }
        Object getProtoResult = getProtoTrap.call(handler, target);
        Scriptable targetProto = target.getPrototype();
        if (!SameValue(getProtoResult, maskNull(targetProto))) {
            throw throwTypeError(realm, "");
        }
        return trapResult;
    }

    /**
     * 8.5.3 [[IsExtensible]] ( )
     */
    @Override
    public boolean isExtensible() {
        Scriptable handler = proxyHandler;
        Scriptable target = proxyTarget;
        Callable trap = GetMethod(realm, handler, "isExtensible");
        if (trap == null) {
            return target.isExtensible();
        }
        Object trapResult = trap.call(handler, target);
        boolean proxyIsExtensible = ToBoolean(trapResult);
        boolean targetIsExtensible = target.isExtensible();
        if (proxyIsExtensible != targetIsExtensible) {
            throw throwTypeError(realm, "");
        }
        return proxyIsExtensible;
    }

    /**
     * 8.5.4 [[PreventExtensions]] ( )
     */
    @Override
    public void preventExtensions() {
        Scriptable handler = proxyHandler;
        Scriptable target = proxyTarget;
        Callable trap = GetMethod(realm, handler, "preventExtensions");
        if (trap == null) {
            target.preventExtensions();
            return;
        }
        @SuppressWarnings("unused")
        Object trapResult = trap.call(handler, target);
        Callable isTrap = GetMethod(realm, handler, "isExtensible");
        if (isTrap == null) {
            return;
        }
        Object isTrapResult = isTrap.call(handler, target);
        boolean proxyIsExtensible = ToBoolean(isTrapResult);
        boolean targetIsExtensible = target.isExtensible();
        if (proxyIsExtensible != targetIsExtensible) {
            throw throwTypeError(realm, "");
        }
    }

    /**
     * 8.5.5 [[HasOwnProperty]] (P)
     */
    @Override
    public boolean hasOwnProperty(String propertyKey) {
        return hasOwnProperty((Object) propertyKey);
    }

    /**
     * 8.5.5 [[HasOwnProperty]] (P)
     */
    @Override
    public boolean hasOwnProperty(Symbol propertyKey) {
        return hasOwnProperty((Object) propertyKey);
    }

    /**
     * 8.5.5 [[HasOwnProperty]] (P)
     */
    private boolean hasOwnProperty(Object propertyKey) {
        Scriptable handler = proxyHandler;
        Scriptable target = proxyTarget;
        Callable trap = GetMethod(realm, handler, "hasOwn");
        if (trap == null) {
            return __hasOwnProperty(target, propertyKey);
        }
        Object trapResult = trap.call(handler, target, propertyKey);
        boolean success = ToBoolean(trapResult);
        if (!success) {
            Property targetDesc = __getOwnProperty(target, propertyKey);
            if (targetDesc != null) {
                if (!targetDesc.isConfigurable()) {
                    throw throwTypeError(realm, "");
                }
                boolean extensibleTarget = target.isExtensible();
                if (!extensibleTarget) {
                    throw throwTypeError(realm, "");
                }
            }
        } else {
            boolean extensibleTarget = target.isExtensible();
            if (extensibleTarget) {
                return success;
            }
            Property targetDesc = __getOwnProperty(target, propertyKey);
            if (targetDesc == null) {
                throw throwTypeError(realm, "");
            }
        }
        return success;
    }

    /**
     * 8.5.6 [[GetOwnProperty]] (P)
     */
    @Override
    public Property getOwnProperty(String propertyKey) {
        return getOwnProperty((Object) propertyKey);
    }

    /**
     * 8.5.6 [[GetOwnProperty]] (P)
     */
    @Override
    public Property getOwnProperty(Symbol propertyKey) {
        return getOwnProperty((Object) propertyKey);
    }

    /**
     * 8.5.6 [[GetOwnProperty]] (P)
     */
    private Property getOwnProperty(Object propertyKey) {
        Scriptable handler = proxyHandler;
        Scriptable target = proxyTarget;
        Callable trap = GetMethod(realm, handler, "getOwnPropertyDescriptor");
        if (trap == null) {
            return __getOwnProperty(target, propertyKey);
        }
        Object trapResultObj = trap.call(handler, target, propertyKey);
        if (!(Type.isObject(trapResultObj) || Type.isUndefined(trapResultObj))) {
            throw throwTypeError(realm, "");
        }
        // TODO: need copy b/c of side-effects?
        Property targetDesc = __getOwnProperty(target, propertyKey);
        if (Type.isUndefined(trapResultObj)) {
            if (targetDesc == null) {
                return null;
            }
            if (!targetDesc.isConfigurable()) {
                throw throwTypeError(realm, "");
            }
            boolean extensibleTarget = target.isExtensible();
            if (!extensibleTarget) {
                throw throwTypeError(realm, "");
            }
            return null;
        }
        // TODO: side-effect in isExtensible()?
        boolean extensibleTarget = target.isExtensible();
        PropertyDescriptor resultDesc = ToPropertyDescriptor(realm, trapResultObj);
        CompletePropertyDescriptor(resultDesc, targetDesc);
        boolean valid = IsCompatiblePropertyDescriptor(extensibleTarget, resultDesc, targetDesc);
        if (!valid) {
            throw throwTypeError(realm, "");
        }
        if (!resultDesc.isConfigurable()) {
            if (targetDesc != null && targetDesc.isConfigurable()) {
                throw throwTypeError(realm, "");
            }
        }
        // TODO: [[Origin]] ???
        return resultDesc.toProperty();
    }

    /**
     * 8.5.7 [[DefineOwnProperty]] (P, Desc)
     */
    @Override
    public boolean defineOwnProperty(String propertyKey, PropertyDescriptor desc) {
        return defineOwnProperty((Object) propertyKey, desc);
    }

    /**
     * 8.5.7 [[DefineOwnProperty]] (P, Desc)
     */
    @Override
    public boolean defineOwnProperty(Symbol propertyKey, PropertyDescriptor desc) {
        return defineOwnProperty((Object) propertyKey, desc);
    }

    /**
     * 8.5.7 [[DefineOwnProperty]] (P, Desc)
     */
    private boolean defineOwnProperty(Object propertyKey, PropertyDescriptor desc) {
        Scriptable handler = proxyHandler;
        Scriptable target = proxyTarget;
        Callable trap = GetMethod(realm, handler, "defineProperty");
        if (trap == null) {
            return __defineOwnProperty(target, propertyKey, desc);
        }
        Object descObj = FromPropertyDescriptor(realm, desc);
        Object trapResult = trap.call(handler, target, propertyKey, descObj);
        if (!ToBoolean(trapResult)) {
            return false;
        }
        // TODO: need copy b/c of side-effects?
        Property targetDesc = __getOwnProperty(target, propertyKey);
        // TODO: side-effect in isExtensible()?
        boolean extensibleTarget = target.isExtensible();
        if (targetDesc == null) {
            if (!extensibleTarget) {
                throw throwTypeError(realm, "");
            }
            if (!desc.isConfigurable()) {
                throw throwTypeError(realm, "");
            }
        } else {
            if (!IsCompatiblePropertyDescriptor(extensibleTarget, desc, targetDesc)) {
                throw throwTypeError(realm, "");
            }
            if (!desc.isConfigurable() && targetDesc.isConfigurable()) {
                throw throwTypeError(realm, "");
            }
        }
        return true;
    }

    /**
     * 8.5.8 [[HasProperty]] (P)
     */
    @Override
    public boolean hasProperty(String propertyKey) {
        return hasProperty((Object) propertyKey);
    }

    /**
     * 8.5.8 [[HasProperty]] (P)
     */
    @Override
    public boolean hasProperty(Symbol propertyKey) {
        return hasProperty((Object) propertyKey);
    }

    /**
     * 8.5.8 [[HasProperty]] (P)
     */
    private boolean hasProperty(Object propertyKey) {
        Scriptable handler = proxyHandler;
        Scriptable target = proxyTarget;
        Callable trap = GetMethod(realm, handler, "has");
        if (trap == null) {
            return __hasProperty(target, propertyKey);
        }
        Object trapResult = trap.call(handler, target, propertyKey);
        boolean success = ToBoolean(trapResult);
        if (!success) {
            Property targetDesc = __getOwnProperty(target, propertyKey);
            if (targetDesc != null) {
                if (!targetDesc.isConfigurable()) {
                    throw throwTypeError(realm, "");
                }
                boolean extensibleTarget = target.isExtensible();
                if (!extensibleTarget) {
                    throw throwTypeError(realm, "");
                }
            }
        }
        return success;
    }

    /**
     * 8.5.9 [[GetP]] (P, Receiver)
     */
    @Override
    public Object get(String propertyKey, Object receiver) {
        return get((Object) propertyKey, receiver);
    }

    /**
     * 8.5.9 [[GetP]] (P, Receiver)
     */
    @Override
    public Object get(Symbol propertyKey, Object receiver) {
        return get((Object) propertyKey, receiver);
    }

    /**
     * 8.5.9 [[GetP]] (P, Receiver)
     */
    private Object get(Object propertyKey, Object receiver) {
        Scriptable handler = proxyHandler;
        Scriptable target = proxyTarget;
        Callable trap = GetMethod(realm, handler, "get");
        if (trap == null) {
            return __get(target, propertyKey, receiver);
        }
        Object trapResult = trap.call(handler, target, propertyKey, receiver);
        Property targetDesc = __getOwnProperty(target, propertyKey);
        if (targetDesc != null) {
            if (targetDesc.isDataDescriptor() && !targetDesc.isConfigurable()
                    && !targetDesc.isWritable()) {
                if (!SameValue(trapResult, targetDesc.getValue())) {
                    throw throwTypeError(realm, "");
                }
            }
            if (targetDesc.isAccessorDescriptor() && !targetDesc.isConfigurable()
                    && targetDesc.getGetter() == null) {
                if (trapResult != UNDEFINED) {
                    throw throwTypeError(realm, "");
                }
            }
        }
        return trapResult;
    }

    /**
     * 8.5.10 [[SetP]] ( P, V, Receiver)
     */
    @Override
    public boolean set(String propertyKey, Object value, Object receiver) {
        return set((Object) propertyKey, value, receiver);
    }

    /**
     * 8.5.10 [[SetP]] ( P, V, Receiver)
     */
    @Override
    public boolean set(Symbol propertyKey, Object value, Object receiver) {
        return set((Object) propertyKey, value, receiver);
    }

    /**
     * 8.5.10 [[SetP]] ( P, V, Receiver)
     */
    private boolean set(Object propertyKey, Object value, Object receiver) {
        Scriptable handler = proxyHandler;
        Scriptable target = proxyTarget;
        Callable trap = GetMethod(realm, handler, "set");
        if (trap == null) {
            return __set(target, propertyKey, value, receiver);
        }
        Object trapResult = trap.call(handler, target, propertyKey, value, receiver);
        if (!ToBoolean(trapResult)) {
            return false;
        }
        Property targetDesc = __getOwnProperty(target, propertyKey);
        if (targetDesc != null) {
            if (targetDesc.isDataDescriptor() && !targetDesc.isConfigurable()
                    && !targetDesc.isWritable()) {
                if (!SameValue(value, targetDesc.getValue())) {
                    throw throwTypeError(realm, "");
                }
            }
            if (targetDesc.isAccessorDescriptor() && !targetDesc.isConfigurable()) {
                if (targetDesc.getSetter() == null) {
                    throw throwTypeError(realm, "");
                }
            }
        }
        return true;
    }

    /**
     * 8.5.11 [[Delete]] (P)
     */
    @Override
    public boolean delete(String propertyKey) {
        return delete((Object) propertyKey);
    }

    /**
     * 8.5.11 [[Delete]] (P)
     */
    @Override
    public boolean delete(Symbol propertyKey) {
        return delete((Object) propertyKey);
    }

    /**
     * 8.5.11 [[Delete]] (P)
     */
    private boolean delete(Object propertyKey) {
        Scriptable handler = proxyHandler;
        Scriptable target = proxyTarget;
        Callable trap = GetMethod(realm, handler, "deleteProperty");
        if (trap == null) {
            return __delete(target, propertyKey);
        }
        Object trapResult = trap.call(handler, target, propertyKey);
        if (!ToBoolean(trapResult)) {
            return false;
        }
        Property targetDesc = __getOwnProperty(target, propertyKey);
        if (targetDesc == null) {
            return true;
        }
        if (!targetDesc.isConfigurable()) {
            throw throwTypeError(realm, "");
        }
        return true;
    }

    /**
     * 8.5.12 [[Enumerate]] ()
     */
    @Override
    public Scriptable enumerate() {
        Scriptable handler = proxyHandler;
        Scriptable target = proxyTarget;
        Callable trap = GetMethod(realm, handler, "enumerate");
        if (trap == null) {
            return target.enumerate();
        }
        Object trapResult = trap.call(handler, target);
        if (!Type.isObject(trapResult)) {
            throw throwTypeError(realm, "");
        }
        return Type.objectValue(trapResult);
    }

    /**
     * 8.5.13 [[OwnPropertyKeys]] ()
     */
    @Override
    public Scriptable ownPropertyKeys() {
        Scriptable handler = proxyHandler;
        Scriptable target = proxyTarget;
        Callable trap = GetMethod(realm, handler, "ownPropertyKeys");
        if (trap == null) {
            return target.ownPropertyKeys();
        }
        Object trapResult = trap.call(handler, target);
        if (!Type.isObject(trapResult)) {
            throw throwTypeError(realm, "");
        }
        return Type.objectValue(trapResult);
    }

    /**
     * 8.5.14 [[Freeze]] ()
     */
    @Override
    public void freeze() {
        // FIXME: spec bug (boolean argument should be `true`) (bug 1208)
        MakeObjectSecure(realm, this, true);
    }

    /**
     * 8.5.15 [[Seal]] ()
     */
    @Override
    public void seal() {
        MakeObjectSecure(realm, this, false);
    }

    /**
     * 8.5.16 [[IsFrozen]] ()
     */
    @Override
    public boolean isFrozen() {
        return TestIfSecureObject(realm, this, true);
    }

    /**
     * 8.5.17 [[IsSealed]] ()
     */
    @Override
    public boolean isSealed() {
        return TestIfSecureObject(realm, this, false);
    }

}
