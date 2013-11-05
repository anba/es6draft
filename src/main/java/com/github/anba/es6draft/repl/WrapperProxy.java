/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.CreateListIterator;
import static com.github.anba.es6draft.runtime.AbstractOperations.HasOwnProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsCallable;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.objects.internal.ListIterator.FromListIterator;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.HashSet;
import java.util.Iterator;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
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
 * @see MozShellGlobalObject#wrap(Object)
 * @see MozShellGlobalObject#wrapWithProto(Object, Object)
 */
class WrapperProxy implements ScriptObject {
    /** [[ProxyTarget]] */
    protected final ScriptObject proxyTarget;
    /** [[Prototype]] */
    protected final ScriptObject prototype;
    protected final boolean withProto;

    public WrapperProxy(Realm realm, ScriptObject target, ScriptObject prototype, boolean withProto) {
        this.proxyTarget = target;
        this.prototype = prototype;
        this.withProto = withProto;
    }

    private static class CallabeWrapperProxy extends WrapperProxy implements Callable {
        public CallabeWrapperProxy(Realm realm, ScriptObject target, ScriptObject prototype,
                boolean withProto) {
            super(realm, target, prototype, withProto);
        }

        /**
         * [[Call]] (thisArgument, argumentsList)
         */
        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            return ((Callable) proxyTarget).call(callerContext, thisValue, args);
        }

        /**
         * [[Call]] (thisArgument, argumentsList)
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

    /**
     * Abstract Operation (extension): CreateWrapProxy
     */
    public static WrapperProxy CreateWrapProxy(ExecutionContext cx, Object target) {
        if (!Type.isObject(target)) {
            throwTypeError(cx, Messages.Key.NotObjectType);
        }
        ScriptObject proxyTarget = Type.objectValue(target);
        ScriptObject prototype = proxyTarget.getPrototypeOf(cx);
        WrapperProxy proxy;
        if (IsCallable(proxyTarget)) {
            proxy = new CallabeWrapperProxy(cx.getRealm(), proxyTarget, prototype, false);
        } else {
            proxy = new WrapperProxy(cx.getRealm(), proxyTarget, prototype, false);
        }
        return proxy;
    }

    /**
     * Abstract Operation (extension): CreateWrapProxy
     */
    public static WrapperProxy CreateWrapProxy(ExecutionContext cx, Object target, Object proto) {
        if (!Type.isObject(target)) {
            throwTypeError(cx, Messages.Key.NotObjectType);
        }
        if (!(Type.isObject(proto) || Type.isNull(proto))) {
            throwTypeError(cx, Messages.Key.NotObjectOrNull);
        }
        ScriptObject proxyTarget = Type.objectValue(target);
        ScriptObject prototype = Type.isObject(proto) ? Type.objectValue(proto) : null;
        WrapperProxy proxy;
        if (IsCallable(proxyTarget)) {
            proxy = new CallabeWrapperProxy(cx.getRealm(), proxyTarget, prototype, true);
        } else {
            proxy = new WrapperProxy(cx.getRealm(), proxyTarget, prototype, true);
        }
        return proxy;
    }

    protected final ScriptObject getProto(ExecutionContext cx) {
        return (withProto ? prototype : proxyTarget.getPrototypeOf(cx));
    }

    /**
     * [[GetPrototypeOf]] ( )
     */
    @Override
    public ScriptObject getPrototypeOf(ExecutionContext cx) {
        // don't pay attention to the 'withProto' flag, always return the original prototype
        return prototype;
    }

    /**
     * [[SetPrototypeOf]] (V)
     */
    @Override
    public boolean setPrototypeOf(ExecutionContext cx, ScriptObject prototype) {
        return proxyTarget.setPrototypeOf(cx, prototype);
    }

    /**
     * [[IsExtensible]] ()
     */
    @Override
    public boolean isExtensible(ExecutionContext cx) {
        return proxyTarget.isExtensible(cx);
    }

    /**
     * [[PreventExtensions]] ()
     */
    @Override
    public boolean preventExtensions(ExecutionContext cx) {
        return proxyTarget.preventExtensions(cx);
    }

    /**
     * [[GetOwnProperty]] (P)
     */
    @Override
    public Property getOwnProperty(ExecutionContext cx, String propertyKey) {
        return proxyTarget.getOwnProperty(cx, propertyKey);
    }

    /**
     * [[GetOwnProperty]] (P)
     */
    @Override
    public Property getOwnProperty(ExecutionContext cx, Symbol propertyKey) {
        return proxyTarget.getOwnProperty(cx, propertyKey);
    }

    /**
     * [[DefineOwnProperty]] (P, Desc)
     */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, String propertyKey,
            PropertyDescriptor desc) {
        return proxyTarget.defineOwnProperty(cx, propertyKey, desc);
    }

    /**
     * [[DefineOwnProperty]] (P, Desc)
     */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, Symbol propertyKey,
            PropertyDescriptor desc) {
        return proxyTarget.defineOwnProperty(cx, propertyKey, desc);
    }

    /**
     * [[HasProperty]] (P)
     */
    @Override
    public boolean hasProperty(ExecutionContext cx, String propertyKey) {
        /* modified 8.3.8 [[HasProperty]](P) */
        boolean hasOwn = HasOwnProperty(cx, proxyTarget, propertyKey);
        if (!hasOwn) {
            ScriptObject parent = getProto(cx); // modified
            if (parent != null) {
                return parent.hasProperty(cx, propertyKey);
            }
        }
        return hasOwn;
    }

    /**
     * [[HasProperty]] (P)
     */
    @Override
    public boolean hasProperty(ExecutionContext cx, Symbol propertyKey) {
        /* modified 8.3.8 [[HasProperty]](P) */
        boolean hasOwn = HasOwnProperty(cx, proxyTarget, propertyKey);
        if (!hasOwn) {
            ScriptObject parent = getProto(cx);// modified
            if (parent != null) {
                return parent.hasProperty(cx, propertyKey);
            }
        }
        return hasOwn;
    }

    /**
     * [[Get]] (P, Receiver)
     */
    @Override
    public Object get(ExecutionContext cx, String propertyKey, Object receiver) {
        /* modified 8.3.9 [[Get]] (P, Receiver) */
        Property desc = proxyTarget.getOwnProperty(cx, propertyKey);
        if (desc == null) {
            ScriptObject parent = getProto(cx);// modified
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

    /**
     * [[Get]] (P, Receiver)
     */
    @Override
    public Object get(ExecutionContext cx, Symbol propertyKey, Object receiver) {
        /* modified 8.3.9 [[Get]] (P, Receiver) */
        Property desc = proxyTarget.getOwnProperty(cx, propertyKey);
        if (desc == null) {
            ScriptObject parent = getProto(cx);// modified
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

    /**
     * [[Set]] ( P, V, Receiver)
     */
    @Override
    public boolean set(ExecutionContext cx, String propertyKey, Object value, Object receiver) {
        /* modified 8.3.10 [[Set] (P, V, Receiver) */
        Property ownDesc = proxyTarget.getOwnProperty(cx, propertyKey);
        if (ownDesc == null) {
            ScriptObject parent = getProto(cx);// modified
            if (parent != null) {
                return parent.set(cx, propertyKey, value, receiver);
            } else {
                if (!Type.isObject(receiver)) {
                    return false;
                }
                return CreateDataProperty(cx, Type.objectValue(receiver), propertyKey, value);
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

    /**
     * [[Set]] ( P, V, Receiver)
     */
    @Override
    public boolean set(ExecutionContext cx, Symbol propertyKey, Object value, Object receiver) {
        /* modified 8.3.10 [[Set] (P, V, Receiver) */
        Property ownDesc = proxyTarget.getOwnProperty(cx, propertyKey);
        if (ownDesc == null) {
            ScriptObject parent = getProto(cx);// modified
            if (parent != null) {
                return parent.set(cx, propertyKey, value, receiver);
            } else {
                if (!Type.isObject(receiver)) {
                    return false;
                }
                return CreateDataProperty(cx, Type.objectValue(receiver), propertyKey, value);
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

    /**
     * [[Invoke]] (P, ArgumentsList, Receiver)
     */
    @Override
    public Object invoke(ExecutionContext cx, String propertyKey, Object[] arguments,
            Object receiver) {
        return proxyTarget.invoke(cx, propertyKey, arguments, receiver);
    }

    /**
     * [[Invoke]] (P, ArgumentsList, Receiver)
     */
    @Override
    public Object invoke(ExecutionContext cx, Symbol propertyKey, Object[] arguments,
            Object receiver) {
        return proxyTarget.invoke(cx, propertyKey, arguments, receiver);
    }

    /**
     * [[Delete]] (P)
     */
    @Override
    public boolean delete(ExecutionContext cx, String propertyKey) {
        return proxyTarget.delete(cx, propertyKey);
    }

    /**
     * [[Delete]] (P)
     */
    @Override
    public boolean delete(ExecutionContext cx, Symbol propertyKey) {
        return proxyTarget.delete(cx, propertyKey);
    }

    /**
     * [[Enumerate]] ()
     */
    @Override
    public ScriptObject enumerate(ExecutionContext cx) {
        return CreateListIterator(cx, new AppendIterator(cx, proxyTarget, getProto(cx)));
    }

    private static final class AppendIterator extends SimpleIterator<Object> {
        private final ExecutionContext cx;
        private final ScriptObject proxyTarget;
        private final Iterator<?> targetKeys;
        private final Iterator<?> protoKeys;
        private HashSet<Object> visitedKeys = new HashSet<>();

        AppendIterator(ExecutionContext cx, ScriptObject proxyTarget, ScriptObject proto) {
            this.cx = cx;
            this.proxyTarget = proxyTarget;
            this.targetKeys = FromListIterator(cx, proxyTarget, proxyTarget.ownPropertyKeys(cx));
            this.protoKeys = FromListIterator(cx, proto, proto.enumerate(cx));
        }

        @Override
        protected Object tryNext() {
            while (targetKeys.hasNext()) {
                Object k = targetKeys.next();
                if (k instanceof String) {
                    String propertyKey = (String) k;
                    Property property = proxyTarget.getOwnProperty(cx, propertyKey);
                    if (property != null && property.isEnumerable() && visitedKeys.add(propertyKey)) {
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

    /**
     * [[OwnPropertyKeys]] ()
     */
    @Override
    public ScriptObject ownPropertyKeys(ExecutionContext cx) {
        return proxyTarget.ownPropertyKeys(cx);
    }
}
