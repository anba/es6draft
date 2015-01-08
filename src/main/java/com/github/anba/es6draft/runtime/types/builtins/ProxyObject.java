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
import static com.github.anba.es6draft.runtime.objects.internal.ListIterator.FromScriptIterator;
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
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ScriptIterator;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Null;
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
     * Revoke this proxy, that means set both, [[ProxyTarget]] and [[ProxyHandler]], to {@code null}
     * and by that prevent further operations on this proxy object.
     */
    public final void revoke() {
        assert !isRevoked() : "Proxy already revoked";
        this.proxyHandler = null;
        this.proxyTarget = null;
    }

    /**
     * Repeatedly unwraps the proxy object and returns the underlying target object.
     * 
     * @return the proxy target object or {@code null} if the proxy has been revoked
     */
    public final ScriptObject unwrap() {
        ScriptObject target;
        for (ProxyObject proxy = this; (target = proxy.proxyTarget) instanceof ProxyObject;) {
            proxy = (ProxyObject) target;
        }
        return target;
    }

    @Override
    public String toString() {
        return String.format("%s@%x {{%n\tTarget=%s%n\tHandler=%s%n}}", getClass().getSimpleName(),
                System.identityHashCode(this), proxyTarget, proxyHandler);
    }

    private static class CallabeProxyObject extends ProxyObject implements Callable {
        /**
         * Constructs a new Proxy object.
         * 
         * @param target
         *            the proxy target object
         * @param handler
         *            the proxy handler object
         */
        public CallabeProxyObject(ScriptObject target, ScriptObject handler) {
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
        public Object tailCall(ExecutionContext callerContext, Object thisValue, Object... args)
                throws Throwable {
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
            /* 7.3.21 GetFunctionRealm ( obj ) Abstract Operation */
            if (isRevoked()) {
                return cx.getRealm();
            }
            return ((Callable) getProxyTarget()).getRealm(cx);
        }

        @Override
        public String toSource(SourceSelector selector) {
            if (isRevoked()) {
                return FunctionSource.noSource(selector);
            }
            return ((Callable) getProxyTarget()).toSource(selector);
        }
    }

    private static class ConstructorProxyObject extends CallabeProxyObject implements Constructor {
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
        public ScriptObject construct(ExecutionContext callerContext, Object... args) {
            /* steps 1-3 */
            ScriptObject handler = getProxyHandler(callerContext);
            /* step 4 */
            ScriptObject target = getProxyTarget();
            /* steps 5-6 */
            Callable trap = GetMethod(callerContext, handler, "construct");
            /* step 7 */
            if (trap == null) {
                return ((Constructor) target).construct(callerContext, args);
            }
            /* step 8 */
            ArrayObject argArray = CreateArrayFromList(callerContext, Arrays.asList(args));
            /* steps 9-10 */
            Object newObj = trap.call(callerContext, handler, target, argArray);
            /* step 11 */
            if (!Type.isObject(newObj)) {
                throw newTypeError(callerContext, Messages.Key.NotObjectType);
            }
            /* step 12 */
            return Type.objectValue(newObj);
        }

        /**
         * 9.5.14 [[Construct]] Internal Method
         */
        @Override
        public Object tailConstruct(ExecutionContext callerContext, Object... args)
                throws Throwable {
            /* steps 1-3 */
            ScriptObject handler = getProxyHandler(callerContext);
            /* step 4 */
            ScriptObject target = getProxyTarget();
            /* steps 5-6 */
            Callable trap = GetMethod(callerContext, handler, "construct");
            /* step 7 */
            if (trap == null) {
                return ((Constructor) target).tailConstruct(callerContext, args);
            }
            /* step 8 */
            ArrayObject argArray = CreateArrayFromList(callerContext, Arrays.asList(args));
            /* steps 9-10 */
            Object newObj = trap.call(callerContext, handler, target, argArray);
            /* step 11 */
            if (!Type.isObject(newObj)) {
                throw newTypeError(callerContext, Messages.Key.NotObjectType);
            }
            /* step 12 */
            return Type.objectValue(newObj);
        }
    }

    /**
     * 9.5.15 ProxyCreate(target, handler) Abstract Operation
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
        /* step 2 */
        if (!Type.isObject(handler)) {
            throw newTypeError(cx, Messages.Key.NotObjectType);
        }
        ScriptObject proxyTarget = Type.objectValue(target);
        ScriptObject proxyHandler = Type.objectValue(handler);
        /* steps 3-7 */
        ProxyObject proxy;
        if (IsCallable(proxyTarget)) {
            if (IsConstructor(proxyTarget)) {
                proxy = new ConstructorProxyObject(proxyTarget, proxyHandler);
            } else {
                proxy = new CallabeProxyObject(proxyTarget, proxyHandler);
            }
        } else {
            proxy = new ProxyObject(proxyTarget, proxyHandler);
        }
        /* step 8 */
        return proxy;
    }

    /**
     * Java {@code null} to {@link Null#NULL}.
     * 
     * @param value
     *            the value
     * @return either <var>value</var> or {@link Null#NULL}
     */
    private static Object maskNull(Object value) {
        return value != null ? value : NULL;
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
        ScriptObject handlerProto_ = Type.objectValueOrNull(handlerProto);
        /* steps 11-12 */
        boolean extensibleTarget = IsExtensible(cx, target);
        /* step 13 */
        if (extensibleTarget) {
            return handlerProto_;
        }
        /* steps 14-15 */
        ScriptObject targetProto = target.getPrototypeOf(cx);
        /* step 16 */
        if (!SameValue(handlerProto_, targetProto)) {
            throw newTypeError(cx, Messages.Key.ProxySamePrototype);
        }
        /* step 17 */
        return handlerProto_;
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
        /* step 8 */
        Object trapResult = trap.call(cx, handler, target, maskNull(prototype));
        /* steps 9-10 */
        boolean booleanTrapResult = ToBoolean(trapResult);
        /* steps 11-12 */
        boolean extensibleTarget = IsExtensible(cx, target);
        /* step 13 */
        if (extensibleTarget) {
            return booleanTrapResult;
        }
        /* steps 14-15 */
        ScriptObject targetProto = target.getPrototypeOf(cx);
        /* step 16 */
        if (booleanTrapResult && !SameValue(prototype, targetProto)) {
            throw newTypeError(cx, Messages.Key.ProxySamePrototype);
        }
        /* step 17 */
        return booleanTrapResult;
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
        /* step 8 */
        Object trapResult = trap.call(cx, handler, target);
        /* steps 9-10 */
        boolean booleanTrapResult = ToBoolean(trapResult);
        /* steps 11-12 */
        boolean targetResult = target.isExtensible(cx);
        /* step 13 */
        if (booleanTrapResult != targetResult) {
            throw newTypeError(cx, Messages.Key.ProxyExtensible);
        }
        /* step 14 */
        return booleanTrapResult;
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
        /* step 8 */
        Object trapResult = trap.call(cx, handler, target);
        /* steps 9-10 */
        boolean booleanTrapResult = ToBoolean(trapResult);
        /* step 11 */
        if (booleanTrapResult) {
            boolean targetIsExtensible = target.isExtensible(cx);
            if (targetIsExtensible) {
                throw newTypeError(cx, Messages.Key.ProxyExtensible);
            }
        }
        /* step 12 */
        return booleanTrapResult;
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

    private Property validateGetOwnProperty(ExecutionContext cx, ScriptObject target,
            Object trapResultObj, Property targetDesc) {
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
    public boolean defineOwnProperty(ExecutionContext cx, String propertyKey,
            PropertyDescriptor desc) {
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
        /* step 10 */
        Object trapResult = trap.call(cx, handler, target, propertyKey, descObj);
        /* steps 11-12 */
        boolean booleanTrapResult = ToBoolean(trapResult);
        /* step 13 */
        if (!booleanTrapResult) {
            return false;
        }
        /* steps 14-15 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 16-22 */
        return validateDefineOwnProperty(cx, desc, target, targetDesc);
    }

    /**
     * 9.5.6 [[DefineOwnProperty]] (P, Desc)
     */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, Symbol propertyKey,
            PropertyDescriptor desc) {
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
        /* step 10 */
        Object trapResult = trap.call(cx, handler, target, propertyKey, descObj);
        /* steps 11-12 */
        boolean booleanTrapResult = ToBoolean(trapResult);
        /* step 13 */
        if (!booleanTrapResult) {
            return false;
        }
        /* steps 14-15 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 16-22 */
        return validateDefineOwnProperty(cx, desc, target, targetDesc);
    }

    private boolean validateDefineOwnProperty(ExecutionContext cx, PropertyDescriptor desc,
            ScriptObject target, Property targetDesc) {
        if (targetDesc != null) {
            // need copy because of possible side-effects in IsExtensible()
            targetDesc = targetDesc.clone();
        }
        /* steps 16-17 */
        boolean extensibleTarget = IsExtensible(cx, target);
        /* steps 18-19 */
        boolean settingConfigFalse = desc.hasConfigurable() && !desc.isConfigurable();
        /* steps 20-21 */
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
        /* step 22 */
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
        /* step 9 */
        Object trapResult = trap.call(cx, handler, target, propertyKey);
        /* steps 10-11 */
        boolean booleanTrapResult = ToBoolean(trapResult);
        /* step 12 */
        if (!booleanTrapResult) {
            /* steps 12.a-12.b */
            Property targetDesc = target.getOwnProperty(cx, propertyKey);
            /* step 12.c */
            validateHasProperty(cx, target, targetDesc);
        }
        /* step 13 */
        return booleanTrapResult;
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
        /* step 9 */
        Object trapResult = trap.call(cx, handler, target, propertyKey);
        /* steps 10-11 */
        boolean booleanTrapResult = ToBoolean(trapResult);
        /* step 12 */
        if (!booleanTrapResult) {
            /* steps 12.a-12.b */
            Property targetDesc = target.getOwnProperty(cx, propertyKey);
            /* step 12.c */
            validateHasProperty(cx, target, targetDesc);
        }
        /* step 13 */
        return booleanTrapResult;
    }

    private void validateHasProperty(ExecutionContext cx, ScriptObject target, Property targetDesc) {
        /* step 12.c */
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
        if (targetDesc != null) {
            if (targetDesc.isDataDescriptor() && !targetDesc.isConfigurable()
                    && !targetDesc.isWritable()) {
                if (!SameValue(trapResult, targetDesc.getValue())) {
                    throw newTypeError(cx, Messages.Key.ProxySameValue);
                }
            }
            if (targetDesc.isAccessorDescriptor() && !targetDesc.isConfigurable()
                    && targetDesc.getGetter() == null) {
                if (trapResult != UNDEFINED) {
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
        /* step 9 */
        Object trapResult = trap.call(cx, handler, target, propertyKey, value, receiver);
        /* steps 10-11 */
        boolean booleanTrapResult = ToBoolean(trapResult);
        /* step 12 */
        if (!booleanTrapResult) {
            return false;
        }
        /* steps 13-14 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 15-16 */
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
        /* step 9 */
        Object trapResult = trap.call(cx, handler, target, propertyKey, value, receiver);
        /* steps 10-11 */
        boolean booleanTrapResult = ToBoolean(trapResult);
        /* step 12 */
        if (!booleanTrapResult) {
            return false;
        }
        /* steps 13-14 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 15-16 */
        return validateSet(cx, value, targetDesc);
    }

    private boolean validateSet(ExecutionContext cx, Object value, Property targetDesc) {
        /* step 15 */
        if (targetDesc != null) {
            if (targetDesc.isDataDescriptor() && !targetDesc.isConfigurable()
                    && !targetDesc.isWritable()) {
                if (!SameValue(value, targetDesc.getValue())) {
                    throw newTypeError(cx, Messages.Key.ProxySameValue);
                }
            }
            if (targetDesc.isAccessorDescriptor() && !targetDesc.isConfigurable()) {
                if (targetDesc.getSetter() == null) {
                    throw newTypeError(cx, Messages.Key.ProxyNoSetter);
                }
            }
        }
        /* step 16 */
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
        /* step 9 */
        Object trapResult = trap.call(cx, handler, target, propertyKey);
        /* steps 10-11 */
        boolean booleanTrapResult = ToBoolean(trapResult);
        /* step 12 */
        if (!booleanTrapResult) {
            return false;
        }
        /* steps 13-14 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 15-17 */
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
        /* step 9 */
        Object trapResult = trap.call(cx, handler, target, propertyKey);
        /* steps 10-11 */
        boolean booleanTrapResult = ToBoolean(trapResult);
        /* step 12 */
        if (!booleanTrapResult) {
            return false;
        }
        /* steps 13-14 */
        Property targetDesc = target.getOwnProperty(cx, propertyKey);
        /* steps 15-17 */
        return validateDelete(cx, targetDesc);
    }

    private boolean validateDelete(ExecutionContext cx, Property targetDesc) {
        /* step 15 */
        if (targetDesc == null) {
            return true;
        }
        /* step 16 */
        if (!targetDesc.isConfigurable()) {
            throw newTypeError(cx, Messages.Key.ProxyDeleteNonConfigurable);
        }
        /* step 17 */
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
        return FromScriptIterator(cx, enumerate(cx));
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
        List<Object> trapResult = CreateListFromArrayLike(cx, trapResultArray,
                EnumSet.of(Type.String, Type.Symbol));
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
            Property desc;
            if (key instanceof String) {
                desc = target.getOwnProperty(cx, (String) key);
            } else {
                desc = target.getOwnProperty(cx, (Symbol) key);
            }
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
        HashSet<Object> uncheckedResultKeys = new HashSet<>();
        HashMap<Object, Integer> uncheckedDuplicateKeys = null;
        for (Object key : trapResult) {
            if (!uncheckedResultKeys.add(key)) {
                // Duplicate key in result set
                if (uncheckedDuplicateKeys == null) {
                    uncheckedDuplicateKeys = new HashMap<>();
                }
                Integer count = uncheckedDuplicateKeys.get(key);
                uncheckedDuplicateKeys.put(key, (count != null ? count + 1 : 1));
            }
        }
        /* steps 21 */
        for (Object key : targetNonConfigurableKeys) {
            if (!uncheckedResultKeys.remove(key)) {
                throw newTypeError(cx, Messages.Key.ProxyNotConfigurable);
            }
            if (uncheckedDuplicateKeys != null) {
                updateUncheckedWithDuplicates(uncheckedResultKeys, uncheckedDuplicateKeys, key);
            }
        }
        /* step 22 */
        if (extensibleTarget) {
            return trapResult;
        }
        /* step 23 */
        for (Object key : targetConfigurableKeys) {
            if (!uncheckedResultKeys.remove(key)) {
                throw newTypeError(cx, Messages.Key.ProxyNotExtensible);
            }
            if (uncheckedDuplicateKeys != null) {
                updateUncheckedWithDuplicates(uncheckedResultKeys, uncheckedDuplicateKeys, key);
            }
        }
        /* step 24 */
        if (!uncheckedResultKeys.isEmpty()) {
            throw newTypeError(cx, Messages.Key.ProxyAbsentNotExtensible);
        }
        /* step 25 */
        return trapResult;
    }

    private static void updateUncheckedWithDuplicates(HashSet<Object> uncheckedResultKeys,
            HashMap<Object, Integer> uncheckedDuplicateKeys, Object key) {
        Integer count = uncheckedDuplicateKeys.get(key);
        if (count != null) {
            if (count == 1) {
                uncheckedDuplicateKeys.remove(key);
            } else {
                assert count > 1;
                uncheckedDuplicateKeys.put(key, count - 1);
            }
            uncheckedResultKeys.add(key);
        }
    }

    /**
     * 9.5.12 [[OwnPropertyKeys]] ()
     */
    @Override
    public Iterator<?> ownKeys(ExecutionContext cx) {
        return ownPropertyKeys(cx).iterator();
    }
}
