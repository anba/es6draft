/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.global;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static java.util.Collections.emptyIterator;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ScriptIterator;
import com.github.anba.es6draft.runtime.internal.SimpleIterator;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * Wrapper-Proxy for shell tests
 * 
 * @see MozShellFunctions#wrapWithProto(ExecutionContext, Object, Object)
 */
class WrapperProxy implements ScriptObject {
    /** [[ProxyTarget]] */
    protected final ScriptObject proxyTarget;
    /** [[Prototype]] */
    protected final ScriptObject prototype;

    public WrapperProxy(ScriptObject target, ScriptObject prototype) {
        this.proxyTarget = target;
        this.prototype = prototype;
    }

    @Override
    public String toString() {
        return String.format("%s@%x {{%n\tTarget=%s%n}}", getClass().getSimpleName(), System.identityHashCode(this),
                proxyTarget);
    }

    static final class CallableWrapperProxy extends WrapperProxy implements Callable {
        public CallableWrapperProxy(Callable target, ScriptObject prototype) {
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
        public CallableWrapperProxy clone(ExecutionContext cx) {
            throw newTypeError(cx, Messages.Key.FunctionNotCloneable);
        }

        @Override
        public Realm getRealm(ExecutionContext cx) {
            return ((Callable) proxyTarget).getRealm(cx);
        }
    }

    /**
     * (extension): CreateWrapProxy
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
        if (IsCallable(proxyTarget)) {
            proxy = new CallableWrapperProxy((Callable) proxyTarget, prototype);
        } else {
            proxy = new WrapperProxy(proxyTarget, prototype);
        }
        return proxy;
    }

    protected final ScriptObject getPrototype() {
        return prototype;
    }

    @Override
    public ScriptObject getPrototypeOf(ExecutionContext cx) {
        // Always return the original prototype.
        return prototype;
    }

    @Override
    public boolean setPrototypeOf(ExecutionContext cx, ScriptObject prototype) {
        return proxyTarget.setPrototypeOf(cx, prototype);
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
        /* modified 9.1.7 [[HasProperty]](P) */
        boolean hasOwn = HasOwnProperty(cx, proxyTarget, propertyKey);
        if (hasOwn) {
            return true;
        }
        ScriptObject parent = getPrototype(); // modified
        if (parent != null) {
            return parent.hasProperty(cx, propertyKey);
        }
        return false;
    }

    @Override
    public boolean hasProperty(ExecutionContext cx, String propertyKey) {
        /* modified 9.1.7 [[HasProperty]](P) */
        boolean hasOwn = HasOwnProperty(cx, proxyTarget, propertyKey);
        if (hasOwn) {
            return true;
        }
        ScriptObject parent = getPrototype(); // modified
        if (parent != null) {
            return parent.hasProperty(cx, propertyKey);
        }
        return false;
    }

    @Override
    public boolean hasProperty(ExecutionContext cx, Symbol propertyKey) {
        /* modified 9.1.7 [[HasProperty]](P) */
        boolean hasOwn = HasOwnProperty(cx, proxyTarget, propertyKey);
        if (hasOwn) {
            return true;
        }
        ScriptObject parent = getPrototype(); // modified
        if (parent != null) {
            return parent.hasProperty(cx, propertyKey);
        }
        return false;
    }

    @Override
    public Object get(ExecutionContext cx, long propertyKey, Object receiver) {
        /* modified 9.1.8 [[Get]] (P, Receiver) */
        Property desc = proxyTarget.getOwnProperty(cx, propertyKey);
        if (desc == null) {
            ScriptObject parent = getPrototype();// modified
            if (parent == null) {
                return UNDEFINED;
            }
            return parent.get(cx, propertyKey, receiver);
        }
        if (desc.isDataDescriptor()) {
            return desc.getValue();
        }
        assert desc.isAccessorDescriptor();
        Callable getter = desc.getGetter();
        if (getter == null) {
            return UNDEFINED;
        }
        return getter.call(cx, receiver);
    }

    @Override
    public Object get(ExecutionContext cx, String propertyKey, Object receiver) {
        /* modified 9.1.8 [[Get]] (P, Receiver) */
        Property desc = proxyTarget.getOwnProperty(cx, propertyKey);
        if (desc == null) {
            ScriptObject parent = getPrototype();// modified
            if (parent == null) {
                return UNDEFINED;
            }
            return parent.get(cx, propertyKey, receiver);
        }
        if (desc.isDataDescriptor()) {
            return desc.getValue();
        }
        assert desc.isAccessorDescriptor();
        Callable getter = desc.getGetter();
        if (getter == null) {
            return UNDEFINED;
        }
        return getter.call(cx, receiver);
    }

    @Override
    public Object get(ExecutionContext cx, Symbol propertyKey, Object receiver) {
        /* modified 9.1.8 [[Get]] (P, Receiver) */
        Property desc = proxyTarget.getOwnProperty(cx, propertyKey);
        if (desc == null) {
            ScriptObject parent = getPrototype();// modified
            if (parent == null) {
                return UNDEFINED;
            }
            return parent.get(cx, propertyKey, receiver);
        }
        if (desc.isDataDescriptor()) {
            return desc.getValue();
        }
        assert desc.isAccessorDescriptor();
        Callable getter = desc.getGetter();
        if (getter == null) {
            return UNDEFINED;
        }
        return getter.call(cx, receiver);
    }

    @Override
    public boolean set(ExecutionContext cx, long propertyKey, Object value, Object receiver) {
        /* modified 9.1.9 [[Set] (P, V, Receiver) */
        Property ownDesc = proxyTarget.getOwnProperty(cx, propertyKey);
        if (ownDesc == null) {
            ScriptObject parent = getPrototype();// modified
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
        /* modified 9.1.9 [[Set] (P, V, Receiver) */
        Property ownDesc = proxyTarget.getOwnProperty(cx, propertyKey);
        if (ownDesc == null) {
            ScriptObject parent = getPrototype();// modified
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
        /* modified 9.1.9 [[Set] (P, V, Receiver) */
        Property ownDesc = proxyTarget.getOwnProperty(cx, propertyKey);
        if (ownDesc == null) {
            ScriptObject parent = getPrototype();// modified
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
    public ScriptObject enumerate(ExecutionContext cx) {
        return CreateListIterator(cx, new AppendIterator(cx, proxyTarget, getPrototype()));
    }

    @Override
    public ScriptIterator<?> enumerateKeys(ExecutionContext cx) {
        return ToScriptIterator(cx, enumerate(cx));
    }

    private static final class AppendIterator extends SimpleIterator<Object> {
        private final ExecutionContext cx;
        private final ScriptObject proxyTarget;
        private final Iterator<?> targetKeys;
        private final Iterator<?> protoKeys;
        private final HashSet<Object> visitedKeys = new HashSet<>();

        AppendIterator(ExecutionContext cx, ScriptObject proxyTarget, ScriptObject proto) {
            this.cx = cx;
            this.proxyTarget = proxyTarget;
            this.targetKeys = proxyTarget.ownKeys(cx);
            this.protoKeys = proto != null ? proto.enumerateKeys(cx) : emptyIterator();
        }

        @Override
        protected Object findNext() {
            while (targetKeys.hasNext()) {
                Object k = targetKeys.next();
                if (k instanceof String) {
                    String propertyKey = (String) k;
                    Property property = proxyTarget.getOwnProperty(cx, propertyKey);
                    if (property != null && visitedKeys.add(propertyKey) && property.isEnumerable()) {
                        return propertyKey;
                    }
                }
            }
            while (protoKeys.hasNext()) {
                Object propertyKey = protoKeys.next();
                if (visitedKeys.add(propertyKey)) {
                    return propertyKey;
                }
            }
            return null;
        }
    }

    @Override
    public List<?> ownPropertyKeys(ExecutionContext cx) {
        return proxyTarget.ownPropertyKeys(cx);
    }

    @Override
    public Iterator<?> ownKeys(ExecutionContext cx) {
        return proxyTarget.ownKeys(cx);
    }
}
