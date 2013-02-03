/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateOwnDataProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.MakeObjectSecure;
import static com.github.anba.es6draft.runtime.AbstractOperations.SameValue;
import static com.github.anba.es6draft.runtime.AbstractOperations.TestIfSecureObject;
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

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.BuiltinBrand;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * <h1>8 Types</h1>
 * <ul>
 * <li>8.3 Ordinary Object Internal Methods and Internal Data Properties
 * </ul>
 */
public abstract class OrdinaryObject implements Scriptable {
    // Map<String|Symbol, Property> properties
    private LinkedHashMap<Object, Property> properties = new LinkedHashMap<>();
    private Scriptable prototype = null;
    private boolean extensible = true;
    private final Realm realm;

    public OrdinaryObject(Realm realm) {
        this.realm = realm;
    }

    protected final Realm realm() {
        return realm;
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

    /**
     * [[BuiltinBrand]]
     */
    @Override
    public BuiltinBrand getBuiltinBrand() {
        return null;
    }

    /** 8.3.1 [[GetPrototype]] ( ) */
    @Override
    public Scriptable getPrototype() {
        return prototype;
    }

    /** 8.3.2 [[SetPrototype]] (V) */
    @Override
    public boolean setPrototype(Scriptable prototype) {
        if (!extensible) {
            return false;
        }
        this.prototype = prototype;
        return true;
    }

    /** 8.3.3 [[IsExtensible]] ( ) */
    @Override
    public boolean isExtensible() {
        return extensible;
    }

    /** 8.3.4 [[PreventExtensions]] ( ) */
    @Override
    public void preventExtensions() {
        this.extensible = false;
    }

    /** 8.3.5 [[HasOwnProperty]] (P) */
    @Override
    public boolean hasOwnProperty(String propertyKey) {
        return __has__(propertyKey);
    }

    /** 8.3.5 [[HasOwnProperty]] (P) */
    @Override
    public boolean hasOwnProperty(Symbol propertyKey) {
        return __has__(propertyKey);
    }

    /** 8.3.6 [[GetOwnProperty]] (P) */
    @Override
    public Property getOwnProperty(String propertyKey) {
        return ordinaryGetOwnProperty(propertyKey);
    }

    /** 8.3.6 [[GetOwnProperty]] (P) */
    @Override
    public Property getOwnProperty(Symbol propertyKey) {
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
    public boolean defineOwnProperty(String propertyKey, PropertyDescriptor desc) {
        /* step 1 */
        return ordinaryDefineOwnProperty(propertyKey, desc);
    }

    /** 8.3.7 [[DefineOwnProperty]] (P, Desc) */
    @Override
    public boolean defineOwnProperty(Symbol propertyKey, PropertyDescriptor desc) {
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
            assert !(object != null) || propertyKey != null;
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
     * 8.7.8 [[HasProperty]](P)
     */
    @Override
    public boolean hasProperty(String propertyKey) {
        // FIXME: spec bug ([[GetOwnProperty]] vs. [[HasOwnProperty]] (bug 1205)
        boolean has = hasOwnProperty(propertyKey);
        if (has) {
            return true;
        }
        Scriptable parent = getPrototype();
        if (parent == null) {
            return false;
        }
        return parent.hasProperty(propertyKey);
    }

    /**
     * 8.7.8 [[HasProperty]](P)
     */
    @Override
    public boolean hasProperty(Symbol propertyKey) {
        // FIXME: spec bug ([[GetOwnProperty]] vs. [[HasOwnProperty]] (bug 1205)
        boolean has = hasOwnProperty(propertyKey);
        if (has) {
            return true;
        }
        Scriptable parent = getPrototype();
        if (parent == null) {
            return false;
        }
        return parent.hasProperty(propertyKey);
    }

    /** 8.3.7 [[GetP]] (P, Receiver) */
    @Override
    public Object get(String propertyKey, Object receiver) {
        /* step 2-3 */
        Property desc = getOwnProperty(propertyKey);
        /* step 4 */
        if (desc == null) {
            Scriptable parent = getPrototype();
            if (parent == null) {
                return UNDEFINED;
            }
            return parent.get(propertyKey, receiver);
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
        return getter.call(receiver);
    }

    /** 8.3.7 [[GetP]] (P, Receiver) */
    @Override
    public Object get(Symbol propertyKey, Object receiver) {
        /* step 2-3 */
        Property desc = getOwnProperty(propertyKey);
        /* step 4 */
        if (desc == null) {
            Scriptable parent = getPrototype();
            if (parent == null) {
                return UNDEFINED;
            }
            return parent.get(propertyKey, receiver);
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
        return getter.call(receiver);
    }

    /** 8.3.8 [[SetP] (P, V, Receiver) */
    @Override
    public boolean set(String propertyKey, Object value, Object receiver) {
        /* step 2-3 */
        Property ownDesc = getOwnProperty(propertyKey);
        /* step 4 */
        if (ownDesc == null) {
            Scriptable parent = getPrototype();
            if (parent != null) {
                return parent.set(propertyKey, value, receiver);
            } else {
                if (!Type.isObject(receiver)) {
                    return false;
                }
                return CreateOwnDataProperty(Type.objectValue(receiver), propertyKey, value);
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
            Scriptable _receiver = Type.objectValue(receiver);
            Property existingDescriptor = _receiver.getOwnProperty(propertyKey);
            if (existingDescriptor != null) {
                PropertyDescriptor valueDesc = new PropertyDescriptor(value);
                return _receiver.defineOwnProperty(propertyKey, valueDesc);
            } else {
                return CreateOwnDataProperty(_receiver, propertyKey, value);
            }
        }
        /* step 6 */
        assert ownDesc.isAccessorDescriptor();
        Callable setter = ownDesc.getSetter();
        if (setter == null) {
            return false;
        }
        setter.call(receiver, value);
        return true;
    }

    /** 8.3.8 [[SetP] (P, V, Receiver) */
    @Override
    public boolean set(Symbol propertyKey, Object value, Object receiver) {
        /* step 2-3 */
        Property ownDesc = getOwnProperty(propertyKey);
        /* step 4 */
        if (ownDesc == null) {
            Scriptable parent = getPrototype();
            if (parent != null) {
                return parent.set(propertyKey, value, receiver);
            } else {
                if (!Type.isObject(receiver)) {
                    return false;
                }
                return CreateOwnDataProperty(Type.objectValue(receiver), propertyKey, value);
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
            Scriptable _receiver = Type.objectValue(receiver);
            Property existingDescriptor = _receiver.getOwnProperty(propertyKey);
            if (existingDescriptor != null) {
                PropertyDescriptor valueDesc = new PropertyDescriptor(value);
                return _receiver.defineOwnProperty(propertyKey, valueDesc);
            } else {
                return CreateOwnDataProperty(_receiver, propertyKey, value);
            }
        }
        /* step 6 */
        assert ownDesc.isAccessorDescriptor();
        Callable setter = ownDesc.getSetter();
        if (setter == null) {
            return false;
        }
        setter.call(receiver, value);
        return true;
    }

    /** 8.3.9 [[Delete]] (P) */
    @Override
    public boolean delete(String propertyKey) {
        /* step 2 */
        Property desc = getOwnProperty(propertyKey);
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

    /** 8.3.9 [[Delete]] (P) */
    @Override
    public boolean delete(Symbol propertyKey) {
        /* step 2 */
        Property desc = getOwnProperty(propertyKey);
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

    /** 8.3.11 [[Enumerate]] () */
    @Override
    public final Scriptable enumerate() {
        return MakeListIterator(realm(), new EnumKeysIterator(realm(), this));
    }

    /** 8.3.11 [[Enumerate]] () */
    protected Collection<String> enumerateKeys() {
        List<String> propList = new ArrayList<>();
        for (Object key : __keys__()) {
            if (key instanceof Symbol)
                continue;
            propList.add((String) key);
        }
        return propList;
    }

    private static final class EnumKeysIterator extends SimpleIterator<Object> {
        private final Realm realm;
        private OrdinaryObject obj;
        private HashSet<Object> visitedKeys = new HashSet<>();
        private Iterator<String> keys;
        private Iterator<?> protoKeys;

        EnumKeysIterator(Realm realm, OrdinaryObject obj) {
            this.realm = realm;
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
                    Object key = keys.next();
                    Property prop = obj.__get__(key); // OrdinaryGetOwnProperty
                    if (prop != null && visitedKeys.add(key) && prop.isEnumerable()) {
                        return key;
                    }
                }
                // switch to prototype enumerate
                Scriptable proto = this.obj.getPrototype();
                if (proto != null) {
                    if (proto instanceof OrdinaryObject) {
                        this.obj = ((OrdinaryObject) proto);
                        this.keys = ((OrdinaryObject) proto).enumerateKeys().iterator();
                        return tryNext();
                    } else {
                        this.obj = null;
                        this.keys = null;
                        this.protoKeys = FromListIterator(realm, proto.enumerate());
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
    public Scriptable ownPropertyKeys() {
        List<Object> keys = new ArrayList<>();
        for (Object key : __keys__()) {
            if (key instanceof Symbol && ((Symbol) key).isPrivate())
                continue;
            keys.add(key);
        }
        return MakeListIterator(realm(), keys.iterator());
    }

    /** 8.3.14 [[Freeze]] ( ) */
    @Override
    public void freeze() {
        MakeObjectSecure(realm(), this, true);
    }

    /** 8.3.15 [[Seal]] ( ) */
    @Override
    public void seal() {
        MakeObjectSecure(realm(), this, false);
    }

    /** 8.3.16 [[IsFrozen]] ( ) */
    @Override
    public boolean isFrozen() {
        return TestIfSecureObject(realm(), this, true);
    }

    /** 8.3.17 [[IsSealed]] ( ) */
    @Override
    public boolean isSealed() {
        return TestIfSecureObject(realm(), this, false);
    }

    /** 8.3.18 ObjectCreate Abstract Operation */
    public static Scriptable ObjectCreate(Realm realm) {
        Scriptable proto = realm.getIntrinsic(Intrinsics.ObjectPrototype);
        return ObjectCreate(realm, proto, proto);
    }

    /** 8.3.18 ObjectCreate Abstract Operation */
    public static Scriptable ObjectCreate(Realm realm, Scriptable proto) {
        return ObjectCreate(realm, proto, realm.getIntrinsic(Intrinsics.ObjectPrototype));
    }

    @Override
    public Scriptable newInstance(Realm realm) {
        return new OrdinaryObject(realm) {
        };
    }

    /** 8.3.18 ObjectCreate Abstract Operation (extension) */
    public static Scriptable ObjectCreate(Realm realm, Scriptable proto, Scriptable creator) {
        /* step 1 (implicit) */
        /* step 2, step 3 */
        Scriptable obj = creator.newInstance(realm);
        /* step 4 */
        obj.setPrototype(proto);
        /* step 5 (implicit) */
        // obj.setExtensible(true);
        /* step 6 */
        return obj;
    }
}
