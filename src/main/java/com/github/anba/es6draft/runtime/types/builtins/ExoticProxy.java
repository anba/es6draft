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
        public Object call(Object thisValue, Object... args) {
            ScriptObject handler = proxyHandler;
            ScriptObject target = proxyTarget;
            Callable trap = GetMethod(realm, handler, "apply");
            if (trap == null) {
                return ((Callable) target).call(thisValue, args);
            }
            ScriptObject argArray = CreateArrayFromList(realm, Arrays.asList(args));
            return trap.call(handler, target, thisValue, argArray);
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
        public Object construct(Object... args) {
            ScriptObject handler = proxyHandler;
            ScriptObject target = proxyTarget;
            Callable trap = GetMethod(realm, handler, "construct");
            if (trap == null) {
                return ((Constructor) target).construct(args);
            }
            ScriptObject argArray = CreateArrayFromList(realm, Arrays.asList(args));
            return trap.call(handler, target, argArray);
        }
    }

    /**
     * Abstract Operation: CreateProxy
     */
    public static ExoticProxy CreateProxy(Realm realm, Object target, Object handler) {
        if (!Type.isObject(target)) {
            throwTypeError(realm, Messages.Key.NotObjectType);
        }
        if (!Type.isObject(handler)) {
            throwTypeError(realm, Messages.Key.NotObjectType);
        }
        ScriptObject proxyTarget = Type.objectValue(target);
        ScriptObject proxyHandler = Type.objectValue(handler);
        ExoticProxy proxy;
        if (IsConstructor(proxyTarget)) {
            proxy = new ConstructorExoticProxy(realm, proxyTarget, proxyHandler);
        } else if (IsCallable(proxyTarget)) {
            proxy = new CallabeExoticProxy(realm, proxyTarget, proxyHandler);
        } else {
            proxy = new ExoticProxy(realm, proxyTarget, proxyHandler);
        }
        return proxy;
    }

    private static boolean __hasOwnProperty(Realm realm, ScriptObject target, Object propertyKey) {
        if (propertyKey instanceof String) {
            return target.hasOwnProperty(realm, (String) propertyKey);
        } else {
            assert propertyKey instanceof Symbol;
            return target.hasOwnProperty(realm, (Symbol) propertyKey);
        }
    }

    private static Property __getOwnProperty(Realm realm, ScriptObject target, Object propertyKey) {
        if (propertyKey instanceof String) {
            return target.getOwnProperty(realm, (String) propertyKey);
        } else {
            assert propertyKey instanceof Symbol;
            return target.getOwnProperty(realm, (Symbol) propertyKey);
        }
    }

    private static boolean __defineOwnProperty(Realm realm, ScriptObject target,
            Object propertyKey, PropertyDescriptor desc) {
        if (propertyKey instanceof String) {
            return target.defineOwnProperty(realm, (String) propertyKey, desc);
        } else {
            assert propertyKey instanceof Symbol;
            return target.defineOwnProperty(realm, (Symbol) propertyKey, desc);
        }
    }

    private static boolean __hasProperty(Realm realm, ScriptObject target, Object propertyKey) {
        if (propertyKey instanceof String) {
            return target.hasProperty(realm, (String) propertyKey);
        } else {
            assert propertyKey instanceof Symbol;
            return target.hasProperty(realm, (Symbol) propertyKey);
        }
    }

    private static Object __get(Realm realm, ScriptObject target, Object propertyKey,
            Object receiver) {
        if (propertyKey instanceof String) {
            return target.get(realm, (String) propertyKey, receiver);
        } else {
            assert propertyKey instanceof Symbol;
            return target.get(realm, (Symbol) propertyKey, receiver);
        }
    }

    private static boolean __set(Realm realm, ScriptObject target, Object propertyKey,
            Object value, Object receiver) {
        if (propertyKey instanceof String) {
            return target.set(realm, (String) propertyKey, value, receiver);
        } else {
            assert propertyKey instanceof Symbol;
            return target.set(realm, (Symbol) propertyKey, value, receiver);
        }
    }

    private static boolean __delete(Realm realm, ScriptObject target, Object propertyKey) {
        if (propertyKey instanceof String) {
            return target.delete(realm, (String) propertyKey);
        } else {
            assert propertyKey instanceof Symbol;
            return target.delete(realm, (Symbol) propertyKey);
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
    public ScriptObject getPrototype(Realm realm) {
        ScriptObject handler = proxyHandler;
        ScriptObject target = proxyTarget;
        Callable trap = GetMethod(realm, handler, "getPrototypeOf");
        if (trap == null) {
            return target.getPrototype(realm);
        }
        Object handlerProto = trap.call(handler, target);
        ScriptObject targetProto = target.getPrototype(realm);
        if (!SameValue(handlerProto, maskNull(targetProto))) {
            throw throwTypeError(realm, Messages.Key.ProxySameValue);
        }
        assert (Type.isNull(handlerProto) || Type.isObject(handlerProto));
        return (ScriptObject) unmaskNull(handlerProto);
    }

    /**
     * 8.5.2 [[SetInheritance]] (V)
     */
    @Override
    public boolean setPrototype(Realm realm, ScriptObject prototype) {
        assert prototype == null || Type.isObject(prototype);
        ScriptObject handler = proxyHandler;
        ScriptObject target = proxyTarget;
        Callable trap = GetMethod(realm, handler, "setPrototypeOf");
        if (trap == null) {
            return target.setPrototype(realm, prototype);
        }
        boolean trapResult = ToBoolean(trap.call(handler, target, maskNull(prototype)));
        ScriptObject targetProto = target.getPrototype(realm);
        if (trapResult && !SameValue(maskNull(prototype), maskNull(targetProto))) {
            throw throwTypeError(realm, Messages.Key.ProxySameValue);
        }
        return trapResult;
    }

    /**
     * 8.5.3 [[HasIntegrity]] ( Level )
     */
    @Override
    public boolean hasIntegrity(Realm realm, IntegrityLevel level) {
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
        Callable trap = GetMethod(realm, handler, trapName);
        if (trap == null) {
            return target.hasIntegrity(realm, level);
        }
        Object trapResult = trap.call(handler, target);
        boolean booleanTrapResult = ToBoolean(trapResult);
        boolean targetResult = target.hasIntegrity(realm, level);
        if (booleanTrapResult != targetResult) {
            throw throwTypeError(realm, Messages.Key.ProxyNotExtensible);
        }
        return booleanTrapResult;
    }

    /**
     * 8.5.4 [[SetIntegrity]] ( Level )
     */
    @Override
    public boolean setIntegrity(Realm realm, IntegrityLevel level) {
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
        Callable trap = GetMethod(realm, handler, trapName);
        if (trap == null) {
            return target.setIntegrity(realm, level);
        }
        Object trapResult = trap.call(handler, target);
        boolean booleanTrapResult = ToBoolean(trapResult);
        boolean targetResult = target.hasIntegrity(realm, level);
        if (booleanTrapResult != targetResult) {
            throw throwTypeError(realm, Messages.Key.ProxyNotExtensible);
        }
        return booleanTrapResult;
    }

    /**
     * 8.5.5 [[HasOwnProperty]] (P)
     */
    @Override
    public boolean hasOwnProperty(Realm realm, String propertyKey) {
        return hasOwnProperty(realm, (Object) propertyKey);
    }

    /**
     * 8.5.5 [[HasOwnProperty]] (P)
     */
    @Override
    public boolean hasOwnProperty(Realm realm, Symbol propertyKey) {
        return hasOwnProperty(realm, (Object) propertyKey);
    }

    /**
     * 8.5.5 [[HasOwnProperty]] (P)
     */
    private boolean hasOwnProperty(Realm realm, Object propertyKey) {
        ScriptObject handler = proxyHandler;
        ScriptObject target = proxyTarget;
        Callable trap = GetMethod(realm, handler, "hasOwn");
        if (trap == null) {
            return __hasOwnProperty(realm, target, propertyKey);
        }
        Object trapResult = trap.call(handler, target, propertyKey);
        boolean success = ToBoolean(trapResult);
        if (!success) {
            Property targetDesc = __getOwnProperty(realm, target, propertyKey);
            if (targetDesc != null) {
                if (!targetDesc.isConfigurable()) {
                    throw throwTypeError(realm, Messages.Key.ProxyNotConfigurable);
                }
                boolean extensibleTarget = IsExtensible(realm, target);
                if (!extensibleTarget) {
                    throw throwTypeError(realm, Messages.Key.ProxyNotExtensible);
                }
            }
        } else {
            boolean extensibleTarget = IsExtensible(realm, target);
            if (extensibleTarget) {
                return success;
            }
            Property targetDesc = __getOwnProperty(realm, target, propertyKey);
            if (targetDesc == null) {
                throw throwTypeError(realm, Messages.Key.ProxyNoOwnProperty);
            }
        }
        return success;
    }

    /**
     * 8.5.6 [[GetOwnProperty]] (P)
     */
    @Override
    public Property getOwnProperty(Realm realm, String propertyKey) {
        return getOwnProperty(realm, (Object) propertyKey);
    }

    /**
     * 8.5.6 [[GetOwnProperty]] (P)
     */
    @Override
    public Property getOwnProperty(Realm realm, Symbol propertyKey) {
        return getOwnProperty(realm, (Object) propertyKey);
    }

    /**
     * 8.5.6 [[GetOwnProperty]] (P)
     */
    private Property getOwnProperty(Realm realm, Object propertyKey) {
        ScriptObject handler = proxyHandler;
        ScriptObject target = proxyTarget;
        Callable trap = GetMethod(realm, handler, "getOwnPropertyDescriptor");
        if (trap == null) {
            return __getOwnProperty(realm, target, propertyKey);
        }
        Object trapResultObj = trap.call(handler, target, propertyKey);
        if (!(Type.isObject(trapResultObj) || Type.isUndefined(trapResultObj))) {
            throw throwTypeError(realm, Messages.Key.ProxyNotObjectOrUndefined);
        }
        // TODO: need copy b/c of side-effects?
        Property targetDesc = __getOwnProperty(realm, target, propertyKey);
        if (Type.isUndefined(trapResultObj)) {
            if (targetDesc == null) {
                return null;
            }
            if (!targetDesc.isConfigurable()) {
                throw throwTypeError(realm, Messages.Key.ProxyNotConfigurable);
            }
            boolean extensibleTarget = IsExtensible(realm, target);
            if (!extensibleTarget) {
                throw throwTypeError(realm, Messages.Key.ProxyNotExtensible);
            }
            return null;
        }
        // TODO: side-effect in isExtensible()?
        boolean extensibleTarget = IsExtensible(realm, target);
        PropertyDescriptor resultDesc = ToPropertyDescriptor(realm, trapResultObj);
        CompletePropertyDescriptor(resultDesc, targetDesc);
        boolean valid = IsCompatiblePropertyDescriptor(extensibleTarget, resultDesc, targetDesc);
        if (!valid) {
            throw throwTypeError(realm, Messages.Key.ProxyIncompatibleDescriptor);
        }
        if (!resultDesc.isConfigurable()) {
            if (targetDesc == null || targetDesc.isConfigurable()) {
                throw throwTypeError(realm, Messages.Key.ProxyNotConfigurable);
            }
        }
        // TODO: [[Origin]] ???
        return resultDesc.toProperty();
    }

    /**
     * 8.5.7 [[DefineOwnProperty]] (P, Desc)
     */
    @Override
    public boolean defineOwnProperty(Realm realm, String propertyKey, PropertyDescriptor desc) {
        return defineOwnProperty(realm, (Object) propertyKey, desc);
    }

    /**
     * 8.5.7 [[DefineOwnProperty]] (P, Desc)
     */
    @Override
    public boolean defineOwnProperty(Realm realm, Symbol propertyKey, PropertyDescriptor desc) {
        return defineOwnProperty(realm, (Object) propertyKey, desc);
    }

    /**
     * 8.5.7 [[DefineOwnProperty]] (P, Desc)
     */
    private boolean defineOwnProperty(Realm realm, Object propertyKey, PropertyDescriptor desc) {
        ScriptObject handler = proxyHandler;
        ScriptObject target = proxyTarget;
        Callable trap = GetMethod(realm, handler, "defineProperty");
        if (trap == null) {
            return __defineOwnProperty(realm, target, propertyKey, desc);
        }
        Object descObj = FromPropertyDescriptor(realm, desc);
        Object trapResult = trap.call(handler, target, propertyKey, descObj);
        if (!ToBoolean(trapResult)) {
            return false;
        }
        // TODO: need copy b/c of side-effects?
        Property targetDesc = __getOwnProperty(realm, target, propertyKey);
        // TODO: side-effect in isExtensible()?
        boolean extensibleTarget = IsExtensible(realm, target);
        if (targetDesc == null) {
            if (!extensibleTarget) {
                throw throwTypeError(realm, Messages.Key.ProxyNotExtensible);
            }
            if (!desc.isConfigurable()) {
                throw throwTypeError(realm, Messages.Key.ProxyNotConfigurable);
            }
        } else {
            if (!IsCompatiblePropertyDescriptor(extensibleTarget, desc, targetDesc)) {
                throw throwTypeError(realm, Messages.Key.ProxyIncompatibleDescriptor);
            }
            if (!desc.isConfigurable() && targetDesc.isConfigurable()) {
                throw throwTypeError(realm, Messages.Key.ProxyNotConfigurable);
            }
        }
        return true;
    }

    /**
     * 8.5.8 [[HasProperty]] (P)
     */
    @Override
    public boolean hasProperty(Realm realm, String propertyKey) {
        return hasProperty(realm, (Object) propertyKey);
    }

    /**
     * 8.5.8 [[HasProperty]] (P)
     */
    @Override
    public boolean hasProperty(Realm realm, Symbol propertyKey) {
        return hasProperty(realm, (Object) propertyKey);
    }

    /**
     * 8.5.8 [[HasProperty]] (P)
     */
    private boolean hasProperty(Realm realm, Object propertyKey) {
        ScriptObject handler = proxyHandler;
        ScriptObject target = proxyTarget;
        Callable trap = GetMethod(realm, handler, "has");
        if (trap == null) {
            return __hasProperty(realm, target, propertyKey);
        }
        Object trapResult = trap.call(handler, target, propertyKey);
        boolean success = ToBoolean(trapResult);
        if (!success) {
            Property targetDesc = __getOwnProperty(realm, target, propertyKey);
            if (targetDesc != null) {
                if (!targetDesc.isConfigurable()) {
                    throw throwTypeError(realm, Messages.Key.ProxyNotConfigurable);
                }
                boolean extensibleTarget = IsExtensible(realm, target);
                if (!extensibleTarget) {
                    throw throwTypeError(realm, Messages.Key.ProxyNotExtensible);
                }
            }
        }
        return success;
    }

    /**
     * 8.5.9 [[Get]] (P, Receiver)
     */
    @Override
    public Object get(Realm realm, String propertyKey, Object receiver) {
        return get(realm, (Object) propertyKey, receiver);
    }

    /**
     * 8.5.9 [[Get]] (P, Receiver)
     */
    @Override
    public Object get(Realm realm, Symbol propertyKey, Object receiver) {
        return get(realm, (Object) propertyKey, receiver);
    }

    /**
     * 8.5.9 [[Get]] (P, Receiver)
     */
    private Object get(Realm realm, Object propertyKey, Object receiver) {
        ScriptObject handler = proxyHandler;
        ScriptObject target = proxyTarget;
        Callable trap = GetMethod(realm, handler, "get");
        if (trap == null) {
            return __get(realm, target, propertyKey, receiver);
        }
        Object trapResult = trap.call(handler, target, propertyKey, receiver);
        Property targetDesc = __getOwnProperty(realm, target, propertyKey);
        if (targetDesc != null) {
            if (targetDesc.isDataDescriptor() && !targetDesc.isConfigurable()
                    && !targetDesc.isWritable()) {
                if (!SameValue(trapResult, targetDesc.getValue())) {
                    throw throwTypeError(realm, Messages.Key.ProxySameValue);
                }
            }
            if (targetDesc.isAccessorDescriptor() && !targetDesc.isConfigurable()
                    && targetDesc.getGetter() == null) {
                if (trapResult != UNDEFINED) {
                    throw throwTypeError(realm, Messages.Key.ProxyNoGetter);
                }
            }
        }
        return trapResult;
    }

    /**
     * 8.5.10 [[Set]] ( P, V, Receiver)
     */
    @Override
    public boolean set(Realm realm, String propertyKey, Object value, Object receiver) {
        return set(realm, (Object) propertyKey, value, receiver);
    }

    /**
     * 8.5.10 [[Set]] ( P, V, Receiver)
     */
    @Override
    public boolean set(Realm realm, Symbol propertyKey, Object value, Object receiver) {
        return set(realm, (Object) propertyKey, value, receiver);
    }

    /**
     * 8.5.10 [[Set]] ( P, V, Receiver)
     */
    private boolean set(Realm realm, Object propertyKey, Object value, Object receiver) {
        ScriptObject handler = proxyHandler;
        ScriptObject target = proxyTarget;
        Callable trap = GetMethod(realm, handler, "set");
        if (trap == null) {
            return __set(realm, target, propertyKey, value, receiver);
        }
        Object trapResult = trap.call(handler, target, propertyKey, value, receiver);
        if (!ToBoolean(trapResult)) {
            return false;
        }
        Property targetDesc = __getOwnProperty(realm, target, propertyKey);
        if (targetDesc != null) {
            if (targetDesc.isDataDescriptor() && !targetDesc.isConfigurable()
                    && !targetDesc.isWritable()) {
                if (!SameValue(value, targetDesc.getValue())) {
                    throw throwTypeError(realm, Messages.Key.ProxySameValue);
                }
            }
            if (targetDesc.isAccessorDescriptor() && !targetDesc.isConfigurable()) {
                if (targetDesc.getSetter() == null) {
                    throw throwTypeError(realm, Messages.Key.ProxyNoSetter);
                }
            }
        }
        return true;
    }

    /**
     * 8.5.11 [[Delete]] (P)
     */
    @Override
    public boolean delete(Realm realm, String propertyKey) {
        return delete(realm, (Object) propertyKey);
    }

    /**
     * 8.5.11 [[Delete]] (P)
     */
    @Override
    public boolean delete(Realm realm, Symbol propertyKey) {
        return delete(realm, (Object) propertyKey);
    }

    /**
     * 8.5.11 [[Delete]] (P)
     */
    private boolean delete(Realm realm, Object propertyKey) {
        ScriptObject handler = proxyHandler;
        ScriptObject target = proxyTarget;
        Callable trap = GetMethod(realm, handler, "deleteProperty");
        if (trap == null) {
            return __delete(realm, target, propertyKey);
        }
        Object trapResult = trap.call(handler, target, propertyKey);
        if (!ToBoolean(trapResult)) {
            return false;
        }
        Property targetDesc = __getOwnProperty(realm, target, propertyKey);
        if (targetDesc == null) {
            return true;
        }
        if (!targetDesc.isConfigurable()) {
            throw throwTypeError(realm, Messages.Key.ProxyNotConfigurable);
        }
        return true;
    }

    /**
     * 8.5.12 [[Enumerate]] ()
     */
    @Override
    public ScriptObject enumerate(Realm realm) {
        ScriptObject handler = proxyHandler;
        ScriptObject target = proxyTarget;
        Callable trap = GetMethod(realm, handler, "enumerate");
        if (trap == null) {
            return target.enumerate(realm);
        }
        Object trapResult = trap.call(handler, target);
        if (!Type.isObject(trapResult)) {
            throw throwTypeError(realm, Messages.Key.ProxyNotObject);
        }
        return Type.objectValue(trapResult);
    }

    /**
     * 8.5.13 [[OwnPropertyKeys]] ()
     */
    @Override
    public ScriptObject ownPropertyKeys(Realm realm) {
        ScriptObject handler = proxyHandler;
        ScriptObject target = proxyTarget;
        Callable trap = GetMethod(realm, handler, "ownKeys");
        if (trap == null) {
            return target.ownPropertyKeys(realm);
        }
        Object trapResult = trap.call(handler, target);
        if (!Type.isObject(trapResult)) {
            throw throwTypeError(realm, Messages.Key.ProxyNotObject);
        }
        return Type.objectValue(trapResult);
    }
}
