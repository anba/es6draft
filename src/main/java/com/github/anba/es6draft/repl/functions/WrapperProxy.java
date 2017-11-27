/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.functions;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsCallable;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsConstructor;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.HashMap;
import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.PrivateName;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * Wrapper-Proxy for shell tests
 * 
 * @see MozShellFunctions#wrapWithProto(ExecutionContext, Object, Object)
 */
class WrapperProxy implements ScriptObject {
    /** [[ProxyTarget]] */
    protected final ScriptObject proxyTarget;
    /** [[Prototype]] */
    protected ScriptObject prototype;
    // Map for private names
    private final HashMap<PrivateName, Property> privateNames;

    WrapperProxy(ScriptObject target, ScriptObject prototype) {
        this.proxyTarget = target;
        this.prototype = prototype;
        this.privateNames = new HashMap<>();
    }

    @Override
    public String toString() {
        return String.format("%s@%x {{%n\tTarget=%s%n}}", getClass().getSimpleName(), System.identityHashCode(this),
                proxyTarget);
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

    static class CallableWrapperProxy extends WrapperProxy implements Callable {
        CallableWrapperProxy(Callable target, ScriptObject prototype) {
            super(target, prototype);
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            return ((Callable) proxyTarget).call(callerContext, thisValue, args);
        }

        @Override
        public Object tailCall(ExecutionContext callerContext, Object thisValue, Object... args) {
            return call(callerContext, thisValue, args);
        }

        @Override
        public String toSource(ExecutionContext cx) {
            return ((Callable) proxyTarget).toSource(cx);
        }

        @Override
        public Realm getRealm(ExecutionContext cx) {
            return ((Callable) proxyTarget).getRealm(cx);
        }
    }

    static final class ConstructorWrapperProxy extends CallableWrapperProxy implements Constructor {
        ConstructorWrapperProxy(Constructor target, ScriptObject prototype) {
            super(target, prototype);
        }

        @Override
        public ScriptObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
            return ((Constructor) proxyTarget).construct(callerContext, newTarget, args);
        }
    }

    /**
     * CreateWrapProxy(target, proto)
     * 
     * @param cx
     *            the execution context
     * @param target
     *            the proxy target object
     * @param proto
     *            the proxy protoype object
     * @return the new wrapper proxy
     */
    public static WrapperProxy CreateWrapProxy(ExecutionContext cx, Object target, Object proto) {
        if (!Type.isObject(target)) {
            throw newTypeError(cx, Messages.Key.NotObjectType);
        }
        if (!Type.isObjectOrNull(proto)) {
            throw newTypeError(cx, Messages.Key.NotObjectOrNull);
        }
        ScriptObject proxyTarget = Type.objectValue(target);
        ScriptObject prototype = Type.objectValueOrNull(proto);
        WrapperProxy proxy;
        if (IsConstructor(proxyTarget)) {
            proxy = new ConstructorWrapperProxy((Constructor) proxyTarget, prototype);
        } else if (IsCallable(proxyTarget)) {
            proxy = new CallableWrapperProxy((Callable) proxyTarget, prototype);
        } else {
            proxy = new WrapperProxy(proxyTarget, prototype);
        }
        return proxy;
    }

    @Override
    public ScriptObject getPrototypeOf(ExecutionContext cx) {
        return prototype;
    }

    @Override
    public boolean setPrototypeOf(ExecutionContext cx, ScriptObject prototype) {
        if (prototype == this.prototype) {
            return true;
        }
        if (!isExtensible(cx)) {
            return false;
        }
        for (ScriptObject p = prototype; p != null;) {
            if (p == this) {
                return false;
            }
            if (p instanceof OrdinaryObject) {
                p = ((OrdinaryObject) p).getPrototype();
            } else if (p instanceof WrapperProxy) {
                p = ((WrapperProxy) p).prototype;
            } else {
                break;
            }
        }
        this.prototype = prototype;
        return true;
    }

    @Override
    public boolean isExtensible(ExecutionContext cx) {
        return proxyTarget.isExtensible(cx);
    }

    @Override
    public boolean preventExtensions(ExecutionContext cx) {
        return proxyTarget.preventExtensions(cx);
    }

    @Override
    public Property getOwnProperty(ExecutionContext cx, long propertyKey) {
        return proxyTarget.getOwnProperty(cx, propertyKey);
    }

    @Override
    public Property getOwnProperty(ExecutionContext cx, String propertyKey) {
        return proxyTarget.getOwnProperty(cx, propertyKey);
    }

    @Override
    public Property getOwnProperty(ExecutionContext cx, Symbol propertyKey) {
        return proxyTarget.getOwnProperty(cx, propertyKey);
    }

    @Override
    public boolean defineOwnProperty(ExecutionContext cx, long propertyKey, PropertyDescriptor desc) {
        return proxyTarget.defineOwnProperty(cx, propertyKey, desc);
    }

    @Override
    public boolean defineOwnProperty(ExecutionContext cx, String propertyKey, PropertyDescriptor desc) {
        return proxyTarget.defineOwnProperty(cx, propertyKey, desc);
    }

    @Override
    public boolean defineOwnProperty(ExecutionContext cx, Symbol propertyKey, PropertyDescriptor desc) {
        return proxyTarget.defineOwnProperty(cx, propertyKey, desc);
    }

    @Override
    public boolean hasProperty(ExecutionContext cx, long propertyKey) {
        if (proxyTarget.hasOwnProperty(cx, propertyKey)) {
            return true;
        }
        ScriptObject parent = getPrototypeOf(cx);
        if (parent != null) {
            return parent.hasProperty(cx, propertyKey);
        }
        return false;
    }

    @Override
    public boolean hasProperty(ExecutionContext cx, String propertyKey) {
        if (proxyTarget.hasOwnProperty(cx, propertyKey)) {
            return true;
        }
        ScriptObject parent = getPrototypeOf(cx);
        if (parent != null) {
            return parent.hasProperty(cx, propertyKey);
        }
        return false;
    }

    @Override
    public boolean hasProperty(ExecutionContext cx, Symbol propertyKey) {
        if (proxyTarget.hasOwnProperty(cx, propertyKey)) {
            return true;
        }
        ScriptObject parent = getPrototypeOf(cx);
        if (parent != null) {
            return parent.hasProperty(cx, propertyKey);
        }
        return false;
    }

    @Override
    public Object get(ExecutionContext cx, long propertyKey, Object receiver) {
        if (proxyTarget.hasOwnProperty(cx, propertyKey)) {
            return proxyTarget.get(cx, propertyKey, receiver);
        }
        ScriptObject parent = getPrototypeOf(cx);
        if (parent != null) {
            return parent.get(cx, propertyKey, receiver);
        }
        return UNDEFINED;
    }

    @Override
    public Object get(ExecutionContext cx, String propertyKey, Object receiver) {
        if (proxyTarget.hasOwnProperty(cx, propertyKey)) {
            return proxyTarget.get(cx, propertyKey, receiver);
        }
        ScriptObject parent = getPrototypeOf(cx);
        if (parent != null) {
            return parent.get(cx, propertyKey, receiver);
        }
        return UNDEFINED;
    }

    @Override
    public Object get(ExecutionContext cx, Symbol propertyKey, Object receiver) {
        if (proxyTarget.hasOwnProperty(cx, propertyKey)) {
            return proxyTarget.get(cx, propertyKey, receiver);
        }
        ScriptObject parent = getPrototypeOf(cx);
        if (parent != null) {
            return parent.get(cx, propertyKey, receiver);
        }
        return UNDEFINED;
    }

    @Override
    public boolean set(ExecutionContext cx, long propertyKey, Object value, Object receiver) {
        Property ownDesc = proxyTarget.getOwnProperty(cx, propertyKey);
        if (ownDesc == null) {
            ScriptObject parent = getPrototypeOf(cx);
            if (parent != null) {
                return parent.set(cx, propertyKey, value, receiver);
            } else {
                ownDesc = new Property(UNDEFINED, true, true, true);
            }
        }
        if (ownDesc.isDataDescriptor()) {
            if (!ownDesc.isWritable()) {
                return false;
            }
            if (!Type.isObject(receiver)) {
                return false;
            }
            ScriptObject _receiver = Type.objectValue(receiver);
            Property existingDescriptor = _receiver.getOwnProperty(cx, propertyKey);
            if (existingDescriptor != null) {
                PropertyDescriptor valueDesc = new PropertyDescriptor(value);
                return _receiver.defineOwnProperty(cx, propertyKey, valueDesc);
            } else {
                return CreateDataProperty(cx, _receiver, propertyKey, value);
            }
        }
        assert ownDesc.isAccessorDescriptor();
        Callable setter = ownDesc.getSetter();
        if (setter == null) {
            return false;
        }
        setter.call(cx, receiver, value);
        return true;
    }

    @Override
    public boolean set(ExecutionContext cx, String propertyKey, Object value, Object receiver) {
        Property ownDesc = proxyTarget.getOwnProperty(cx, propertyKey);
        if (ownDesc == null) {
            ScriptObject parent = getPrototypeOf(cx);
            if (parent != null) {
                return parent.set(cx, propertyKey, value, receiver);
            } else {
                ownDesc = new Property(UNDEFINED, true, true, true);
            }
        }
        if (ownDesc.isDataDescriptor()) {
            if (!ownDesc.isWritable()) {
                return false;
            }
            if (!Type.isObject(receiver)) {
                return false;
            }
            ScriptObject _receiver = Type.objectValue(receiver);
            Property existingDescriptor = _receiver.getOwnProperty(cx, propertyKey);
            if (existingDescriptor != null) {
                PropertyDescriptor valueDesc = new PropertyDescriptor(value);
                return _receiver.defineOwnProperty(cx, propertyKey, valueDesc);
            } else {
                return CreateDataProperty(cx, _receiver, propertyKey, value);
            }
        }
        assert ownDesc.isAccessorDescriptor();
        Callable setter = ownDesc.getSetter();
        if (setter == null) {
            return false;
        }
        setter.call(cx, receiver, value);
        return true;
    }

    @Override
    public boolean set(ExecutionContext cx, Symbol propertyKey, Object value, Object receiver) {
        Property ownDesc = proxyTarget.getOwnProperty(cx, propertyKey);
        if (ownDesc == null) {
            ScriptObject parent = getPrototypeOf(cx);
            if (parent != null) {
                return parent.set(cx, propertyKey, value, receiver);
            } else {
                ownDesc = new Property(UNDEFINED, true, true, true);
            }
        }
        if (ownDesc.isDataDescriptor()) {
            if (!ownDesc.isWritable()) {
                return false;
            }
            if (!Type.isObject(receiver)) {
                return false;
            }
            ScriptObject _receiver = Type.objectValue(receiver);
            Property existingDescriptor = _receiver.getOwnProperty(cx, propertyKey);
            if (existingDescriptor != null) {
                PropertyDescriptor valueDesc = new PropertyDescriptor(value);
                return _receiver.defineOwnProperty(cx, propertyKey, valueDesc);
            } else {
                return CreateDataProperty(cx, _receiver, propertyKey, value);
            }
        }
        assert ownDesc.isAccessorDescriptor();
        Callable setter = ownDesc.getSetter();
        if (setter == null) {
            return false;
        }
        setter.call(cx, receiver, value);
        return true;
    }

    @Override
    public boolean delete(ExecutionContext cx, long propertyKey) {
        return proxyTarget.delete(cx, propertyKey);
    }

    @Override
    public boolean delete(ExecutionContext cx, String propertyKey) {
        return proxyTarget.delete(cx, propertyKey);
    }

    @Override
    public boolean delete(ExecutionContext cx, Symbol propertyKey) {
        return proxyTarget.delete(cx, propertyKey);
    }

    @Override
    public List<?> ownPropertyKeys(ExecutionContext cx) {
        return proxyTarget.ownPropertyKeys(cx);
    }

    @Override
    public String className() {
        return proxyTarget.className();
    }
}
