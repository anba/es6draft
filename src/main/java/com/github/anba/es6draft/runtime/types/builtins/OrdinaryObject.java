/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateOwnDataProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.SameValue;
import static com.github.anba.es6draft.runtime.AbstractOperations.SetIntegrityLevel;
import static com.github.anba.es6draft.runtime.AbstractOperations.TestIntegrityLevel;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ListIterator.FromListIterator;
import static com.github.anba.es6draft.runtime.types.builtins.ListIterator.MakeListIterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.IntegrityLevel;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * <h1>8 Types</h1>
 * <ul>
 * <li>8.3 Ordinary Object Internal Methods and Internal Data Properties
 * </ul>
 */
public abstract class OrdinaryObject implements ScriptObject {
    // Map<String|Symbol, Property> properties
    private LinkedHashMap<Object, Property> properties = new LinkedHashMap<>();

    /** [[Realm]] */
    @SuppressWarnings("unused")
    private final Realm realm;

    /** [[Prototype]] */
    private ScriptObject prototype = null;

    /** [[Extensible]] */
    private boolean extensible = true;

    public OrdinaryObject(Realm realm) {
        this.realm = realm;
    }

    /** [[Extensible]] */
    protected final boolean isExtensible() {
        return extensible;
    }

    private void __put__(Object propertyKey, Property property) {
        assert propertyKey instanceof String || propertyKey instanceof Symbol;
        properties.put(propertyKey, property);
    }

    private boolean __has__(Object propertyKey) {
        assert propertyKey instanceof String || propertyKey instanceof Symbol;
        return properties.containsKey(propertyKey);
    }

    private Property __get__(Object propertyKey) {
        assert propertyKey instanceof String || propertyKey instanceof Symbol;
        return properties.get(propertyKey);
    }

    private void __delete__(Object propertyKey) {
        assert propertyKey instanceof String || propertyKey instanceof Symbol;
        properties.remove(propertyKey);
    }

    private Set<Object> __keys__() {
        return properties.keySet();
    }

    /** 8.3.1 [[GetPrototype]] ( ) */
    @Override
    public ScriptObject getPrototype(ExecutionContext cx) {
        return prototype;
    }

    /** 8.3.2 [[SetPrototype]] (V) */
    @Override
    public boolean setPrototype(ExecutionContext cx, ScriptObject prototype) {
        /* steps 1-3 */
        boolean extensible = this.extensible;
        ScriptObject current = this.prototype;
        /* step 4 */
        if (prototype == current) { // SameValue(prototype, current)
            return true;
        }
        /* step 5 */
        if (!extensible) {
            return false;
        }
        /* step 6 */
        if (prototype != null) {
            ScriptObject p = prototype;
            while (p != null) {
                if (p == this) { // SameValue(p, O)
                    return false;
                }
                p = p.getPrototype(cx);
            }
        }
        /* step 7 */
        this.prototype = prototype;
        /* step 8 */
        return true;
    }

    /** 8.3.3 [[HasIntegrity]] ( Level ) */
    @Override
    public boolean hasIntegrity(ExecutionContext cx, IntegrityLevel level) {
        if (level == IntegrityLevel.NonExtensible) {
            return !extensible;
        }
        return TestIntegrityLevel(cx, this, level);
    }

    /** 8.3.4 [[SetIntegrity]] ( Level ) */
    @Override
    public boolean setIntegrity(ExecutionContext cx, IntegrityLevel level) {
        this.extensible = false;
        if (level != IntegrityLevel.NonExtensible) {
            return SetIntegrityLevel(cx, this, level);
        }
        return true;
    }

    /** 8.3.5 [[HasOwnProperty]] (P) */
    @Override
    public boolean hasOwnProperty(ExecutionContext cx, String propertyKey) {
        return __has__(propertyKey);
    }

    /** 8.3.5 [[HasOwnProperty]] (P) */
    @Override
    public boolean hasOwnProperty(ExecutionContext cx, Symbol propertyKey) {
        return __has__(propertyKey);
    }

    /** 8.3.6 [[GetOwnProperty]] (P) */
    @Override
    public Property getOwnProperty(ExecutionContext cx, String propertyKey) {
        return ordinaryGetOwnProperty(propertyKey);
    }

    /** 8.3.6 [[GetOwnProperty]] (P) */
    @Override
    public Property getOwnProperty(ExecutionContext cx, Symbol propertyKey) {
        return ordinaryGetOwnProperty(propertyKey);
    }

    /**
     * 8.3.6.1 OrdinaryGetOwnProperty (O, P)
     */
    protected final Property ordinaryGetOwnProperty(String propertyKey) {
        Property desc = __get__(propertyKey);
        /* step 2 */
        if (desc == null) {
            return null;
        }
        /* step 3-9 */
        return desc;
    }

    /**
     * 8.3.6.1 OrdinaryGetOwnProperty (O, P)
     */
    protected final Property ordinaryGetOwnProperty(Symbol propertyKey) {
        Property desc = __get__(propertyKey);
        /* step 2 */
        if (desc == null) {
            return null;
        }
        /* step 3-9 */
        return desc;
    }

    /** 8.3.7 [[DefineOwnProperty]] (P, Desc) */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, String propertyKey,
            PropertyDescriptor desc) {
        /* step 1 */
        return ordinaryDefineOwnProperty(propertyKey, desc);
    }

    /** 8.3.7 [[DefineOwnProperty]] (P, Desc) */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, Symbol propertyKey,
            PropertyDescriptor desc) {
        /* step 1 */
        return ordinaryDefineOwnProperty(propertyKey, desc);
    }

    /**
     * 8.3.7.1 OrdinaryDefineOwnProperty (O, P, Desc)
     */
    protected final boolean ordinaryDefineOwnProperty(String propertyKey, PropertyDescriptor desc) {
        /* step 1 */
        Property current = ordinaryGetOwnProperty(propertyKey);
        /* step 2 */
        boolean extensible = isExtensible();
        /* step 3 */
        return __validateAndApplyPropertyDescriptor(this, propertyKey, extensible, desc, current);
    }

    /**
     * 8.3.7.1 OrdinaryDefineOwnProperty (O, P, Desc)
     */
    protected final boolean ordinaryDefineOwnProperty(Symbol propertyKey, PropertyDescriptor desc) {
        /* step 1 */
        Property current = ordinaryGetOwnProperty(propertyKey);
        /* step 2 */
        boolean extensible = isExtensible();
        /* step 3 */
        return __validateAndApplyPropertyDescriptor(this, propertyKey, extensible, desc, current);
    }

    /**
     * 8.3.7.2 IsCompatiblePropertyDescriptor (Extensible, Desc, Current)
     */
    protected static final boolean IsCompatiblePropertyDescriptor(boolean extensible,
            PropertyDescriptor desc, Property current) {
        /* step 1 */
        return __validateAndApplyPropertyDescriptor(null, null, extensible, desc, current);
    }

    /**
     * 8.3.7.3 ValidateAndApplyPropertyDescriptor (O, P, extensible, Desc, current)
     */
    protected static final boolean ValidateAndApplyPropertyDescriptor(OrdinaryObject object,
            String propertyKey, boolean extensible, PropertyDescriptor desc, Property current) {
        return __validateAndApplyPropertyDescriptor(object, propertyKey, extensible, desc, current);
    }

    /**
     * 8.3.7.3 ValidateAndApplyPropertyDescriptor (O, P, extensible, Desc, current)
     */
    protected static final boolean ValidateAndApplyPropertyDescriptor(OrdinaryObject object,
            Symbol propertyKey, boolean extensible, PropertyDescriptor desc, Property current) {
        return __validateAndApplyPropertyDescriptor(object, propertyKey, extensible, desc, current);
    }

    /**
     * 8.3.7.3 ValidateAndApplyPropertyDescriptor (O, P, extensible, Desc, current)
     */
    private static final boolean __validateAndApplyPropertyDescriptor(OrdinaryObject object,
            Object propertyKey, boolean extensible, PropertyDescriptor desc, Property current) {
        @SuppressWarnings("unused")
        String reason;
        reject: {
            /* step 1 */
            assert (object == null || propertyKey != null);
            /* step 2 */
            if (current == null && !extensible) {
                reason = "not extensible";
                break reject;
            }
            /* step 3 */
            if (current == null && extensible) {
                if (desc.isGenericDescriptor() || desc.isDataDescriptor()) {
                    if (object != null) {
                        object.__put__(propertyKey, desc.toProperty());
                    }
                } else {
                    assert desc.isAccessorDescriptor();
                    if (object != null) {
                        object.__put__(propertyKey, desc.toProperty());
                    }
                }
                return true;
            }
            /* step 4 */
            if (desc.isEmpty()) {
                return true;
            }
            /* step 5 */
            if (current.isSubset(desc)) {
                return true;
            }
            /* step 6 */
            if (!current.isConfigurable()) {
                if (desc.isConfigurable()) {
                    reason = "changing configurable";
                    break reject;
                }
                if (desc.hasEnumerable() && desc.isEnumerable() != current.isEnumerable()) {
                    reason = "changing enumerable";
                    break reject;
                }
            }
            if (desc.isGenericDescriptor()) {
                /* step 7 */
                // no further validation required, proceed below...
            } else if (desc.isDataDescriptor() != current.isDataDescriptor()) {
                /* step 8 */
                if (!current.isConfigurable()) {
                    reason = "changing data/accessor";
                    break reject;
                }
                if (current.isDataDescriptor()) {
                    if (object != null) {
                        object.__get__(propertyKey).toAccessorProperty();
                    }
                } else {
                    if (object != null) {
                        object.__get__(propertyKey).toDataProperty();
                    }
                }
            } else if (desc.isDataDescriptor() && current.isDataDescriptor()) {
                /* step 9 */
                if (!current.isConfigurable()) {
                    if (!current.isWritable() && desc.isWritable()) {
                        reason = "changing writable";
                        break reject;
                    }
                    if (!current.isWritable()) {
                        if (desc.hasValue() && !SameValue(desc.getValue(), current.getValue())) {
                            reason = "changing value";
                            break reject;
                        }
                    }
                }
            } else {
                /* step 10 */
                assert desc.isAccessorDescriptor() && current.isAccessorDescriptor();
                if (!current.isConfigurable()) {
                    if (desc.hasSetter() && !SameValue(desc.getSetter(), current.getSetter())) {
                        reason = "changing setter";
                        break reject;
                    }
                    if (desc.hasGetter() && !SameValue(desc.getGetter(), current.getGetter())) {
                        reason = "changing getter";
                        break reject;
                    }
                }
            }
            /* step 11 */
            if (object != null) {
                object.__get__(propertyKey).apply(desc);
            }
            /* step 12 */
            return true;
        }
        return false;
    }

    /**
     * 8.3.8 [[HasProperty]](P)
     */
    @Override
    public boolean hasProperty(ExecutionContext cx, String propertyKey) {
        boolean hasOwn = hasOwnProperty(cx, propertyKey);
        if (!hasOwn) {
            ScriptObject parent = getPrototype(cx);
            if (parent != null) {
                return parent.hasProperty(cx, propertyKey);
            }
        }
        return hasOwn;
    }

    /**
     * 8.3.8 [[HasProperty]](P)
     */
    @Override
    public boolean hasProperty(ExecutionContext cx, Symbol propertyKey) {
        boolean hasOwn = hasOwnProperty(cx, propertyKey);
        if (!hasOwn) {
            ScriptObject parent = getPrototype(cx);
            if (parent != null) {
                return parent.hasProperty(cx, propertyKey);
            }
        }
        return hasOwn;
    }

    /** 8.3.9 [[Get]] (P, Receiver) */
    @Override
    public Object get(ExecutionContext cx, String propertyKey, Object receiver) {
        /* step 2-3 */
        Property desc = getOwnProperty(cx, propertyKey);
        /* step 4 */
        if (desc == null) {
            ScriptObject parent = getPrototype(cx);
            if (parent == null) {
                return UNDEFINED;
            }
            return parent.get(cx, propertyKey, receiver);
        }
        /* step 5 */
        if (desc.isDataDescriptor()) {
            return desc.getValue();
        }
        assert desc.isAccessorDescriptor();
        /* step 6 */
        Callable getter = desc.getGetter();
        /* step 7 */
        if (getter == null) {
            return UNDEFINED;
        }
        /* step 8 */
        return getter.call(cx, receiver);
    }

    /** 8.3.9 [[Get]] (P, Receiver) */
    @Override
    public Object get(ExecutionContext cx, Symbol propertyKey, Object receiver) {
        /* step 2-3 */
        Property desc = getOwnProperty(cx, propertyKey);
        /* step 4 */
        if (desc == null) {
            ScriptObject parent = getPrototype(cx);
            if (parent == null) {
                return UNDEFINED;
            }
            return parent.get(cx, propertyKey, receiver);
        }
        /* step 5 */
        if (desc.isDataDescriptor()) {
            return desc.getValue();
        }
        assert desc.isAccessorDescriptor();
        /* step 6 */
        Callable getter = desc.getGetter();
        /* step 7 */
        if (getter == null) {
            return UNDEFINED;
        }
        /* step 8 */
        return getter.call(cx, receiver);
    }

    /** 8.3.10 [[Set] (P, V, Receiver) */
    @Override
    public boolean set(ExecutionContext cx, String propertyKey, Object value, Object receiver) {
        /* step 2-3 */
        Property ownDesc = getOwnProperty(cx, propertyKey);
        /* step 4 */
        if (ownDesc == null) {
            ScriptObject parent = getPrototype(cx);
            if (parent != null) {
                return parent.set(cx, propertyKey, value, receiver);
            } else {
                if (!Type.isObject(receiver)) {
                    return false;
                }
                return CreateOwnDataProperty(cx, Type.objectValue(receiver), propertyKey, value);
            }
        }
        /* step 5 */
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
                return CreateOwnDataProperty(cx, _receiver, propertyKey, value);
            }
        }
        /* step 6 */
        assert ownDesc.isAccessorDescriptor();
        Callable setter = ownDesc.getSetter();
        if (setter == null) {
            return false;
        }
        setter.call(cx, receiver, value);
        return true;
    }

    /** 8.3.10 [[Set] (P, V, Receiver) */
    @Override
    public boolean set(ExecutionContext cx, Symbol propertyKey, Object value, Object receiver) {
        /* step 2-3 */
        Property ownDesc = getOwnProperty(cx, propertyKey);
        /* step 4 */
        if (ownDesc == null) {
            ScriptObject parent = getPrototype(cx);
            if (parent != null) {
                return parent.set(cx, propertyKey, value, receiver);
            } else {
                if (!Type.isObject(receiver)) {
                    return false;
                }
                return CreateOwnDataProperty(cx, Type.objectValue(receiver), propertyKey, value);
            }
        }
        /* step 5 */
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
                return CreateOwnDataProperty(cx, _receiver, propertyKey, value);
            }
        }
        /* step 6 */
        assert ownDesc.isAccessorDescriptor();
        Callable setter = ownDesc.getSetter();
        if (setter == null) {
            return false;
        }
        setter.call(cx, receiver, value);
        return true;
    }

    /** 8.3.11 [[Delete]] (P) */
    @Override
    public boolean delete(ExecutionContext cx, String propertyKey) {
        /* step 2 */
        Property desc = getOwnProperty(cx, propertyKey);
        /* step 3 */
        if (desc == null) {
            return true;
        }
        /* step 4 */
        if (desc.isConfigurable()) {
            __delete__(propertyKey);
            return true;
        }
        /* step 5 */
        return false;
    }

    /** 8.3.11 [[Delete]] (P) */
    @Override
    public boolean delete(ExecutionContext cx, Symbol propertyKey) {
        /* step 2 */
        Property desc = getOwnProperty(cx, propertyKey);
        /* step 3 */
        if (desc == null) {
            return true;
        }
        /* step 4 */
        if (desc.isConfigurable()) {
            __delete__(propertyKey);
            return true;
        }
        /* step 5 */
        return false;
    }

    /** 8.3.12 [[Enumerate]] () */
    @Override
    public final ScriptObject enumerate(ExecutionContext cx) {
        return MakeListIterator(cx, new EnumKeysIterator(cx, this));
    }

    /** 8.3.12 [[Enumerate]] () */
    protected Collection<String> enumerateKeys() {
        List<String> propList = new ArrayList<>();
        for (Object key : __keys__()) {
            if (key instanceof String) {
                propList.add((String) key);
            }
        }
        return propList;
    }

    protected boolean isEnumerableOwnProperty(String key) {
        Property prop = ordinaryGetOwnProperty(key);
        return (prop != null && prop.isEnumerable());
    }

    private static final class EnumKeysIterator extends SimpleIterator<Object> {
        private final ExecutionContext cx;
        private OrdinaryObject obj;
        private HashSet<Object> visitedKeys = new HashSet<>();
        private Iterator<String> keys;
        private Iterator<?> protoKeys;

        EnumKeysIterator(ExecutionContext cx, OrdinaryObject obj) {
            this.cx = cx;
            this.obj = obj;
            this.keys = obj.enumerateKeys().iterator();
        }

        @Override
        protected Object tryNext() {
            HashSet<Object> visitedKeys = this.visitedKeys;
            Iterator<String> keys = this.keys;
            if (keys != null) {
                assert protoKeys == null;
                while (keys.hasNext()) {
                    String key = keys.next();
                    if (visitedKeys.add(key) && obj.isEnumerableOwnProperty(key)) {
                        return key;
                    }
                }
                // switch to prototype enumerate
                ScriptObject proto = this.obj.getPrototype(cx);
                if (proto != null) {
                    if (proto instanceof OrdinaryObject) {
                        this.obj = ((OrdinaryObject) proto);
                        this.keys = ((OrdinaryObject) proto).enumerateKeys().iterator();
                        return tryNext();
                    } else {
                        this.obj = null;
                        this.keys = null;
                        this.protoKeys = FromListIterator(cx, proto.enumerate(cx));
                    }
                } else {
                    this.obj = null;
                    this.keys = null;
                    this.protoKeys = null;
                }
            }
            Iterator<?> protoKeys = this.protoKeys;
            if (protoKeys != null) {
                while (protoKeys.hasNext()) {
                    Object key = protoKeys.next();
                    if (visitedKeys.add(key)) {
                        return key;
                    }
                }
                // visited all inherited keys
                this.protoKeys = null;
            }
            return null;
        }
    }

    private static abstract class SimpleIterator<KEY> implements Iterator<KEY> {
        private KEY nextKey = null;

        protected abstract KEY tryNext();

        @Override
        public final boolean hasNext() {
            if (nextKey == null) {
                nextKey = tryNext();
            }
            return (nextKey != null);
        }

        @Override
        public final KEY next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            KEY key = nextKey;
            nextKey = null;
            return key;
        }

        @Override
        public final void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /** 8.3.13 [[OwnPropertyKeys]] ( ) */
    @Override
    public final ScriptObject ownPropertyKeys(ExecutionContext cx) {
        return MakeListIterator(cx, enumerateOwnKeys().iterator());
    }

    /** 8.3.13 [[OwnPropertyKeys]] ( ) */
    protected Collection<Object> enumerateOwnKeys() {
        List<Object> keys = new ArrayList<>();
        for (Object key : __keys__()) {
            if (!(key instanceof Symbol && ((Symbol) key).isPrivate())) {
                keys.add(key);
            }
        }
        return keys;
    }

    private static class DefaultAllocator implements ObjectAllocator<OrdinaryObject> {
        static final ObjectAllocator<OrdinaryObject> INSTANCE = new DefaultAllocator();

        static class DefaultObject extends OrdinaryObject {
            public DefaultObject(Realm realm) {
                super(realm);
            }
        }

        @Override
        public OrdinaryObject newInstance(Realm realm) {
            return new DefaultObject(realm);
        }
    }

    /** 8.3.14 ObjectCreate Abstract Operation */
    public static OrdinaryObject ObjectCreate(ExecutionContext cx, ScriptObject proto) {
        return ObjectCreate(cx, proto, DefaultAllocator.INSTANCE);
    }

    /** 8.3.14 ObjectCreate Abstract Operation */
    public static OrdinaryObject ObjectCreate(ExecutionContext cx, Intrinsics proto) {
        return ObjectCreate(cx, proto, DefaultAllocator.INSTANCE);
    }

    /** 8.3.14 ObjectCreate Abstract Operation */
    public static <OBJECT extends ScriptObject> OBJECT ObjectCreate(ExecutionContext cx,
            ScriptObject proto, ObjectAllocator<OBJECT> allocator) {
        OBJECT obj = allocator.newInstance(cx.getRealm());
        obj.setPrototype(cx, proto);
        return obj;
    }

    /** 8.3.14 ObjectCreate Abstract Operation */
    public static <OBJECT extends ScriptObject> OBJECT ObjectCreate(ExecutionContext cx,
            Intrinsics proto, ObjectAllocator<OBJECT> allocator) {
        OBJECT obj = allocator.newInstance(cx.getRealm());
        obj.setPrototype(cx, cx.getIntrinsic(proto));
        return obj;
    }
}
