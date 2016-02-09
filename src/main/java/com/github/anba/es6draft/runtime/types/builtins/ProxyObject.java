/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.PrepareForTailCall;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.CompletePropertyDescriptor;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.FromPropertyDescriptor;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.ToPropertyDescriptor;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.IsCompatiblePropertyDescriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ScriptIterator;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1>
 * <ul>
 * <li>9.5 Proxy Object Internal Methods and Internal Slots
 * </ul>
 */
public class ProxyObject implements ScriptObject {
    /** [[ProxyTarget]] */
    private ScriptObject proxyTarget;
    /** [[ProxyHandler]] */
    private ScriptObject proxyHandler;

    /**
     * Constructs a new Proxy object.
     * 
     * @param target
     *            the proxy target object
     * @param handler
     *            the proxy handler object
     */
    protected ProxyObject(ScriptObject target, ScriptObject handler) {
        assert target != null && handler != null;
        this.proxyTarget = target;
        this.proxyHandler = handler;
    }

    /**
     * Returns the proxy target object.
     * 
     * @return the proxy target object
     */
    protected final ScriptObject getProxyTarget() {
        assert proxyTarget != null : "Proxy is revoked";
        return proxyTarget;
    }

    /**
     * Returns the proxy handler object.
     * 
     * @return the proxy handler object
     */
    protected final ScriptObject getProxyHandler() {
        assert proxyHandler != null : "Proxy is revoked";
        return proxyHandler;
    }

    /**
     * Returns the proxy target object or throws a script exception of the proxy has been revoked.
     * 
     * @param cx
     *            the execution context
     * @return the proxy handler object
     */
    public final ScriptObject getProxyTarget(ExecutionContext cx) {
        if (isRevoked()) {
            throw newTypeError(cx, Messages.Key.ProxyRevoked);
        }
        return getProxyTarget();
    }

    /**
     * Returns the proxy handler object or throws a script exception of the proxy has been revoked.
     * 
     * @param cx
     *            the execution context
     * @return the proxy handler object
     */
    public final ScriptObject getProxyHandler(ExecutionContext cx) {
        if (isRevoked()) {
            throw newTypeError(cx, Messages.Key.ProxyRevoked);
        }
        return getProxyHandler();
    }

    /**
     * Returns {@code true} if the proxy has been revoked.
     * 
     * @return {@code true} if the proxy has been revoked
     */
    protected final boolean isRevoked() {
        return proxyHandler == null;
    }

    /**
     * Revoke this proxy, that means set both, [[ProxyTarget]] and [[ProxyHandler]], to {@code null} and by that prevent
     * further operations on this proxy object.
     */
    public final void revoke() {
        assert !isRevoked() : "Proxy already revoked";
        this.proxyHandler = null;
        this.proxyTarget = null;
    }

    /**
     * Repeatedly unwraps the proxy object and returns the underlying target object.
     * 
     * @param cx
     *            the execution context
     * @return the proxy target object
     */
    public final ScriptObject unwrap(ExecutionContext cx) {
        ScriptObject target;
        for (ProxyObject proxy = this; (target = proxy.proxyTarget) instanceof ProxyObject;) {
            proxy = (ProxyObject) target;
        }
        if (target == null) {
            throw newTypeError(cx, Messages.Key.ProxyRevoked);
        }
        return target;
    }

    @Override
    public String toString() {
        return String.format("%s@%x {{%n\tTarget=%s%n\tHandler=%s%n}}", getClass().getSimpleName(),
                System.identityHashCode(this), proxyTarget, proxyHandler);
    }

    private static class CallableProxyObject extends ProxyObject implements Callable {
        /**
         * Constructs a new Proxy object.
         * 
         * @param target
         *            the proxy target object
         * @param handler
         *            the proxy handler object
         */
        public CallableProxyObject(ScriptObject target, ScriptObject handler) {
            super(target, handler);
        }

        /**
         * 9.5.13 [[Call]] (thisArgument, argumentsList)
         */
        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            /* steps 1-3 */
            ScriptObject handler = getProxyHandler(callerContext);
            /* step 4 */
            ScriptObject target = getProxyTarget();
            /* steps 5-6 */
            Callable trap = GetMethod(callerContext, handler, "apply");
            /* step 7 */
            if (trap == null) {
                return ((Callable) target).call(callerContext, thisValue, args);
            }
            /* step 8 */
            ArrayObject argArray = CreateArrayFromList(callerContext, Arrays.asList(args));
            /* step 9 */
            return trap.call(callerContext, handler, target, thisValue, argArray);
        }

        /**
         * 9.5.13 [[Call]] (thisArgument, argumentsList)
         */
        @Override
        public Object tailCall(ExecutionContext callerContext, Object thisValue, Object... args) throws Throwable {
            /* steps 1-3 */
            ScriptObject handler = getProxyHandler(callerContext);
            /* step 4 */
            ScriptObject target = getProxyTarget();
            /* steps 5-6 */
            Callable trap = GetMethod(callerContext, handler, "apply");
            /* step 7 */
            if (trap == null) {
                return ((Callable) target).tailCall(callerContext, thisValue, args);
            }
            /* step 8 */
            ArrayObject argArray = CreateArrayFromList(callerContext, Arrays.asList(args));
            /* step 9 */
            // NB: PrepareForTailCall is necessary to handle the case when trap is this proxy
            // object, or a bound function instance of this proxy object.
            return PrepareForTailCall(trap, handler, new Object[] { target, thisValue, argArray });
        }

        @Override
        public Callable clone(ExecutionContext cx) {
            throw newTypeError(cx, Messages.Key.FunctionNotCloneable);
        }

        @Override
        public Realm getRealm(ExecutionContext cx) {
            /* 7.3.22 GetFunctionRealm ( obj ) */
            if (isRevoked()) {
                throw newTypeError(cx, Messages.Key.ProxyRevoked);
            }
            return ((Callable) getProxyTarget()).getRealm(cx);
        }

        @Override
        public String toSource(ExecutionContext cx) {
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }
    }

    private static class ConstructorProxyObject extends CallableProxyObject implements Constructor {
        /**
         * Constructs a new Proxy object.
         * 
         * @param target
         *            the proxy target object
         * @param handler
         *            the proxy handler object
         */
        public ConstructorProxyObject(ScriptObject target, ScriptObject handler) {
            super(target, handler);
        }

        /**
         * 9.5.14 [[Construct]] Internal Method
         */
        @Override
        public ScriptObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
            /* steps 1-3 */
            ScriptObject handler = getProxyHandler(callerContext);
            /* step 4 */
            ScriptObject target = getProxyTarget();
            /* steps 5-6 */
            Callable trap = GetMethod(callerContext, handler, "construct");
            /* step 7 */
            if (trap == null) {
                return ((Constructor) target).construct(callerContext, newTarget, args);
            }
            /* step 8 */
            ArrayObject argArray = CreateArrayFromList(callerContext, Arrays.asList(args));
            /* steps 9-10 */
            Object newObj = trap.call(callerContext, handler, target, argArray, newTarget);
            /* step 11 */
            if (!Type.isObject(newObj)) {
                throw newTypeError(callerContext, Messages.Key.ProxyNotObject);
            }
            /* step 12 */
            return Type.objectValue(newObj);
        }
    }

    /**
     * 9.5.15 ProxyCreate(target, handler)
     * 
     * @param cx
     *            the execution context
     * @param target
     *            the proxy target object
     * @param handler
     *            the proxy handler object
     * @return the new proxy object
     */
    public static ProxyObject ProxyCreate(ExecutionContext cx, Object target, Object handler) {
        /* step 1 */
        if (!Type.isObject(target)) {
            throw newTypeError(cx, Messages.Key.NotObjectType);
        }
        ScriptObject proxyTarget = Type.objectValue(target);
        /* step 2 */
        if (proxyTarget instanceof ProxyObject && ((ProxyObject) proxyTarget).isRevoked()) {
            throw newTypeError(cx, Messages.Key.ProxyRevoked);
        }
        /* step 3 */
        if (!Type.isObject(handler)) {
            throw newTypeError(cx, Messages.Key.NotObjectType);
        }
        ScriptObject proxyHandler = Type.objectValue(handler);
        /* step 4 */
        if (proxyHandler instanceof ProxyObject && ((ProxyObject) proxyHandler).isRevoked()) {
            throw newTypeError(cx, Messages.Key.ProxyRevoked);
        }
        /* steps 8-9 */
        ProxyObject proxy;
        if (IsCallable(proxyTarget)) {
            if (IsConstructor(proxyTarget)) {
                proxy = new ConstructorProxyObject(proxyTarget, proxyHandler);
            } else {
                proxy = new CallableProxyObject(proxyTarget, proxyHandler);
            }
        } else {
            proxy = new ProxyObject(proxyTarget, proxyHandler);
        }
        /* step 10 */
        return proxy;
    }

    /**
     * 9.5.1 [[GetPrototypeOf]] ( )
     */
    @Override
    public ScriptObject getPrototypeOf(ExecutionContext cx) {
        /* steps 1-3 */
        ScriptObject handler = getProxyHandler(cx);
        /* step 4 */
        ScriptObject target = getProxyTarget();
        /* steps 5-6 */
        Callable trap = GetMethod(cx, handler, "getPrototypeOf");
        /* step 7 */
        if (trap == null) {
            return target.getPrototypeOf(cx);
        }
        /* steps 8-9 */
        Object handlerProto = trap.call(cx, handler, target);
        /* step 10 */
        if (!Type.isObjectOrNull(handlerProto)) {
            throw newTypeError(cx, Messages.Key.NotObjectOrNull);
        }
        ScriptObject handlerProtoObj = Type.objectValueOrNull(handlerProto);
        /* steps 11-12 */
        boolean extensibleTarget = IsExtensible(cx, target);
        /* step 13 */
        if (extensibleTarget) {
            return handlerProtoObj;
        }
        /* steps 14-15 */
        ScriptObject targetProto = target.getPrototypeOf(cx);
        /* step 16 */
        if (handlerProtoObj != targetProto) {
            throw newTypeError(cx, Messages.Key.ProxySamePrototype);
        }
        /* step 17 */
        return handlerProtoObj;
    }

    /**
     * 9.5.2 [[SetPrototypeOf]] (V)
     */
    @Override
    public boolean setPrototypeOf(ExecutionContext cx, ScriptObject prototype) {
        /* step 1 (implicit) */
        /* steps 2-4 */
        ScriptObject handler = getProxyHandler(cx);
        /* step 5 */
        ScriptObject target = getProxyTarget();
        /* steps 6-7 */
        Callable trap = GetMethod(cx, handler, "setPrototypeOf");
        /* step 8 */
        if (trap == null) {
            return target.setPrototypeOf(cx, prototype);
        }
        /* steps 9-10 */
        Object prototypeValue = prototype != null ? prototype : NULL;
        boolean trapResult = ToBoolean(trap.call(cx, handler, target, prototypeValue));
        /* steps 11-12 */
        boolean extensibleTarget = IsExtensible(cx, target);
        /* step 13 */
        if (extensibleTarget) {
            return trapResult;
        }
        /* steps 14-15 */
        ScriptObject targetProto = target.getPrototypeOf(cx);
        /* step 16 */
        if (trapResult && prototype != targetProto) {
            throw newTypeError(cx, Messages.Key.ProxySamePrototype);
        }
        /* step 17 */
        return trapResult;
    }

    /**
     * 9.5.3 [[IsExtensible]] ( )
     */
    @Override
    public boolean isExtensible(ExecutionContext cx) {
        /* steps 1-3 */
        ScriptObject handler = getProxyHandler(cx);
        /* step 4 */
        ScriptObject target = getProxyTarget();
        /* steps 5-6 */
        Callable trap = GetMethod(cx, handler, "isExtensible");
        /* step 7 */
        if (trap == null) {
            return target.isExtensible(cx);
        }
        /* steps 8-9 */
        boolean trapResult = ToBoolean(trap.call(cx, handler, target));
        /* steps 10-11 */
        boolean targetResult = target.isExtensible(cx);
        /* step 12 */
        if (trapResult != targetResult) {
            throw newTypeError(cx, Messages.Key.ProxyExtensible);
        }
        /* step 13 */
        return trapResult;
    }

    /**
     * 9.5.4 [[PreventExtensions]] ( )
     */
    @Override
    public boolean preventExtensions(ExecutionContext cx) {
        /* steps 1-3 */
        ScriptObject handler = getProxyHandler(cx);
        /* step 4 */
        ScriptObject target = getProxyTarget();
        /* steps 5-6 */
        Callable trap = GetMethod(cx, handler, "preventExtensions");
        /* step 7 */
        if (trap == null) {
            return target.preventExtensions(cx);
        }
        /* steps 8-9 */
        boolean trapResult = ToBoolean(trap.call(cx, handler, target));
        /* step 10 */
        if (trapResult) {
            boolean targetIsExtensible = target.isExtensible(cx);
            if (targetIsExtensible) {
                throw newTypeError(cx, Messages.Key.ProxyExtensible);
            }
        }
        /* step 11 */
        return trapResult;
    }

    /**
     * 9.5.5 [[GetOwnProperty]] (P)
     */
    @Override
    public Property getOwnProperty(ExecutionContext cx, long propertyKey) {
        return getOwnProperty(cx, ToString(propertyKey));
    }

    /**
     * 9.5.5 [[GetOwnProperty]] (P)
     */
    @Override
    public Property getOwnProperty(ExecutionContext cx, String propertyKey) {
        /* step 1 (implicit) */
        /* steps 2-4 */
        ScriptObject handler = getProxyHandler(cx);
        /* step 5 */
        ScriptObject target = getProxyTarget();
        /* steps 6-7 */
        Callable trap = GetMethod(cx, handler, "getOwnPropertyDescriptor");
        /* step 8 */
        if (trap == null) {
            return target.getOwnProperty(cx, propertyKey);
        }
        /* steps 9-10 */
        Object trapResultObj = trap.call(cx, handler, target, propertyKey);
        /* step 11 */
        if (!(Type.isObject(trapResultObj) || Type.isUndefined(trapResultObj))) {
            throw newTypeError(cx, Messages.Key.ProxyNotObjectOrUndefined);
        }
        /* steps 12-13 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 14-23 */
        return validateGetOwnProperty(cx, target, trapResultObj, targetDesc);
    }

    /**
     * 9.5.5 [[GetOwnProperty]] (P)
     */
    @Override
    public Property getOwnProperty(ExecutionContext cx, Symbol propertyKey) {
        /* step 1 (implicit) */
        /* steps 2-4 */
        ScriptObject handler = getProxyHandler(cx);
        /* step 5 */
        ScriptObject target = getProxyTarget();
        /* steps 6-7 */
        Callable trap = GetMethod(cx, handler, "getOwnPropertyDescriptor");
        /* step 8 */
        if (trap == null) {
            return target.getOwnProperty(cx, propertyKey);
        }
        /* steps 9-10 */
        Object trapResultObj = trap.call(cx, handler, target, propertyKey);
        /* step 11 */
        if (!(Type.isObject(trapResultObj) || Type.isUndefined(trapResultObj))) {
            throw newTypeError(cx, Messages.Key.ProxyNotObjectOrUndefined);
        }
        /* steps 12-13 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 14-23 */
        return validateGetOwnProperty(cx, target, trapResultObj, targetDesc);
    }

    private Property validateGetOwnProperty(ExecutionContext cx, ScriptObject target, Object trapResultObj,
            Property targetDesc) {
        /* step 14 */
        if (Type.isUndefined(trapResultObj)) {
            if (targetDesc == null) {
                return null;
            }
            if (!targetDesc.isConfigurable()) {
                throw newTypeError(cx, Messages.Key.ProxyNotConfigurable);
            }
            boolean extensibleTarget = IsExtensible(cx, target);
            if (!extensibleTarget) {
                throw newTypeError(cx, Messages.Key.ProxyNotExtensible);
            }
            return null;
        }
        if (targetDesc != null) {
            // need copy because of possible side-effects in IsExtensible()
            targetDesc = targetDesc.clone();
        }
        /* steps 15-16 */
        boolean extensibleTarget = IsExtensible(cx, target);
        /* steps 17-18 */
        PropertyDescriptor resultDesc = ToPropertyDescriptor(cx, trapResultObj);
        /* step 19 */
        CompletePropertyDescriptor(resultDesc);
        /* step 20 */
        boolean valid = IsCompatiblePropertyDescriptor(extensibleTarget, resultDesc, targetDesc);
        /* step 21 */
        if (!valid) {
            throw newTypeError(cx, Messages.Key.ProxyIncompatibleDescriptor);
        }
        /* step 22 */
        if (!resultDesc.isConfigurable()) {
            if (targetDesc == null || targetDesc.isConfigurable()) {
                throw newTypeError(cx, Messages.Key.ProxyAbsentOrConfigurable);
            }
        }
        /* step 23 */
        return resultDesc.toProperty();
    }

    /**
     * 9.5.6 [[DefineOwnProperty]] (P, Desc)
     */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, long propertyKey, PropertyDescriptor desc) {
        return defineOwnProperty(cx, ToString(propertyKey), desc);
    }

    /**
     * 9.5.6 [[DefineOwnProperty]] (P, Desc)
     */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, String propertyKey, PropertyDescriptor desc) {
        /* step 1 (implicit) */
        /* steps 2-4 */
        ScriptObject handler = getProxyHandler(cx);
        /* step 5 */
        ScriptObject target = getProxyTarget();
        /* steps 6-7 */
        Callable trap = GetMethod(cx, handler, "defineProperty");
        /* step 8 */
        if (trap == null) {
            return target.defineOwnProperty(cx, propertyKey, desc);
        }
        /* step 9 */
        Object descObj = FromPropertyDescriptor(cx, desc);
        /* steps 10-11 */
        boolean trapResult = ToBoolean(trap.call(cx, handler, target, propertyKey, descObj));
        /* step 12 */
        if (!trapResult) {
            return false;
        }
        /* steps 13-14 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 15-21 */
        return validateDefineOwnProperty(cx, desc, target, targetDesc);
    }

    /**
     * 9.5.6 [[DefineOwnProperty]] (P, Desc)
     */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, Symbol propertyKey, PropertyDescriptor desc) {
        /* step 1 (implicit) */
        /* steps 2-4 */
        ScriptObject handler = getProxyHandler(cx);
        /* step 5 */
        ScriptObject target = getProxyTarget();
        /* steps 6-7 */
        Callable trap = GetMethod(cx, handler, "defineProperty");
        /* step 8 */
        if (trap == null) {
            return target.defineOwnProperty(cx, propertyKey, desc);
        }
        /* step 9 */
        Object descObj = FromPropertyDescriptor(cx, desc);
        /* steps 10-11 */
        boolean trapResult = ToBoolean(trap.call(cx, handler, target, propertyKey, descObj));
        /* step 12 */
        if (!trapResult) {
            return false;
        }
        /* steps 13-14 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 15-21 */
        return validateDefineOwnProperty(cx, desc, target, targetDesc);
    }

    private boolean validateDefineOwnProperty(ExecutionContext cx, PropertyDescriptor desc, ScriptObject target,
            Property targetDesc) {
        if (targetDesc != null) {
            // need copy because of possible side-effects in IsExtensible()
            targetDesc = targetDesc.clone();
        }
        /* steps 15-16 */
        boolean extensibleTarget = IsExtensible(cx, target);
        /* steps 17-18 */
        boolean settingConfigFalse = desc.hasConfigurable() && !desc.isConfigurable();
        /* steps 19-20 */
        if (targetDesc == null) {
            if (!extensibleTarget) {
                throw newTypeError(cx, Messages.Key.ProxyAbsentNotExtensible);
            }
            if (settingConfigFalse) {
                throw newTypeError(cx, Messages.Key.ProxyAbsentOrConfigurable);
            }
        } else {
            if (!IsCompatiblePropertyDescriptor(extensibleTarget, desc, targetDesc)) {
                throw newTypeError(cx, Messages.Key.ProxyIncompatibleDescriptor);
            }
            if (settingConfigFalse && targetDesc.isConfigurable()) {
                throw newTypeError(cx, Messages.Key.ProxyAbsentOrConfigurable);
            }
        }
        /* step 21 */
        return true;
    }

    /**
     * 9.5.7 [[HasProperty]] (P)
     */
    @Override
    public boolean hasProperty(ExecutionContext cx, long propertyKey) {
        return hasProperty(cx, ToString(propertyKey));
    }

    /**
     * 9.5.7 [[HasProperty]] (P)
     */
    @Override
    public boolean hasProperty(ExecutionContext cx, String propertyKey) {
        /* step 1 (implicit) */
        /* steps 2-4 */
        ScriptObject handler = getProxyHandler(cx);
        /* step 5 */
        ScriptObject target = getProxyTarget();
        /* steps 6-7 */
        Callable trap = GetMethod(cx, handler, "has");
        /* step 8 */
        if (trap == null) {
            return target.hasProperty(cx, propertyKey);
        }
        /* steps 9-10 */
        boolean trapResult = ToBoolean(trap.call(cx, handler, target, propertyKey));
        /* step 11 */
        if (!trapResult) {
            /* steps 11.a-11.b */
            Property targetDesc = target.getOwnProperty(cx, propertyKey);
            /* step 11.c */
            validateHasProperty(cx, target, targetDesc);
        }
        /* step 12 */
        return trapResult;
    }

    /**
     * 9.5.7 [[HasProperty]] (P)
     */
    @Override
    public boolean hasProperty(ExecutionContext cx, Symbol propertyKey) {
        /* step 1 (implicit) */
        /* steps 2-4 */
        ScriptObject handler = getProxyHandler(cx);
        /* step 5 */
        ScriptObject target = getProxyTarget();
        /* steps 6-7 */
        Callable trap = GetMethod(cx, handler, "has");
        /* step 8 */
        if (trap == null) {
            return target.hasProperty(cx, propertyKey);
        }
        /* steps 9-10 */
        boolean booleanTrapResult = ToBoolean(trap.call(cx, handler, target, propertyKey));
        /* step 11 */
        if (!booleanTrapResult) {
            /* steps 11.a-11.b */
            Property targetDesc = target.getOwnProperty(cx, propertyKey);
            /* step 11.c */
            validateHasProperty(cx, target, targetDesc);
        }
        /* step 12 */
        return booleanTrapResult;
    }

    private void validateHasProperty(ExecutionContext cx, ScriptObject target, Property targetDesc) {
        /* step 11.c */
        if (targetDesc != null) {
            if (!targetDesc.isConfigurable()) {
                throw newTypeError(cx, Messages.Key.ProxyNotConfigurable);
            }
            boolean extensibleTarget = IsExtensible(cx, target);
            if (!extensibleTarget) {
                throw newTypeError(cx, Messages.Key.ProxyNotExtensible);
            }
        }
    }

    /**
     * 9.5.8 [[Get]] (P, Receiver)
     */
    @Override
    public Object get(ExecutionContext cx, long propertyKey, Object receiver) {
        return get(cx, ToString(propertyKey), receiver);
    }

    /**
     * 9.5.8 [[Get]] (P, Receiver)
     */
    @Override
    public Object get(ExecutionContext cx, String propertyKey, Object receiver) {
        /* step 1 (implicit) */
        /* steps 2-4 */
        ScriptObject handler = getProxyHandler(cx);
        /* step 5 */
        ScriptObject target = getProxyTarget();
        /* steps 6-7 */
        Callable trap = GetMethod(cx, handler, "get");
        /* step 8 */
        if (trap == null) {
            return target.get(cx, propertyKey, receiver);
        }
        /* steps 9-10 */
        Object trapResult = trap.call(cx, handler, target, propertyKey, receiver);
        /* steps 11-12 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 13-14 */
        return validateGet(cx, trapResult, targetDesc);
    }

    /**
     * 9.5.8 [[Get]] (P, Receiver)
     */
    @Override
    public Object get(ExecutionContext cx, Symbol propertyKey, Object receiver) {
        /* step 1 (implicit) */
        /* steps 2-4 */
        ScriptObject handler = getProxyHandler(cx);
        /* step 5 */
        ScriptObject target = getProxyTarget();
        /* steps 6-7 */
        Callable trap = GetMethod(cx, handler, "get");
        /* step 8 */
        if (trap == null) {
            return target.get(cx, propertyKey, receiver);
        }
        /* steps 9-10 */
        Object trapResult = trap.call(cx, handler, target, propertyKey, receiver);
        /* steps 11-12 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 13-14 */
        return validateGet(cx, trapResult, targetDesc);
    }

    private Object validateGet(ExecutionContext cx, Object trapResult, Property targetDesc) {
        /* step 13 */
        if (targetDesc != null && !targetDesc.isConfigurable()) {
            if (targetDesc.isDataDescriptor()) {
                if (!targetDesc.isWritable() && !SameValue(trapResult, targetDesc.getValue())) {
                    throw newTypeError(cx, Messages.Key.ProxySameValue);
                }
            } else {
                if (targetDesc.getGetter() == null && trapResult != UNDEFINED) {
                    throw newTypeError(cx, Messages.Key.ProxyNoGetter);
                }
            }
        }
        /* step 14 */
        return trapResult;
    }

    /**
     * 9.5.9 [[Set]] ( P, V, Receiver)
     */
    @Override
    public boolean set(ExecutionContext cx, long propertyKey, Object value, Object receiver) {
        return set(cx, ToString(propertyKey), value, receiver);
    }

    /**
     * 9.5.9 [[Set]] ( P, V, Receiver)
     */
    @Override
    public boolean set(ExecutionContext cx, String propertyKey, Object value, Object receiver) {
        /* step 1 (implicit) */
        /* steps 2-4 */
        ScriptObject handler = getProxyHandler(cx);
        /* step 5 */
        ScriptObject target = getProxyTarget();
        /* steps 6-7 */
        Callable trap = GetMethod(cx, handler, "set");
        /* step 8 */
        if (trap == null) {
            return target.set(cx, propertyKey, value, receiver);
        }
        /* steps 9-10 */
        boolean trapResult = ToBoolean(trap.call(cx, handler, target, propertyKey, value, receiver));
        /* step 11 */
        if (!trapResult) {
            return false;
        }
        /* steps 12-13 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 14-15 */
        return validateSet(cx, value, targetDesc);
    }

    /**
     * 9.5.9 [[Set]] ( P, V, Receiver)
     */
    @Override
    public boolean set(ExecutionContext cx, Symbol propertyKey, Object value, Object receiver) {
        /* step 1 (implicit) */
        /* steps 2-4 */
        ScriptObject handler = getProxyHandler(cx);
        /* step 5 */
        ScriptObject target = getProxyTarget();
        /* steps 6-7 */
        Callable trap = GetMethod(cx, handler, "set");
        /* step 8 */
        if (trap == null) {
            return target.set(cx, propertyKey, value, receiver);
        }
        /* steps 9-10 */
        boolean trapResult = ToBoolean(trap.call(cx, handler, target, propertyKey, value, receiver));
        /* step 11 */
        if (!trapResult) {
            return false;
        }
        /* steps 12-13 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 14-15 */
        return validateSet(cx, value, targetDesc);
    }

    private boolean validateSet(ExecutionContext cx, Object value, Property targetDesc) {
        /* step 14 */
        if (targetDesc != null && !targetDesc.isConfigurable()) {
            if (targetDesc.isDataDescriptor()) {
                if (!targetDesc.isWritable() && !SameValue(value, targetDesc.getValue())) {
                    throw newTypeError(cx, Messages.Key.ProxySameValue);
                }
            } else {
                if (targetDesc.getSetter() == null) {
                    throw newTypeError(cx, Messages.Key.ProxyNoSetter);
                }
            }
        }
        /* step 15 */
        return true;
    }

    /**
     * 9.5.10 [[Delete]] (P)
     */
    @Override
    public boolean delete(ExecutionContext cx, long propertyKey) {
        return delete(cx, ToString(propertyKey));
    }

    /**
     * 9.5.10 [[Delete]] (P)
     */
    @Override
    public boolean delete(ExecutionContext cx, String propertyKey) {
        /* step 1 (implicit) */
        /* steps 2-4 */
        ScriptObject handler = getProxyHandler(cx);
        /* step 5 */
        ScriptObject target = getProxyTarget();
        /* steps 6-7 */
        Callable trap = GetMethod(cx, handler, "deleteProperty");
        /* step 8 */
        if (trap == null) {
            return target.delete(cx, propertyKey);
        }
        /* steps 9-10 */
        boolean trapResult = ToBoolean(trap.call(cx, handler, target, propertyKey));
        /* step 11 */
        if (!trapResult) {
            return false;
        }
        /* steps 12-13 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 14-16 */
        return validateDelete(cx, targetDesc);
    }

    /**
     * 9.5.10 [[Delete]] (P)
     */
    @Override
    public boolean delete(ExecutionContext cx, Symbol propertyKey) {
        /* step 1 (implicit) */
        /* steps 2-4 */
        ScriptObject handler = getProxyHandler(cx);
        /* step 5 */
        ScriptObject target = getProxyTarget();
        /* steps 6-7 */
        Callable trap = GetMethod(cx, handler, "deleteProperty");
        /* step 8 */
        if (trap == null) {
            return target.delete(cx, propertyKey);
        }
        /* steps 9-10 */
        boolean trapResult = ToBoolean(trap.call(cx, handler, target, propertyKey));
        /* step 11 */
        if (!trapResult) {
            return false;
        }
        /* steps 12-13 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 14-16 */
        return validateDelete(cx, targetDesc);
    }

    private boolean validateDelete(ExecutionContext cx, Property targetDesc) {
        /* step 14 */
        if (targetDesc == null) {
            return true;
        }
        /* step 15 */
        if (!targetDesc.isConfigurable()) {
            throw newTypeError(cx, Messages.Key.ProxyDeleteNonConfigurable);
        }
        /* step 16 */
        return true;
    }

    /**
     * 9.5.11 [[Enumerate]] ()
     */
    @Override
    public ScriptObject enumerate(ExecutionContext cx) {
        /* steps 1-3 */
        ScriptObject handler = getProxyHandler(cx);
        /* step 4 */
        ScriptObject target = getProxyTarget();
        /* steps 5-6 */
        Callable trap = GetMethod(cx, handler, "enumerate");
        /* step 7 */
        if (trap == null) {
            return target.enumerate(cx);
        }
        /* steps 8-9 */
        Object trapResult = trap.call(cx, handler, target);
        /* step 10 */
        if (!Type.isObject(trapResult)) {
            throw newTypeError(cx, Messages.Key.ProxyNotObject);
        }
        /* step 11 */
        return Type.objectValue(trapResult);
    }

    /**
     * 9.5.11 [[Enumerate]] ()
     */
    @Override
    public ScriptIterator<?> enumerateKeys(ExecutionContext cx) {
        /* steps 1-3 */
        ScriptObject handler = getProxyHandler(cx);
        /* step 4 */
        ScriptObject target = getProxyTarget();
        /* steps 5-6 */
        Callable trap = GetMethod(cx, handler, "enumerate");
        /* step 7 */
        if (trap == null) {
            return target.enumerateKeys(cx);
        }
        /* steps 8-9 */
        Object trapResult = trap.call(cx, handler, target);
        /* step 10 */
        if (!Type.isObject(trapResult)) {
            throw newTypeError(cx, Messages.Key.ProxyNotObject);
        }
        /* step 11 */
        return ToScriptIterator(cx, Type.objectValue(trapResult));
    }

    /**
     * 9.5.12 [[OwnPropertyKeys]] ()
     */
    @Override
    public List<?> ownPropertyKeys(ExecutionContext cx) {
        /* steps 1-3 */
        ScriptObject handler = getProxyHandler(cx);
        /* step 4 */
        ScriptObject target = getProxyTarget();
        /* steps 5-6 */
        Callable trap = GetMethod(cx, handler, "ownKeys");
        /* step 7 */
        if (trap == null) {
            return target.ownPropertyKeys(cx);
        }
        /* step 8 */
        Object trapResultArray = trap.call(cx, handler, target);
        /* steps 9-10 */
        List<Object> trapResult = CreateListFromArrayLike(cx, trapResultArray, EnumSet.of(Type.String, Type.Symbol));
        /* steps 11-12 */
        boolean extensibleTarget = target.isExtensible(cx);
        /* steps 13-15 */
        List<?> targetKeys = target.ownPropertyKeys(cx);
        /* step 16 */
        ArrayList<Object> targetConfigurableKeys = new ArrayList<>();
        /* step 17 */
        ArrayList<Object> targetNonConfigurableKeys = new ArrayList<>();
        /* step 18 */
        for (Object key : targetKeys) {
            Property desc = target.getOwnProperty(cx, key);
            if (desc != null && !desc.isConfigurable()) {
                targetNonConfigurableKeys.add(key);
            } else {
                targetConfigurableKeys.add(key);
            }
        }
        /* step 19 */
        if (extensibleTarget && targetNonConfigurableKeys.isEmpty()) {
            return trapResult;
        }
        /* step 20 */
        final Integer zero = Integer.valueOf(0);
        HashMap<Object, Integer> uncheckedResultKeys = new HashMap<>();
        for (Object key : trapResult) {
            Integer c = uncheckedResultKeys.put(key, zero);
            if (c != null) {
                uncheckedResultKeys.put(key, c + 1);
            }
        }
        /* step 21 */
        for (Object key : targetNonConfigurableKeys) {
            Integer c = uncheckedResultKeys.remove(key);
            if (c == null) {
                throw newTypeError(cx, Messages.Key.ProxyNotConfigurable);
            }
            if (c > 0) {
                uncheckedResultKeys.put(key, c - 1);
            }
        }
        /* step 22 */
        if (extensibleTarget) {
            return trapResult;
        }
        /* step 23 */
        for (Object key : targetConfigurableKeys) {
            Integer c = uncheckedResultKeys.remove(key);
            if (c == null) {
                throw newTypeError(cx, Messages.Key.ProxyNotExtensible);
            }
            if (c > 0) {
                uncheckedResultKeys.put(key, c - 1);
            }
        }
        /* step 24 */
        if (!uncheckedResultKeys.isEmpty()) {
            throw newTypeError(cx, Messages.Key.ProxyAbsentNotExtensible);
        }
        /* step 25 */
        return trapResult;
    }

    /**
     * 9.5.12 [[OwnPropertyKeys]] ()
     */
    @Override
    public Iterator<?> ownKeys(ExecutionContext cx) {
        return ownPropertyKeys(cx).iterator();
    }
}
