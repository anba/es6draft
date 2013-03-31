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
import com.github.anba.es6draft.runtime.types.IntegrityLevel;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * <h1>8 Types</h1>
 * <ul>
 * <li>8.5 Proxy Object Internal Methods and Internal Data Properties
 * </ul>
 */
public class ExoticProxy implements ScriptObject {
    protected final Realm realm;
    /** [[ProxyTarget]] */
    protected final ScriptObject proxyTarget;
    /** [[ProxyHandler]] */
    protected final ScriptObject proxyHandler;

    public ExoticProxy(Realm realm, ScriptObject target, ScriptObject handler) {
        this.realm = realm;
        this.proxyTarget = target;
        this.proxyHandler = handler;
    }

    private static class CallabeExoticProxy extends ExoticProxy implements Callable {
        public CallabeExoticProxy(Realm realm, ScriptObject target, ScriptObject handler) {
            super(realm, target, handler);
        }

        /**
         * 8.5.14 [[Call]] (thisArgument, argumentsList)
         */
        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ScriptObject handler = proxyHandler;
            ScriptObject target = proxyTarget;
            Callable trap = GetMethod(callerContext, handler, "apply");
            if (trap == null) {
                return ((Callable) target).call(callerContext, thisValue, args);
            }
            ScriptObject argArray = CreateArrayFromList(callerContext, Arrays.asList(args));
            return trap.call(callerContext, handler, target, thisValue, argArray);
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

        /**
         * 8.5.15 [[Construct]] Internal Method
         */
        @Override
        public Object construct(ExecutionContext callerContext, Object... args) {
            ScriptObject handler = proxyHandler;
            ScriptObject target = proxyTarget;
            Callable trap = GetMethod(callerContext, handler, "construct");
            if (trap == null) {
                return ((Constructor) target).construct(callerContext, args);
            }
            ScriptObject argArray = CreateArrayFromList(callerContext, Arrays.asList(args));
            return trap.call(callerContext, handler, target, argArray);
        }
    }

    /**
     * Abstract Operation: CreateProxy
     */
    public static ExoticProxy CreateProxy(ExecutionContext cx, Object target, Object handler) {
        if (!Type.isObject(target)) {
            throwTypeError(cx, Messages.Key.NotObjectType);
        }
        if (!Type.isObject(handler)) {
            throwTypeError(cx, Messages.Key.NotObjectType);
        }
        ScriptObject proxyTarget = Type.objectValue(target);
        ScriptObject proxyHandler = Type.objectValue(handler);
        ExoticProxy proxy;
        if (IsConstructor(proxyTarget)) {
            proxy = new ConstructorExoticProxy(cx.getRealm(), proxyTarget, proxyHandler);
        } else if (IsCallable(proxyTarget)) {
            proxy = new CallabeExoticProxy(cx.getRealm(), proxyTarget, proxyHandler);
        } else {
            proxy = new ExoticProxy(cx.getRealm(), proxyTarget, proxyHandler);
        }
        return proxy;
    }

    private static boolean __hasOwnProperty(ExecutionContext cx, ScriptObject target,
            Object propertyKey) {
        if (propertyKey instanceof String) {
            return target.hasOwnProperty(cx, (String) propertyKey);
        } else {
            assert propertyKey instanceof Symbol;
            return target.hasOwnProperty(cx, (Symbol) propertyKey);
        }
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
    public ScriptObject getPrototype(ExecutionContext cx) {
        ScriptObject handler = proxyHandler;
        ScriptObject target = proxyTarget;
        Callable trap = GetMethod(cx, handler, "getPrototypeOf");
        if (trap == null) {
            return target.getPrototype(cx);
        }
        Object handlerProto = trap.call(cx, handler, target);
        ScriptObject targetProto = target.getPrototype(cx);
        if (!SameValue(handlerProto, maskNull(targetProto))) {
            throw throwTypeError(cx, Messages.Key.ProxySameValue);
        }
        assert (Type.isNull(handlerProto) || Type.isObject(handlerProto));
        return (ScriptObject) unmaskNull(handlerProto);
    }

    /**
     * 8.5.2 [[SetInheritance]] (V)
     */
    @Override
    public boolean setPrototype(ExecutionContext cx, ScriptObject prototype) {
        assert prototype == null || Type.isObject(prototype);
        ScriptObject handler = proxyHandler;
        ScriptObject target = proxyTarget;
        Callable trap = GetMethod(cx, handler, "setPrototypeOf");
        if (trap == null) {
            return target.setPrototype(cx, prototype);
        }
        boolean trapResult = ToBoolean(trap.call(cx, handler, target, maskNull(prototype)));
        ScriptObject targetProto = target.getPrototype(cx);
        if (trapResult && !SameValue(maskNull(prototype), maskNull(targetProto))) {
            throw throwTypeError(cx, Messages.Key.ProxySameValue);
        }
        return trapResult;
    }

    /**
     * 8.5.3 [[HasIntegrity]] ( Level )
     */
    @Override
    public boolean hasIntegrity(ExecutionContext cx, IntegrityLevel level) {
        ScriptObject handler = proxyHandler;
        ScriptObject target = proxyTarget;
        String trapName;
        if (level == IntegrityLevel.NonExtensible) {
            trapName = "isExtensible";
        } else if (level == IntegrityLevel.Sealed) {
            trapName = "isSealed";
        } else {
            trapName = "isFrozen";
        }
        Callable trap = GetMethod(cx, handler, trapName);
        if (trap == null) {
            return target.hasIntegrity(cx, level);
        }
        Object trapResult = trap.call(cx, handler, target);
        boolean booleanTrapResult = ToBoolean(trapResult);
        boolean targetResult = target.hasIntegrity(cx, level);
        if (booleanTrapResult != targetResult) {
            throw throwTypeError(cx, Messages.Key.ProxyNotExtensible);
        }
        return booleanTrapResult;
    }

    /**
     * 8.5.4 [[SetIntegrity]] ( Level )
     */
    @Override
    public boolean setIntegrity(ExecutionContext cx, IntegrityLevel level) {
        ScriptObject handler = proxyHandler;
        ScriptObject target = proxyTarget;
        String trapName;
        if (level == IntegrityLevel.NonExtensible) {
            trapName = "preventExtensions";
        } else if (level == IntegrityLevel.Sealed) {
            trapName = "seal";
        } else {
            trapName = "freeze";
        }
        Callable trap = GetMethod(cx, handler, trapName);
        if (trap == null) {
            return target.setIntegrity(cx, level);
        }
        Object trapResult = trap.call(cx, handler, target);
        boolean booleanTrapResult = ToBoolean(trapResult);
        boolean targetResult = target.hasIntegrity(cx, level);
        if (booleanTrapResult != targetResult) {
            throw throwTypeError(cx, Messages.Key.ProxyNotExtensible);
        }
        return booleanTrapResult;
    }

    /**
     * 8.5.5 [[HasOwnProperty]] (P)
     */
    @Override
    public boolean hasOwnProperty(ExecutionContext cx, String propertyKey) {
        return hasOwnProperty(cx, (Object) propertyKey);
    }

    /**
     * 8.5.5 [[HasOwnProperty]] (P)
     */
    @Override
    public boolean hasOwnProperty(ExecutionContext cx, Symbol propertyKey) {
        return hasOwnProperty(cx, (Object) propertyKey);
    }

    /**
     * 8.5.5 [[HasOwnProperty]] (P)
     */
    private boolean hasOwnProperty(ExecutionContext cx, Object propertyKey) {
        ScriptObject handler = proxyHandler;
        ScriptObject target = proxyTarget;
        Callable trap = GetMethod(cx, handler, "hasOwn");
        if (trap == null) {
            return __hasOwnProperty(cx, target, propertyKey);
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
        } else {
            boolean extensibleTarget = IsExtensible(cx, target);
            if (extensibleTarget) {
                return success;
            }
            Property targetDesc = __getOwnProperty(cx, target, propertyKey);
            if (targetDesc == null) {
                throw throwTypeError(cx, Messages.Key.ProxyNoOwnProperty);
            }
        }
        return success;
    }

    /**
     * 8.5.6 [[GetOwnProperty]] (P)
     */
    @Override
    public Property getOwnProperty(ExecutionContext cx, String propertyKey) {
        return getOwnProperty(cx, (Object) propertyKey);
    }

    /**
     * 8.5.6 [[GetOwnProperty]] (P)
     */
    @Override
    public Property getOwnProperty(ExecutionContext cx, Symbol propertyKey) {
        return getOwnProperty(cx, (Object) propertyKey);
    }

    /**
     * 8.5.6 [[GetOwnProperty]] (P)
     */
    private Property getOwnProperty(ExecutionContext cx, Object propertyKey) {
        ScriptObject handler = proxyHandler;
        ScriptObject target = proxyTarget;
        Callable trap = GetMethod(cx, handler, "getOwnPropertyDescriptor");
        if (trap == null) {
            return __getOwnProperty(cx, target, propertyKey);
        }
        Object trapResultObj = trap.call(cx, handler, target, propertyKey);
        if (!(Type.isObject(trapResultObj) || Type.isUndefined(trapResultObj))) {
            throw throwTypeError(cx, Messages.Key.ProxyNotObjectOrUndefined);
        }
        // TODO: need copy b/c of side-effects?
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
        // TODO: side-effect in isExtensible()?
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
     * 8.5.7 [[DefineOwnProperty]] (P, Desc)
     */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, String propertyKey,
            PropertyDescriptor desc) {
        return defineOwnProperty(cx, (Object) propertyKey, desc);
    }

    /**
     * 8.5.7 [[DefineOwnProperty]] (P, Desc)
     */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, Symbol propertyKey,
            PropertyDescriptor desc) {
        return defineOwnProperty(cx, (Object) propertyKey, desc);
    }

    /**
     * 8.5.7 [[DefineOwnProperty]] (P, Desc)
     */
    private boolean defineOwnProperty(ExecutionContext cx, Object propertyKey,
            PropertyDescriptor desc) {
        ScriptObject handler = proxyHandler;
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
        // TODO: need copy b/c of side-effects?
        Property targetDesc = __getOwnProperty(cx, target, propertyKey);
        // TODO: side-effect in isExtensible()?
        boolean extensibleTarget = IsExtensible(cx, target);
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
            if (!desc.isConfigurable() && targetDesc.isConfigurable()) {
                throw throwTypeError(cx, Messages.Key.ProxyNotConfigurable);
            }
        }
        return true;
    }

    /**
     * 8.5.8 [[HasProperty]] (P)
     */
    @Override
    public boolean hasProperty(ExecutionContext cx, String propertyKey) {
        return hasProperty(cx, (Object) propertyKey);
    }

    /**
     * 8.5.8 [[HasProperty]] (P)
     */
    @Override
    public boolean hasProperty(ExecutionContext cx, Symbol propertyKey) {
        return hasProperty(cx, (Object) propertyKey);
    }

    /**
     * 8.5.8 [[HasProperty]] (P)
     */
    private boolean hasProperty(ExecutionContext cx, Object propertyKey) {
        ScriptObject handler = proxyHandler;
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
     * 8.5.9 [[Get]] (P, Receiver)
     */
    @Override
    public Object get(ExecutionContext cx, String propertyKey, Object receiver) {
        return get(cx, (Object) propertyKey, receiver);
    }

    /**
     * 8.5.9 [[Get]] (P, Receiver)
     */
    @Override
    public Object get(ExecutionContext cx, Symbol propertyKey, Object receiver) {
        return get(cx, (Object) propertyKey, receiver);
    }

    /**
     * 8.5.9 [[Get]] (P, Receiver)
     */
    private Object get(ExecutionContext cx, Object propertyKey, Object receiver) {
        ScriptObject handler = proxyHandler;
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
     * 8.5.10 [[Set]] ( P, V, Receiver)
     */
    @Override
    public boolean set(ExecutionContext cx, String propertyKey, Object value, Object receiver) {
        return set(cx, (Object) propertyKey, value, receiver);
    }

    /**
     * 8.5.10 [[Set]] ( P, V, Receiver)
     */
    @Override
    public boolean set(ExecutionContext cx, Symbol propertyKey, Object value, Object receiver) {
        return set(cx, (Object) propertyKey, value, receiver);
    }

    /**
     * 8.5.10 [[Set]] ( P, V, Receiver)
     */
    private boolean set(ExecutionContext cx, Object propertyKey, Object value, Object receiver) {
        ScriptObject handler = proxyHandler;
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
     * 8.5.11 [[Delete]] (P)
     */
    @Override
    public boolean delete(ExecutionContext cx, String propertyKey) {
        return delete(cx, (Object) propertyKey);
    }

    /**
     * 8.5.11 [[Delete]] (P)
     */
    @Override
    public boolean delete(ExecutionContext cx, Symbol propertyKey) {
        return delete(cx, (Object) propertyKey);
    }

    /**
     * 8.5.11 [[Delete]] (P)
     */
    private boolean delete(ExecutionContext cx, Object propertyKey) {
        ScriptObject handler = proxyHandler;
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
     * 8.5.12 [[Enumerate]] ()
     */
    @Override
    public ScriptObject enumerate(ExecutionContext cx) {
        ScriptObject handler = proxyHandler;
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
     * 8.5.13 [[OwnPropertyKeys]] ()
     */
    @Override
    public ScriptObject ownPropertyKeys(ExecutionContext cx) {
        ScriptObject handler = proxyHandler;
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
