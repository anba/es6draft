/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.language.CallOperations.PrepareForTailCall;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.PrivateName;
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
    private static final int PRIVATE_NAMES_DEFAULT_INITIAL_CAPACITY = 4;

    /** [[ProxyTarget]] */
    private ScriptObject proxyTarget;
    /** [[ProxyHandler]] */
    private ScriptObject proxyHandler;
    // Map for private names
    private final HashMap<PrivateName, Property> privateNames;

    /**
     * Constructs a new Proxy object.
     * 
     * @param target
     *            the proxy target object
     * @param handler
     *            the proxy handler object
     */
    public ProxyObject(ScriptObject target, ScriptObject handler) {
        assert target != null && handler != null;
        this.proxyTarget = target;
        this.proxyHandler = handler;
        this.privateNames = new HashMap<>(PRIVATE_NAMES_DEFAULT_INITIAL_CAPACITY);
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

    @Override
    public final Property get(PrivateName name) {
        return privateNames.get(name);
    }

    @Override
    public final void define(PrivateName name, Property property) {
        assert !privateNames.containsKey(name);
        privateNames.put(name, property);
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
        CallableProxyObject(ScriptObject target, ScriptObject handler) {
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
        public Realm getRealm(ExecutionContext cx) {
            /* 7.3.22 GetFunctionRealm ( obj ) */
            if (isRevoked()) {
                throw newTypeError(cx, Messages.Key.ProxyRevoked);
            }
            return ((Callable) getProxyTarget()).getRealm(cx);
        }

        @Override
        public String toSource(ExecutionContext cx) {
            if (cx.getRuntimeContext().isEnabled(CompatibilityOption.FunctionToString)) {
                return FunctionSource.nativeCode("");
            }
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
        ConstructorProxyObject(ScriptObject target, ScriptObject handler) {
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
        /* step 5 */
        Callable trap = GetMethod(cx, handler, "getPrototypeOf");
        /* step 6 */
        if (trap == null) {
            return target.getPrototypeOf(cx);
        }
        /* step 7 */
        Object handlerProto = trap.call(cx, handler, target);
        /* step 8 */
        if (!Type.isObjectOrNull(handlerProto)) {
            throw newTypeError(cx, Messages.Key.NotObjectOrNull);
        }
        ScriptObject handlerProtoObj = Type.objectValueOrNull(handlerProto);
        /* step 9 */
        boolean extensibleTarget = IsExtensible(cx, target);
        /* step 10 */
        if (extensibleTarget) {
            return handlerProtoObj;
        }
        /* step 11 */
        ScriptObject targetProto = target.getPrototypeOf(cx);
        /* step 12 */
        if (handlerProtoObj != targetProto) {
            throw newTypeError(cx, Messages.Key.ProxySamePrototype);
        }
        /* step 13 */
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
        /* step 6 */
        Callable trap = GetMethod(cx, handler, "setPrototypeOf");
        /* step 7 */
        if (trap == null) {
            return target.setPrototypeOf(cx, prototype);
        }
        /* step 8 */
        Object prototypeValue = prototype != null ? prototype : NULL;
        boolean trapResult = ToBoolean(trap.call(cx, handler, target, prototypeValue));
        /* step 9 */
        if (!trapResult) {
            return false;
        }
        /* step 10 */
        boolean extensibleTarget = IsExtensible(cx, target);
        /* step 11 */
        if (extensibleTarget) {
            return true;
        }
        /* step 12 */
        ScriptObject targetProto = target.getPrototypeOf(cx);
        /* step 13 */
        if (prototype != targetProto) {
            throw newTypeError(cx, Messages.Key.ProxySamePrototype);
        }
        /* step 14 */
        return true;
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
        /* step 5 */
        Callable trap = GetMethod(cx, handler, "isExtensible");
        /* step 6 */
        if (trap == null) {
            return target.isExtensible(cx);
        }
        /* step 7 */
        boolean trapResult = ToBoolean(trap.call(cx, handler, target));
        /* step 8 */
        boolean targetResult = target.isExtensible(cx);
        /* step 9 */
        if (trapResult != targetResult) {
            throw newTypeError(cx, Messages.Key.ProxyExtensible);
        }
        /* step 10 */
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
        /* step 5 */
        Callable trap = GetMethod(cx, handler, "preventExtensions");
        /* step 6 */
        if (trap == null) {
            return target.preventExtensions(cx);
        }
        /* step 7 */
        boolean trapResult = ToBoolean(trap.call(cx, handler, target));
        /* step 8 */
        if (trapResult) {
            boolean targetIsExtensible = target.isExtensible(cx);
            if (targetIsExtensible) {
                throw newTypeError(cx, Messages.Key.ProxyExtensible);
            }
        }
        /* step 9 */
        return trapResult;
    }

    /**
     * 9.5.5 [[GetOwnProperty]] (P)
     */
    @Override
    public Property getOwnProperty(ExecutionContext cx, long propertyKey) {
        /* step 1 (implicit) */
        /* steps 2-4 */
        ScriptObject handler = getProxyHandler(cx);
        /* step 5 */
        ScriptObject target = getProxyTarget();
        /* step 6 */
        Callable trap = GetMethod(cx, handler, "getOwnPropertyDescriptor");
        /* step 7 */
        if (trap == null) {
            return target.getOwnProperty(cx, propertyKey);
        }
        /* step 8 */
        Object trapResultObj = trap.call(cx, handler, target, ToString(propertyKey));
        /* step 9 */
        if (!(Type.isObject(trapResultObj) || Type.isUndefined(trapResultObj))) {
            throw newTypeError(cx, Messages.Key.ProxyNotObjectOrUndefined);
        }
        /* step 10 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 11-18 */
        return validateGetOwnProperty(cx, target, trapResultObj, targetDesc);
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
        /* step 6 */
        Callable trap = GetMethod(cx, handler, "getOwnPropertyDescriptor");
        /* step 7 */
        if (trap == null) {
            return target.getOwnProperty(cx, propertyKey);
        }
        /* step 8 */
        Object trapResultObj = trap.call(cx, handler, target, propertyKey);
        /* step 9 */
        if (!(Type.isObject(trapResultObj) || Type.isUndefined(trapResultObj))) {
            throw newTypeError(cx, Messages.Key.ProxyNotObjectOrUndefined);
        }
        /* step 10 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 11-18 */
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
        /* step 6 */
        Callable trap = GetMethod(cx, handler, "getOwnPropertyDescriptor");
        /* step 7 */
        if (trap == null) {
            return target.getOwnProperty(cx, propertyKey);
        }
        /* step 8 */
        Object trapResultObj = trap.call(cx, handler, target, propertyKey);
        /* step 9 */
        if (!(Type.isObject(trapResultObj) || Type.isUndefined(trapResultObj))) {
            throw newTypeError(cx, Messages.Key.ProxyNotObjectOrUndefined);
        }
        /* step 10 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 11-18 */
        return validateGetOwnProperty(cx, target, trapResultObj, targetDesc);
    }

    private Property validateGetOwnProperty(ExecutionContext cx, ScriptObject target, Object trapResultObj,
            Property targetDesc) {
        /* step 11 */
        if (Type.isUndefined(trapResultObj)) {
            /* step 11.a */
            if (targetDesc == null) {
                return null;
            }
            /* step 11.b */
            if (!targetDesc.isConfigurable()) {
                throw newTypeError(cx, Messages.Key.ProxyNotConfigurable);
            }
            /* steps 11.c-d */
            boolean extensibleTarget = IsExtensible(cx, target);
            /* step 11.e */
            if (!extensibleTarget) {
                throw newTypeError(cx, Messages.Key.ProxyNotExtensible);
            }
            /* step 11.f */
            return null;
        }
        if (targetDesc != null) {
            // need copy because of possible side-effects in IsExtensible()
            targetDesc = targetDesc.clone();
        }
        /* step 12 */
        boolean extensibleTarget = IsExtensible(cx, target);
        /* step 13 */
        PropertyDescriptor resultDesc = ToPropertyDescriptor(cx, trapResultObj);
        /* step 14 */
        CompletePropertyDescriptor(resultDesc);
        /* step 15 */
        boolean valid = IsCompatiblePropertyDescriptor(extensibleTarget, resultDesc, targetDesc);
        /* step 16 */
        if (!valid) {
            throw newTypeError(cx, Messages.Key.ProxyIncompatibleDescriptor);
        }
        /* step 17 */
        if (!resultDesc.isConfigurable()) {
            /* step 17.a */
            if (targetDesc == null || targetDesc.isConfigurable()) {
                throw newTypeError(cx, Messages.Key.ProxyAbsentOrConfigurable);
            }
        }
        /* step 18 */
        return resultDesc.toProperty();
    }

    /**
     * 9.5.6 [[DefineOwnProperty]] (P, Desc)
     */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, long propertyKey, PropertyDescriptor desc) {
        /* step 1 (implicit) */
        /* steps 2-4 */
        ScriptObject handler = getProxyHandler(cx);
        /* step 5 */
        ScriptObject target = getProxyTarget();
        /* step 6 */
        Callable trap = GetMethod(cx, handler, "defineProperty");
        /* step 7 */
        if (trap == null) {
            return target.defineOwnProperty(cx, propertyKey, desc);
        }
        /* step 8 */
        Object descObj = FromPropertyDescriptor(cx, desc);
        /* step 9 */
        boolean trapResult = ToBoolean(trap.call(cx, handler, target, ToString(propertyKey), descObj));
        /* step 10 */
        if (!trapResult) {
            return false;
        }
        /* step 11 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 12-17 */
        return validateDefineOwnProperty(cx, desc, target, targetDesc);
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
        /* step 6 */
        Callable trap = GetMethod(cx, handler, "defineProperty");
        /* step 7 */
        if (trap == null) {
            return target.defineOwnProperty(cx, propertyKey, desc);
        }
        /* step 8 */
        Object descObj = FromPropertyDescriptor(cx, desc);
        /* step 9 */
        boolean trapResult = ToBoolean(trap.call(cx, handler, target, propertyKey, descObj));
        /* step 10 */
        if (!trapResult) {
            return false;
        }
        /* step 11 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 12-17 */
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
        /* step 6 */
        Callable trap = GetMethod(cx, handler, "defineProperty");
        /* step 7 */
        if (trap == null) {
            return target.defineOwnProperty(cx, propertyKey, desc);
        }
        /* step 8 */
        Object descObj = FromPropertyDescriptor(cx, desc);
        /* step 9 */
        boolean trapResult = ToBoolean(trap.call(cx, handler, target, propertyKey, descObj));
        /* step 10 */
        if (!trapResult) {
            return false;
        }
        /* step 11 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 12-17 */
        return validateDefineOwnProperty(cx, desc, target, targetDesc);
    }

    private boolean validateDefineOwnProperty(ExecutionContext cx, PropertyDescriptor desc, ScriptObject target,
            Property targetDesc) {
        if (targetDesc != null) {
            // need copy because of possible side-effects in IsExtensible()
            targetDesc = targetDesc.clone();
        }
        /* step 12 */
        boolean extensibleTarget = IsExtensible(cx, target);
        /* steps 13-14 */
        boolean settingConfigFalse = desc.hasConfigurable() && !desc.isConfigurable();
        /* steps 15-16 */
        if (targetDesc == null) {
            /* step 15.a */
            if (!extensibleTarget) {
                throw newTypeError(cx, Messages.Key.ProxyAbsentNotExtensible);
            }
            /* step 15.b */
            if (settingConfigFalse) {
                throw newTypeError(cx, Messages.Key.ProxyAbsentOrConfigurable);
            }
        } else {
            /* step 16.a */
            if (!IsCompatiblePropertyDescriptor(extensibleTarget, desc, targetDesc)) {
                throw newTypeError(cx, Messages.Key.ProxyIncompatibleDescriptor);
            }
            /* step 16.b */
            if (settingConfigFalse && targetDesc.isConfigurable()) {
                throw newTypeError(cx, Messages.Key.ProxyAbsentOrConfigurable);
            }
        }
        /* step 17 */
        return true;
    }

    /**
     * 9.5.7 [[HasProperty]] (P)
     */
    @Override
    public boolean hasProperty(ExecutionContext cx, long propertyKey) {
        /* step 1 (implicit) */
        /* steps 2-4 */
        ScriptObject handler = getProxyHandler(cx);
        /* step 5 */
        ScriptObject target = getProxyTarget();
        /* step 6 */
        Callable trap = GetMethod(cx, handler, "has");
        /* step 7 */
        if (trap == null) {
            return target.hasProperty(cx, propertyKey);
        }
        /* step 8 */
        boolean trapResult = ToBoolean(trap.call(cx, handler, target, ToString(propertyKey)));
        /* step 9 */
        if (!trapResult) {
            /* step 9.a */
            Property targetDesc = target.getOwnProperty(cx, propertyKey);
            /* step 9.b */
            validateHasProperty(cx, target, targetDesc);
        }
        /* step 10 */
        return trapResult;
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
        /* step 6 */
        Callable trap = GetMethod(cx, handler, "has");
        /* step 7 */
        if (trap == null) {
            return target.hasProperty(cx, propertyKey);
        }
        /* step 8 */
        boolean trapResult = ToBoolean(trap.call(cx, handler, target, propertyKey));
        /* step 9 */
        if (!trapResult) {
            /* step 9.a */
            Property targetDesc = target.getOwnProperty(cx, propertyKey);
            /* step 9.b */
            validateHasProperty(cx, target, targetDesc);
        }
        /* step 10 */
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
        /* step 6 */
        Callable trap = GetMethod(cx, handler, "has");
        /* step 7 */
        if (trap == null) {
            return target.hasProperty(cx, propertyKey);
        }
        /* step 8 */
        boolean booleanTrapResult = ToBoolean(trap.call(cx, handler, target, propertyKey));
        /* step 9 */
        if (!booleanTrapResult) {
            /* step 9.a */
            Property targetDesc = target.getOwnProperty(cx, propertyKey);
            /* step 9.b */
            validateHasProperty(cx, target, targetDesc);
        }
        /* step 10 */
        return booleanTrapResult;
    }

    private void validateHasProperty(ExecutionContext cx, ScriptObject target, Property targetDesc) {
        /* step 9.b */
        if (targetDesc != null) {
            /* step 9.b.i */
            if (!targetDesc.isConfigurable()) {
                throw newTypeError(cx, Messages.Key.ProxyNotConfigurable);
            }
            /* step 9.b.ii */
            boolean extensibleTarget = IsExtensible(cx, target);
            /* step 9.b.iii */
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
        /* step 1 (implicit) */
        /* steps 2-4 */
        ScriptObject handler = getProxyHandler(cx);
        /* step 5 */
        ScriptObject target = getProxyTarget();
        /* step 6 */
        Callable trap = GetMethod(cx, handler, "get");
        /* step 7 */
        if (trap == null) {
            return target.get(cx, propertyKey, receiver);
        }
        /* step 8 */
        Object trapResult = trap.call(cx, handler, target, ToString(propertyKey), receiver);
        /* step 9 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 10-11 */
        return validateGet(cx, trapResult, targetDesc);
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
        /* step 6 */
        Callable trap = GetMethod(cx, handler, "get");
        /* step 7 */
        if (trap == null) {
            return target.get(cx, propertyKey, receiver);
        }
        /* step 8 */
        Object trapResult = trap.call(cx, handler, target, propertyKey, receiver);
        /* step 9 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 10-11 */
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
        /* step 6 */
        Callable trap = GetMethod(cx, handler, "get");
        /* step 7 */
        if (trap == null) {
            return target.get(cx, propertyKey, receiver);
        }
        /* step 8 */
        Object trapResult = trap.call(cx, handler, target, propertyKey, receiver);
        /* step 9 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 10-11 */
        return validateGet(cx, trapResult, targetDesc);
    }

    private Object validateGet(ExecutionContext cx, Object trapResult, Property targetDesc) {
        /* step 10 */
        if (targetDesc != null && !targetDesc.isConfigurable()) {
            if (targetDesc.isDataDescriptor()) {
                /* step 10.a */
                if (!targetDesc.isWritable() && !SameValue(trapResult, targetDesc.getValue())) {
                    throw newTypeError(cx, Messages.Key.ProxySameValue);
                }
            } else {
                /* step 10.b */
                if (targetDesc.getGetter() == null && trapResult != UNDEFINED) {
                    throw newTypeError(cx, Messages.Key.ProxyNoGetter);
                }
            }
        }
        /* step 11 */
        return trapResult;
    }

    /**
     * 9.5.9 [[Set]] ( P, V, Receiver)
     */
    @Override
    public boolean set(ExecutionContext cx, long propertyKey, Object value, Object receiver) {
        /* step 1 (implicit) */
        /* steps 2-4 */
        ScriptObject handler = getProxyHandler(cx);
        /* step 5 */
        ScriptObject target = getProxyTarget();
        /* step 6 */
        Callable trap = GetMethod(cx, handler, "set");
        /* step 7 */
        if (trap == null) {
            return target.set(cx, propertyKey, value, receiver);
        }
        /* step 8 */
        boolean trapResult = ToBoolean(trap.call(cx, handler, target, ToString(propertyKey), value, receiver));
        /* step 9 */
        if (!trapResult) {
            return false;
        }
        /* step 10 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 11-12 */
        return validateSet(cx, value, targetDesc);
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
        /* step 6 */
        Callable trap = GetMethod(cx, handler, "set");
        /* step 7 */
        if (trap == null) {
            return target.set(cx, propertyKey, value, receiver);
        }
        /* step 8 */
        boolean trapResult = ToBoolean(trap.call(cx, handler, target, propertyKey, value, receiver));
        /* step 9 */
        if (!trapResult) {
            return false;
        }
        /* step 10 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 11-12 */
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
        /* step 6 */
        Callable trap = GetMethod(cx, handler, "set");
        /* step 7 */
        if (trap == null) {
            return target.set(cx, propertyKey, value, receiver);
        }
        /* step 8 */
        boolean trapResult = ToBoolean(trap.call(cx, handler, target, propertyKey, value, receiver));
        /* step 9 */
        if (!trapResult) {
            return false;
        }
        /* step 10 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 11-12 */
        return validateSet(cx, value, targetDesc);
    }

    private boolean validateSet(ExecutionContext cx, Object value, Property targetDesc) {
        /* step 11 */
        if (targetDesc != null && !targetDesc.isConfigurable()) {
            if (targetDesc.isDataDescriptor()) {
                /* step 11.a */
                if (!targetDesc.isWritable() && !SameValue(value, targetDesc.getValue())) {
                    throw newTypeError(cx, Messages.Key.ProxySameValue);
                }
            } else {
                /* step 11.b */
                if (targetDesc.getSetter() == null) {
                    throw newTypeError(cx, Messages.Key.ProxyNoSetter);
                }
            }
        }
        /* step 12 */
        return true;
    }

    /**
     * 9.5.10 [[Delete]] (P)
     */
    @Override
    public boolean delete(ExecutionContext cx, long propertyKey) {
        /* step 1 (implicit) */
        /* steps 2-4 */
        ScriptObject handler = getProxyHandler(cx);
        /* step 5 */
        ScriptObject target = getProxyTarget();
        /* step 6 */
        Callable trap = GetMethod(cx, handler, "deleteProperty");
        /* step 7 */
        if (trap == null) {
            return target.delete(cx, propertyKey);
        }
        /* step 8 */
        boolean trapResult = ToBoolean(trap.call(cx, handler, target, ToString(propertyKey)));
        /* step 9 */
        if (!trapResult) {
            return false;
        }
        /* step 10 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 11-13 */
        return validateDelete(cx, targetDesc);
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
        /* step 6 */
        Callable trap = GetMethod(cx, handler, "deleteProperty");
        /* step 7 */
        if (trap == null) {
            return target.delete(cx, propertyKey);
        }
        /* step 8 */
        boolean trapResult = ToBoolean(trap.call(cx, handler, target, propertyKey));
        /* step 9 */
        if (!trapResult) {
            return false;
        }
        /* step 10 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 11-13 */
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
        /* step 6 */
        Callable trap = GetMethod(cx, handler, "deleteProperty");
        /* step 7 */
        if (trap == null) {
            return target.delete(cx, propertyKey);
        }
        /* step 8 */
        boolean trapResult = ToBoolean(trap.call(cx, handler, target, propertyKey));
        /* step 9 */
        if (!trapResult) {
            return false;
        }
        /* step 10 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 11-13 */
        return validateDelete(cx, targetDesc);
    }

    private boolean validateDelete(ExecutionContext cx, Property targetDesc) {
        /* step 11 */
        if (targetDesc == null) {
            return true;
        }
        /* step 12 */
        if (!targetDesc.isConfigurable()) {
            throw newTypeError(cx, Messages.Key.ProxyDeleteNonConfigurable);
        }
        /* step 13 */
        return true;
    }

    /**
     * 9.5.11 [[OwnPropertyKeys]] ()
     */
    @Override
    public List<?> ownPropertyKeys(ExecutionContext cx) {
        /* steps 1-3 */
        ScriptObject handler = getProxyHandler(cx);
        /* step 4 */
        ScriptObject target = getProxyTarget();
        /* step 5 */
        Callable trap = GetMethod(cx, handler, "ownKeys");
        /* step 6 */
        if (trap == null) {
            return target.ownPropertyKeys(cx);
        }
        /* step 7 */
        Object trapResultArray = trap.call(cx, handler, target);
        /* steps 8-21 */
        return validateOwnPropertyKeys(cx, target, trapResultArray);
    }

    @Override
    public List<String> ownPropertyNames(ExecutionContext cx) {
        /* steps 1-3 */
        ScriptObject handler = getProxyHandler(cx);
        /* step 4 */
        ScriptObject target = getProxyTarget();
        /* step 5 */
        Callable trap = GetMethod(cx, handler, "ownKeys");
        /* step 6 */
        if (trap == null) {
            return target.ownPropertyNames(cx);
        }
        /* step 7 */
        Object trapResultArray = trap.call(cx, handler, target);
        /* steps 8-21 */
        return stringProperties(validateOwnPropertyKeys(cx, target, trapResultArray));
    }

    @Override
    public List<Symbol> ownPropertySymbols(ExecutionContext cx) {
        /* steps 1-3 */
        ScriptObject handler = getProxyHandler(cx);
        /* step 4 */
        ScriptObject target = getProxyTarget();
        /* step 5 */
        Callable trap = GetMethod(cx, handler, "ownKeys");
        /* step 6 */
        if (trap == null) {
            return target.ownPropertySymbols(cx);
        }
        /* step 7 */
        Object trapResultArray = trap.call(cx, handler, target);
        /* steps 8-21 */
        return symbolProperties(validateOwnPropertyKeys(cx, target, trapResultArray));
    }

    /**
     * 9.5.11 [[OwnPropertyKeys]] ()
     */
    @Override
    public Iterator<String> ownEnumerablePropertyKeys(ExecutionContext cx) {
        /* steps 1-3 */
        ScriptObject handler = getProxyHandler(cx);
        /* step 4 */
        ScriptObject target = getProxyTarget();
        /* step 5 */
        Callable trap = GetMethod(cx, handler, "ownKeys");
        /* step 6 */
        if (trap == null) {
            return target.ownEnumerablePropertyKeys(cx);
        }
        /* step 7 */
        Object trapResultArray = trap.call(cx, handler, target);
        /* steps 8-21 */
        return stringProperties(validateOwnPropertyKeys(cx, target, trapResultArray)).iterator();
    }

    private List<String> stringProperties(List<?> ownKeys) {
        ArrayList<String> list = new ArrayList<>();
        for (Object key : ownKeys) {
            if (key instanceof String) {
                list.add((String) key);
            }
        }
        return list;
    }

    private List<Symbol> symbolProperties(List<?> ownKeys) {
        ArrayList<Symbol> list = new ArrayList<>();
        for (Object key : ownKeys) {
            if (key instanceof Symbol) {
                list.add((Symbol) key);
            }
        }
        return list;
    }

    private List<Object> validateOwnPropertyKeys(ExecutionContext cx, ScriptObject target, Object trapResultArray) {
        /* step 8 */
        List<Object> trapResult = CreateListFromArrayLike(cx, trapResultArray, EnumSet.of(Type.String, Type.Symbol));
        /* steps 9, 16 */
        HashSet<Object> uncheckedResultKeys = new HashSet<>(trapResult);
        if (trapResult.size() != uncheckedResultKeys.size()) {
            throw newTypeError(cx, Messages.Key.ProxyDuplicateKeys);
        }
        /* step 10 */
        boolean extensibleTarget = target.isExtensible(cx);
        /* steps 11-13 */
        List<?> targetKeys = target.ownPropertyKeys(cx);
        /* step 14 */
        ArrayList<Object> targetConfigurableKeys = new ArrayList<>();
        /* step 15 */
        ArrayList<Object> targetNonConfigurableKeys = new ArrayList<>();
        /* step 16 */
        for (Object key : targetKeys) {
            /* step 16.a */
            Property desc = target.getOwnProperty(cx, key);
            /* steps 16.b-c */
            if (desc != null && !desc.isConfigurable()) {
                /* step 16.b */
                targetNonConfigurableKeys.add(key);
            } else if (!extensibleTarget) {
                /* step 16.c */
                targetConfigurableKeys.add(key);
            }
        }
        /* step 17 */
        if (extensibleTarget && targetNonConfigurableKeys.isEmpty()) {
            return trapResult;
        }
        /* step 19 */
        for (Object key : targetNonConfigurableKeys) {
            if (!uncheckedResultKeys.remove(key)) {
                throw newTypeError(cx, Messages.Key.ProxyNotConfigurable);
            }
        }
        /* step 20 */
        if (extensibleTarget) {
            return trapResult;
        }
        /* step 21 */
        for (Object key : targetConfigurableKeys) {
            if (!uncheckedResultKeys.remove(key)) {
                throw newTypeError(cx, Messages.Key.ProxyNotExtensible);
            }
        }
        /* step 22 */
        if (!uncheckedResultKeys.isEmpty()) {
            throw newTypeError(cx, Messages.Key.ProxyAbsentNotExtensible);
        }
        /* step 21 */
        return trapResult;
    }
}
