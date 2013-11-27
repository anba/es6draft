/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.CompletePropertyDescriptor;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.FromPropertyDescriptor;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.ToPropertyDescriptor;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.IsCompatiblePropertyDescriptor;

import java.util.Arrays;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
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
public class ExoticProxy implements ScriptObject {
    protected final Realm realm;
    /** [[ProxyTarget]] */
    protected ScriptObject proxyTarget;
    /** [[ProxyHandler]] */
    private ScriptObject proxyHandler;

    public ExoticProxy(Realm realm, ScriptObject target, ScriptObject handler) {
        this.realm = realm;
        this.proxyTarget = target;
        this.proxyHandler = handler;
    }

    protected final ScriptObject getProxyHandler(ExecutionContext cx) {
        if (proxyHandler == null) {
            // FIXME: use better error message
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        return proxyHandler;
    }

    /**
     * Revoke this proxy, that means set both, [[ProxyTarget]] and [[ProxyHandler]], to {@code null}
     * and by that prevent further operations on this proxy.
     */
    public void revoke() {
        assert this.proxyHandler != null && this.proxyTarget != null;
        this.proxyHandler = null;
        this.proxyTarget = null;
    }

    private static class CallabeExoticProxy extends ExoticProxy implements Callable {
        public CallabeExoticProxy(Realm realm, ScriptObject target, ScriptObject handler) {
            super(realm, target, handler);
        }

        /**
         * 9.5.14 [[Call]] (thisArgument, argumentsList)
         */
        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ScriptObject handler = getProxyHandler(callerContext);
            ScriptObject target = proxyTarget;
            Callable trap = GetMethod(callerContext, handler, "apply");
            if (trap == null) {
                return ((Callable) target).call(callerContext, thisValue, args);
            }
            ScriptObject argArray = CreateArrayFromList(callerContext, Arrays.asList(args));
            return trap.call(callerContext, handler, target, thisValue, argArray);
        }

        /**
         * 9.5.14 [[Call]] (thisArgument, argumentsList)
         */
        @Override
        public Object tailCall(ExecutionContext callerContext, Object thisValue, Object... args) {
            return call(callerContext, thisValue, args);
        }

        @Override
        public String toSource() {
            return ((Callable) proxyTarget).toSource();
        }
    }

    private static class ConstructorExoticProxy extends CallabeExoticProxy implements Constructor {
        public ConstructorExoticProxy(Realm realm, ScriptObject target, ScriptObject handler) {
            super(realm, target, handler);
        }

        @Override
        public boolean isConstructor() {
            // ConstructorExoticProxy is only created if [[ProxyTarget]] already has [[Construct]]
            return true;
        }

        /**
         * 9.5.15 [[Construct]] Internal Method
         */
        @Override
        public ScriptObject construct(ExecutionContext callerContext, Object... args) {
            ScriptObject handler = getProxyHandler(callerContext);
            ScriptObject target = proxyTarget;
            Callable trap = GetMethod(callerContext, handler, "construct");
            if (trap == null) {
                return ((Constructor) target).construct(callerContext, args);
            }
            ScriptObject argArray = CreateArrayFromList(callerContext, Arrays.asList(args));
            Object newObj = trap.call(callerContext, handler, target, argArray);
            if (!Type.isObject(newObj)) {
                throw throwTypeError(callerContext, Messages.Key.NotObjectType);
            }
            return Type.objectValue(newObj);
        }

        /**
         * 9.5.14 [[Construct]] Internal Method
         */
        @Override
        public ScriptObject tailConstruct(ExecutionContext callerContext, Object... args) {
            return construct(callerContext, args);
        }
    }

    /**
     * 9.5.16 ProxyCreate Abstract Operation
     */
    public static ExoticProxy ProxyCreate(ExecutionContext cx, Object target, Object handler) {
        /* step 1 */
        if (!Type.isObject(target)) {
            throwTypeError(cx, Messages.Key.NotObjectType);
        }
        /* step 2 */
        if (!Type.isObject(handler)) {
            throwTypeError(cx, Messages.Key.NotObjectType);
        }
        ScriptObject proxyTarget = Type.objectValue(target);
        ScriptObject proxyHandler = Type.objectValue(handler);
        /* steps 3-7 */
        ExoticProxy proxy;
        if (IsConstructor(proxyTarget)) {
            proxy = new ConstructorExoticProxy(cx.getRealm(), proxyTarget, proxyHandler);
        } else if (IsCallable(proxyTarget)) {
            proxy = new CallabeExoticProxy(cx.getRealm(), proxyTarget, proxyHandler);
        } else {
            proxy = new ExoticProxy(cx.getRealm(), proxyTarget, proxyHandler);
        }
        /* step 8 */
        return proxy;
    }

    private static Property __getOwnProperty(ExecutionContext cx, ScriptObject target,
            Object propertyKey) {
        if (propertyKey instanceof String) {
            return target.getOwnProperty(cx, (String) propertyKey);
        } else {
            assert propertyKey instanceof Symbol;
            return target.getOwnProperty(cx, (Symbol) propertyKey);
        }
    }

    private static boolean __defineOwnProperty(ExecutionContext cx, ScriptObject target,
            Object propertyKey, PropertyDescriptor desc) {
        if (propertyKey instanceof String) {
            return target.defineOwnProperty(cx, (String) propertyKey, desc);
        } else {
            assert propertyKey instanceof Symbol;
            return target.defineOwnProperty(cx, (Symbol) propertyKey, desc);
        }
    }

    private static boolean __hasProperty(ExecutionContext cx, ScriptObject target,
            Object propertyKey) {
        if (propertyKey instanceof String) {
            return target.hasProperty(cx, (String) propertyKey);
        } else {
            assert propertyKey instanceof Symbol;
            return target.hasProperty(cx, (Symbol) propertyKey);
        }
    }

    private static Object __get(ExecutionContext cx, ScriptObject target, Object propertyKey,
            Object receiver) {
        if (propertyKey instanceof String) {
            return target.get(cx, (String) propertyKey, receiver);
        } else {
            assert propertyKey instanceof Symbol;
            return target.get(cx, (Symbol) propertyKey, receiver);
        }
    }

    private static boolean __set(ExecutionContext cx, ScriptObject target, Object propertyKey,
            Object value, Object receiver) {
        if (propertyKey instanceof String) {
            return target.set(cx, (String) propertyKey, value, receiver);
        } else {
            assert propertyKey instanceof Symbol;
            return target.set(cx, (Symbol) propertyKey, value, receiver);
        }
    }

    private static boolean __delete(ExecutionContext cx, ScriptObject target, Object propertyKey) {
        if (propertyKey instanceof String) {
            return target.delete(cx, (String) propertyKey);
        } else {
            assert propertyKey instanceof Symbol;
            return target.delete(cx, (Symbol) propertyKey);
        }
    }

    /**
     * Java {@code null} to {@link Null#NULL}
     */
    private static Object maskNull(Object val) {
        return (val != null ? val : NULL);
    }

    /**
     * {@link Null#NULL} to Java {@code null}
     */
    private static Object unmaskNull(Object jsval) {
        return (jsval != NULL ? jsval : null);
    }

    /**
     * 9.5.1 [[GetPrototypeOf]] ( )
     */
    @Override
    public ScriptObject getPrototypeOf(ExecutionContext cx) {
        ScriptObject handler = getProxyHandler(cx);
        ScriptObject target = proxyTarget;
        Callable trap = GetMethod(cx, handler, "getPrototypeOf");
        if (trap == null) {
            return target.getPrototypeOf(cx);
        }
        Object handlerProto = trap.call(cx, handler, target);
        if (!(Type.isNull(handlerProto) || Type.isObject(handlerProto))) {
            throw throwTypeError(cx, Messages.Key.NotObjectOrNull);
        }
        ScriptObject handlerProto_ = (ScriptObject) unmaskNull(handlerProto);
        boolean extensibleTarget = IsExtensible(cx, target);
        if (extensibleTarget) {
            return handlerProto_;
        }
        ScriptObject targetProto = target.getPrototypeOf(cx);
        if (!SameValue(handlerProto_, targetProto)) {
            throw throwTypeError(cx, Messages.Key.ProxySameValue);
        }
        return handlerProto_;
    }

    /**
     * 9.5.2 [[SetPrototypeOf]] (V)
     */
    @Override
    public boolean setPrototypeOf(ExecutionContext cx, ScriptObject prototype) {
        ScriptObject handler = getProxyHandler(cx);
        ScriptObject target = proxyTarget;
        Callable trap = GetMethod(cx, handler, "setPrototypeOf");
        if (trap == null) {
            return target.setPrototypeOf(cx, prototype);
        }
        boolean trapResult = ToBoolean(trap.call(cx, handler, target, maskNull(prototype)));
        boolean extensibleTarget = IsExtensible(cx, target);
        if (extensibleTarget) {
            return trapResult;
        }
        ScriptObject targetProto = target.getPrototypeOf(cx);
        if (trapResult && !SameValue(prototype, targetProto)) {
            throw throwTypeError(cx, Messages.Key.ProxySameValue);
        }
        return trapResult;
    }

    /**
     * 9.5.3 [[IsExtensible]] ( )
     */
    @Override
    public boolean isExtensible(ExecutionContext cx) {
        ScriptObject handler = getProxyHandler(cx);
        ScriptObject target = proxyTarget;
        Callable trap = GetMethod(cx, handler, "isExtensible");
        if (trap == null) {
            return target.isExtensible(cx);
        }
        Object trapResult = trap.call(cx, handler, target);
        boolean booleanTrapResult = ToBoolean(trapResult);
        boolean targetResult = target.isExtensible(cx);
        if (booleanTrapResult != targetResult) {
            throw throwTypeError(cx, Messages.Key.ProxyNotExtensible);
        }
        return booleanTrapResult;
    }

    /**
     * 9.5.4 [[PreventExtensions]] ( )
     */
    @Override
    public boolean preventExtensions(ExecutionContext cx) {
        ScriptObject handler = getProxyHandler(cx);
        ScriptObject target = proxyTarget;
        Callable trap = GetMethod(cx, handler, "preventExtensions");
        if (trap == null) {
            return target.preventExtensions(cx);
        }
        Object trapResult = trap.call(cx, handler, target);
        boolean booleanTrapResult = ToBoolean(trapResult);
        boolean targetIsExtensible = target.isExtensible(cx);
        if (booleanTrapResult && targetIsExtensible) {
            throw throwTypeError(cx, Messages.Key.ProxyNotExtensible);
        }
        return booleanTrapResult;
    }

    /**
     * 9.5.5 [[GetOwnProperty]] (P)
     */
    @Override
    public Property getOwnProperty(ExecutionContext cx, String propertyKey) {
        return getOwnProperty(cx, (Object) propertyKey);
    }

    /**
     * 9.5.5 [[GetOwnProperty]] (P)
     */
    @Override
    public Property getOwnProperty(ExecutionContext cx, Symbol propertyKey) {
        return getOwnProperty(cx, (Object) propertyKey);
    }

    /**
     * 9.5.5 [[GetOwnProperty]] (P)
     */
    private Property getOwnProperty(ExecutionContext cx, Object propertyKey) {
        ScriptObject handler = getProxyHandler(cx);
        ScriptObject target = proxyTarget;
        Callable trap = GetMethod(cx, handler, "getOwnPropertyDescriptor");
        if (trap == null) {
            return __getOwnProperty(cx, target, propertyKey);
        }
        Object trapResultObj = trap.call(cx, handler, target, propertyKey);
        if (!(Type.isObject(trapResultObj) || Type.isUndefined(trapResultObj))) {
            throw throwTypeError(cx, Messages.Key.ProxyNotObjectOrUndefined);
        }
        Property targetDesc = __getOwnProperty(cx, target, propertyKey);
        if (Type.isUndefined(trapResultObj)) {
            if (targetDesc == null) {
                return null;
            }
            if (!targetDesc.isConfigurable()) {
                throw throwTypeError(cx, Messages.Key.ProxyNotConfigurable);
            }
            boolean extensibleTarget = IsExtensible(cx, target);
            if (!extensibleTarget) {
                throw throwTypeError(cx, Messages.Key.ProxyNotExtensible);
            }
            return null;
        }
        if (targetDesc != null) {
            // need copy because of possible side-effects in IsExtensible()
            targetDesc = targetDesc.clone();
        }
        boolean extensibleTarget = IsExtensible(cx, target);
        PropertyDescriptor resultDesc = ToPropertyDescriptor(cx, trapResultObj);
        CompletePropertyDescriptor(resultDesc, targetDesc);
        boolean valid = IsCompatiblePropertyDescriptor(extensibleTarget, resultDesc, targetDesc);
        if (!valid) {
            throw throwTypeError(cx, Messages.Key.ProxyIncompatibleDescriptor);
        }
        if (!resultDesc.isConfigurable()) {
            if (targetDesc == null || targetDesc.isConfigurable()) {
                throw throwTypeError(cx, Messages.Key.ProxyNotConfigurable);
            }
        }
        // TODO: [[Origin]] ???
        return resultDesc.toProperty();
    }

    /**
     * 9.5.6 [[DefineOwnProperty]] (P, Desc)
     */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, String propertyKey,
            PropertyDescriptor desc) {
        return defineOwnProperty(cx, (Object) propertyKey, desc);
    }

    /**
     * 9.5.6 [[DefineOwnProperty]] (P, Desc)
     */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, Symbol propertyKey,
            PropertyDescriptor desc) {
        return defineOwnProperty(cx, (Object) propertyKey, desc);
    }

    /**
     * 9.5.6 [[DefineOwnProperty]] (P, Desc)
     */
    private boolean defineOwnProperty(ExecutionContext cx, Object propertyKey,
            PropertyDescriptor desc) {
        ScriptObject handler = getProxyHandler(cx);
        ScriptObject target = proxyTarget;
        Callable trap = GetMethod(cx, handler, "defineProperty");
        if (trap == null) {
            return __defineOwnProperty(cx, target, propertyKey, desc);
        }
        Object descObj = FromPropertyDescriptor(cx, desc);
        Object trapResult = trap.call(cx, handler, target, propertyKey, descObj);
        if (!ToBoolean(trapResult)) {
            return false;
        }
        Property targetDesc = __getOwnProperty(cx, target, propertyKey);
        if (targetDesc != null) {
            // need copy because of possible side-effects in IsExtensible()
            targetDesc = targetDesc.clone();
        }
        boolean extensibleTarget = IsExtensible(cx, target);
        boolean settingConfigFalse = desc.hasConfigurable() && !desc.isConfigurable();
        if (targetDesc == null) {
            if (!extensibleTarget) {
                throw throwTypeError(cx, Messages.Key.ProxyNotExtensible);
            }
            if (!desc.isConfigurable()) {
                throw throwTypeError(cx, Messages.Key.ProxyNotConfigurable);
            }
        } else {
            if (!IsCompatiblePropertyDescriptor(extensibleTarget, desc, targetDesc)) {
                throw throwTypeError(cx, Messages.Key.ProxyIncompatibleDescriptor);
            }
            if (settingConfigFalse && targetDesc.isConfigurable()) {
                throw throwTypeError(cx, Messages.Key.ProxyNotConfigurable);
            }
        }
        return true;
    }

    /**
     * 9.5.7 [[HasProperty]] (P)
     */
    @Override
    public boolean hasProperty(ExecutionContext cx, String propertyKey) {
        return hasProperty(cx, (Object) propertyKey);
    }

    /**
     * 9.5.7 [[HasProperty]] (P)
     */
    @Override
    public boolean hasProperty(ExecutionContext cx, Symbol propertyKey) {
        return hasProperty(cx, (Object) propertyKey);
    }

    /**
     * 9.5.7 [[HasProperty]] (P)
     */
    private boolean hasProperty(ExecutionContext cx, Object propertyKey) {
        ScriptObject handler = getProxyHandler(cx);
        ScriptObject target = proxyTarget;
        Callable trap = GetMethod(cx, handler, "has");
        if (trap == null) {
            return __hasProperty(cx, target, propertyKey);
        }
        Object trapResult = trap.call(cx, handler, target, propertyKey);
        boolean success = ToBoolean(trapResult);
        if (!success) {
            Property targetDesc = __getOwnProperty(cx, target, propertyKey);
            if (targetDesc != null) {
                if (!targetDesc.isConfigurable()) {
                    throw throwTypeError(cx, Messages.Key.ProxyNotConfigurable);
                }
                boolean extensibleTarget = IsExtensible(cx, target);
                if (!extensibleTarget) {
                    throw throwTypeError(cx, Messages.Key.ProxyNotExtensible);
                }
            }
        }
        return success;
    }

    /**
     * 9.5.8 [[Get]] (P, Receiver)
     */
    @Override
    public Object get(ExecutionContext cx, String propertyKey, Object receiver) {
        return get(cx, (Object) propertyKey, receiver);
    }

    /**
     * 9.5.8 [[Get]] (P, Receiver)
     */
    @Override
    public Object get(ExecutionContext cx, Symbol propertyKey, Object receiver) {
        return get(cx, (Object) propertyKey, receiver);
    }

    /**
     * 9.5.8 [[Get]] (P, Receiver)
     */
    private Object get(ExecutionContext cx, Object propertyKey, Object receiver) {
        ScriptObject handler = getProxyHandler(cx);
        ScriptObject target = proxyTarget;
        Callable trap = GetMethod(cx, handler, "get");
        if (trap == null) {
            return __get(cx, target, propertyKey, receiver);
        }
        Object trapResult = trap.call(cx, handler, target, propertyKey, receiver);
        Property targetDesc = __getOwnProperty(cx, target, propertyKey);
        if (targetDesc != null) {
            if (targetDesc.isDataDescriptor() && !targetDesc.isConfigurable()
                    && !targetDesc.isWritable()) {
                if (!SameValue(trapResult, targetDesc.getValue())) {
                    throw throwTypeError(cx, Messages.Key.ProxySameValue);
                }
            }
            if (targetDesc.isAccessorDescriptor() && !targetDesc.isConfigurable()
                    && targetDesc.getGetter() == null) {
                if (trapResult != UNDEFINED) {
                    throw throwTypeError(cx, Messages.Key.ProxyNoGetter);
                }
            }
        }
        return trapResult;
    }

    /**
     * 9.5.9 [[Set]] ( P, V, Receiver)
     */
    @Override
    public boolean set(ExecutionContext cx, String propertyKey, Object value, Object receiver) {
        return set(cx, (Object) propertyKey, value, receiver);
    }

    /**
     * 9.5.9 [[Set]] ( P, V, Receiver)
     */
    @Override
    public boolean set(ExecutionContext cx, Symbol propertyKey, Object value, Object receiver) {
        return set(cx, (Object) propertyKey, value, receiver);
    }

    /**
     * 9.5.9 [[Set]] ( P, V, Receiver)
     */
    private boolean set(ExecutionContext cx, Object propertyKey, Object value, Object receiver) {
        ScriptObject handler = getProxyHandler(cx);
        ScriptObject target = proxyTarget;
        Callable trap = GetMethod(cx, handler, "set");
        if (trap == null) {
            return __set(cx, target, propertyKey, value, receiver);
        }
        Object trapResult = trap.call(cx, handler, target, propertyKey, value, receiver);
        if (!ToBoolean(trapResult)) {
            return false;
        }
        Property targetDesc = __getOwnProperty(cx, target, propertyKey);
        if (targetDesc != null) {
            if (targetDesc.isDataDescriptor() && !targetDesc.isConfigurable()
                    && !targetDesc.isWritable()) {
                if (!SameValue(value, targetDesc.getValue())) {
                    throw throwTypeError(cx, Messages.Key.ProxySameValue);
                }
            }
            if (targetDesc.isAccessorDescriptor() && !targetDesc.isConfigurable()) {
                if (targetDesc.getSetter() == null) {
                    throw throwTypeError(cx, Messages.Key.ProxyNoSetter);
                }
            }
        }
        return true;
    }

    /**
     * 9.5.10 [[Invoke]] (P, ArgumentsList, Receiver)
     */
    @Override
    public Object invoke(ExecutionContext cx, String propertyKey, Object[] arguments,
            Object receiver) {
        ScriptObject handler = getProxyHandler(cx);
        ScriptObject target = proxyTarget;
        Callable trap = GetMethod(cx, handler, "invoke");
        if (trap == null) {
            return target.invoke(cx, propertyKey, arguments, receiver);
        }
        ScriptObject argArray = CreateArrayFromList(cx, Arrays.asList(arguments));
        return trap.call(cx, handler, target, propertyKey, argArray, receiver);
    }

    /**
     * 9.5.10 [[Invoke]] (P, ArgumentsList, Receiver)
     */
    @Override
    public Object invoke(ExecutionContext cx, Symbol propertyKey, Object[] arguments,
            Object receiver) {
        ScriptObject handler = getProxyHandler(cx);
        ScriptObject target = proxyTarget;
        Callable trap = GetMethod(cx, handler, "invoke");
        if (trap == null) {
            return target.invoke(cx, propertyKey, arguments, receiver);
        }
        ScriptObject argArray = CreateArrayFromList(cx, Arrays.asList(arguments));
        return trap.call(cx, handler, target, propertyKey, argArray, receiver);
    }

    /**
     * 9.5.11 [[Delete]] (P)
     */
    @Override
    public boolean delete(ExecutionContext cx, String propertyKey) {
        return delete(cx, (Object) propertyKey);
    }

    /**
     * 9.5.11 [[Delete]] (P)
     */
    @Override
    public boolean delete(ExecutionContext cx, Symbol propertyKey) {
        return delete(cx, (Object) propertyKey);
    }

    /**
     * 9.5.11 [[Delete]] (P)
     */
    private boolean delete(ExecutionContext cx, Object propertyKey) {
        ScriptObject handler = getProxyHandler(cx);
        ScriptObject target = proxyTarget;
        Callable trap = GetMethod(cx, handler, "deleteProperty");
        if (trap == null) {
            return __delete(cx, target, propertyKey);
        }
        Object trapResult = trap.call(cx, handler, target, propertyKey);
        if (!ToBoolean(trapResult)) {
            return false;
        }
        Property targetDesc = __getOwnProperty(cx, target, propertyKey);
        if (targetDesc == null) {
            return true;
        }
        if (!targetDesc.isConfigurable()) {
            throw throwTypeError(cx, Messages.Key.ProxyNotConfigurable);
        }
        return true;
    }

    /**
     * 9.5.12 [[Enumerate]] ()
     */
    @Override
    public ScriptObject enumerate(ExecutionContext cx) {
        ScriptObject handler = getProxyHandler(cx);
        ScriptObject target = proxyTarget;
        Callable trap = GetMethod(cx, handler, "enumerate");
        if (trap == null) {
            return target.enumerate(cx);
        }
        Object trapResult = trap.call(cx, handler, target);
        if (!Type.isObject(trapResult)) {
            throw throwTypeError(cx, Messages.Key.ProxyNotObject);
        }
        return Type.objectValue(trapResult);
    }

    /**
     * 9.5.13 [[OwnPropertyKeys]] ()
     */
    @Override
    public ScriptObject ownPropertyKeys(ExecutionContext cx) {
        ScriptObject handler = getProxyHandler(cx);
        ScriptObject target = proxyTarget;
        Callable trap = GetMethod(cx, handler, "ownKeys");
        if (trap == null) {
            return target.ownPropertyKeys(cx);
        }
        Object trapResult = trap.call(cx, handler, target);
        if (!Type.isObject(trapResult)) {
            throw throwTypeError(cx, Messages.Key.ProxyNotObject);
        }
        return Type.objectValue(trapResult);
    }
}
