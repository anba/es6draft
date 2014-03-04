/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.CreateListIterator;
import static com.github.anba.es6draft.runtime.AbstractOperations.GetPrototypeFromConstructor;
import static com.github.anba.es6draft.runtime.AbstractOperations.SameValue;
import static com.github.anba.es6draft.runtime.objects.internal.ListIterator.FromScriptIterator;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.SimpleIterator;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1>
 * <ul>
 * <li>9.1 Ordinary Object Internal Methods and Internal Slots
 * </ul>
 */
public class OrdinaryObject implements ScriptObject {
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

    /**
     * Internal hook for ExoticArray
     */
    final void addProperty(String propertyKey, Property property) {
        __put__(propertyKey, property);
    }

    /**
     * Internal hook for FunctionObject
     */
    final void inheritProperties(OrdinaryObject source) {
        assert properties.isEmpty() : "properties not empty";
        properties.putAll(source.properties);
    }

    /** [[Prototype]] */
    public final ScriptObject getPrototype() {
        return prototype;
    }

    /** [[Prototype]] */
    public final void setPrototype(ScriptObject prototype) {
        this.prototype = prototype;
    }

    /** [[Extensible]] */
    protected final boolean isExtensible() {
        return extensible;
    }

    /** [[Extensible]] */
    protected final void setExtensible(boolean extensible) {
        assert this.extensible || !extensible;
        this.extensible = extensible;
    }

    /** [[HasOwnProperty]] (P) */
    protected boolean hasOwnProperty(String propertyKey) {
        // optimised: HasOwnProperty(cx, this, propertyKey)
        return __has__(propertyKey);
    }

    /** [[HasOwnProperty]] (P) */
    protected boolean hasOwnProperty(Symbol propertyKey) {
        // optimised: HasOwnProperty(cx, this, propertyKey)
        return __has__(propertyKey);
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

    /** 9.1.1 [[GetPrototypeOf]] ( ) */
    @Override
    public ScriptObject getPrototypeOf(ExecutionContext cx) {
        /* step 1 */
        return prototype;
    }

    /** 9.1.2 [[SetPrototypeOf]] (V) */
    @Override
    public boolean setPrototypeOf(ExecutionContext cx, ScriptObject prototype) {
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
                p = p.getPrototypeOf(cx);
            }
        }
        /* step 7 */
        this.prototype = prototype;
        /* step 8 */
        return true;
    }

    /** 9.1.3 [[IsExtensible]] ( ) */
    @Override
    public boolean isExtensible(ExecutionContext cx) {
        /* step 1 */
        return extensible;
    }

    /** 9.1.4 [[PreventExtensions]] ( ) */
    @Override
    public boolean preventExtensions(ExecutionContext cx) {
        /* step 1 */
        this.extensible = false;
        /* step 2 */
        return true;
    }

    /** 9.1.5 [[GetOwnProperty]] (P) */
    @Override
    public Property getOwnProperty(ExecutionContext cx, String propertyKey) {
        /* step 1 */
        return ordinaryGetOwnProperty(propertyKey);
    }

    /** 9.1.5 [[GetOwnProperty]] (P) */
    @Override
    public Property getOwnProperty(ExecutionContext cx, Symbol propertyKey) {
        /* step 1 */
        return ordinaryGetOwnProperty(propertyKey);
    }

    /**
     * 9.1.5.1 OrdinaryGetOwnProperty (O, P)
     */
    protected final Property ordinaryGetOwnProperty(String propertyKey) {
        /* step 1 (implicit) */
        Property desc = __get__(propertyKey);
        /* step 2 */
        if (desc == null) {
            return null;
        }
        /* steps 3-9 (altered: returns live view) */
        return desc;
    }

    /**
     * 9.1.5.1 OrdinaryGetOwnProperty (O, P)
     */
    protected final Property ordinaryGetOwnProperty(Symbol propertyKey) {
        /* step 1 (implicit) */
        Property desc = __get__(propertyKey);
        /* step 2 */
        if (desc == null) {
            return null;
        }
        /* steps 3-9 (altered: returns live view) */
        return desc;
    }

    /** 9.1.6 [[DefineOwnProperty]] (P, Desc) */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, String propertyKey,
            PropertyDescriptor desc) {
        /* step 1 */
        return ordinaryDefineOwnProperty(propertyKey, desc);
    }

    /** 9.1.6 [[DefineOwnProperty]] (P, Desc) */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, Symbol propertyKey,
            PropertyDescriptor desc) {
        /* step 1 */
        return ordinaryDefineOwnProperty(propertyKey, desc);
    }

    /**
     * 9.1.6.1 OrdinaryDefineOwnProperty (O, P, Desc)
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
     * 9.1.6.1 OrdinaryDefineOwnProperty (O, P, Desc)
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
     * 9.1.6.2 IsCompatiblePropertyDescriptor (Extensible, Desc, Current)
     */
    protected static final boolean IsCompatiblePropertyDescriptor(boolean extensible,
            PropertyDescriptor desc, Property current) {
        /* step 1 */
        return __validateAndApplyPropertyDescriptor(null, null, extensible, desc, current);
    }

    /**
     * 9.1.6.3 ValidateAndApplyPropertyDescriptor (O, P, extensible, Desc, current)
     */
    protected static final boolean ValidateAndApplyPropertyDescriptor(OrdinaryObject object,
            String propertyKey, boolean extensible, PropertyDescriptor desc, Property current) {
        return __validateAndApplyPropertyDescriptor(object, propertyKey, extensible, desc, current);
    }

    /**
     * 9.1.6.3 ValidateAndApplyPropertyDescriptor (O, P, extensible, Desc, current)
     */
    protected static final boolean ValidateAndApplyPropertyDescriptor(OrdinaryObject object,
            Symbol propertyKey, boolean extensible, PropertyDescriptor desc, Property current) {
        return __validateAndApplyPropertyDescriptor(object, propertyKey, extensible, desc, current);
    }

    /**
     * 9.1.6.3 ValidateAndApplyPropertyDescriptor (O, P, extensible, Desc, current)
     */
    private static final boolean __validateAndApplyPropertyDescriptor(OrdinaryObject object,
            Object propertyKey, boolean extensible, PropertyDescriptor desc, Property current) {
        @SuppressWarnings("unused")
        String reason;
        reject: {
            /* step 1 */
            assert (object == null || propertyKey != null);
            /* step 2 */
            if (current == null) {
                if (!extensible) {
                    reason = "not extensible";
                    break reject;
                }
                if (desc.isGenericDescriptor() || desc.isDataDescriptor()) {
                    if (object != null) {
                        object.__put__(propertyKey, desc.toPlainProperty());
                    }
                } else {
                    assert desc.isAccessorDescriptor();
                    if (object != null) {
                        object.__put__(propertyKey, desc.toPlainProperty());
                    }
                }
                return true;
            }
            /* step 3 */
            if (desc.isEmpty()) {
                return true;
            }
            /* step 4 */
            if (current.isSubset(desc)) {
                return true;
            }
            /* step 5 */
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
                /* step 6 */
                // no further validation required, proceed below...
            } else if (desc.isDataDescriptor() != current.isDataDescriptor()) {
                /* step 7 */
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
                /* step 8 */
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
                /* step 9 */
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
            /* step 10 */
            if (object != null) {
                object.__get__(propertyKey).apply(desc);
            }
            /* step 11 */
            return true;
        }
        return false;
    }

    /**
     * 9.1.7 [[HasProperty]](P)
     */
    @Override
    public boolean hasProperty(ExecutionContext cx, String propertyKey) {
        /* step 1 (implicit) */
        /* steps 2-3 */
        boolean hasOwn = hasOwnProperty(propertyKey);
        /* step 4 */
        if (!hasOwn) {
            ScriptObject parent = getPrototypeOf(cx);
            if (parent != null) {
                return parent.hasProperty(cx, propertyKey);
            }
        }
        /* step 5 */
        return hasOwn;
    }

    /**
     * 9.1.7 [[HasProperty]](P)
     */
    @Override
    public boolean hasProperty(ExecutionContext cx, Symbol propertyKey) {
        /* step 1 (implicit) */
        /* steps 2-3 */
        boolean hasOwn = hasOwnProperty(propertyKey);
        /* step 4 */
        if (!hasOwn) {
            ScriptObject parent = getPrototypeOf(cx);
            if (parent != null) {
                return parent.hasProperty(cx, propertyKey);
            }
        }
        /* step 5 */
        return hasOwn;
    }

    /** 9.1.8 [[Get]] (P, Receiver) */
    @Override
    public Object get(ExecutionContext cx, String propertyKey, Object receiver) {
        /* step 1 (implicit) */
        /* steps 2-3 */
        Property desc = getOwnProperty(cx, propertyKey);
        /* step 4 */
        if (desc == null) {
            ScriptObject parent = getPrototypeOf(cx);
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

    /** 9.1.8 [[Get]] (P, Receiver) */
    @Override
    public Object get(ExecutionContext cx, Symbol propertyKey, Object receiver) {
        /* step 1 (implicit) */
        /* steps 2-3 */
        Property desc = getOwnProperty(cx, propertyKey);
        /* step 4 */
        if (desc == null) {
            ScriptObject parent = getPrototypeOf(cx);
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

    /** 9.1.9 [[Set] (P, V, Receiver) */
    @Override
    public boolean set(ExecutionContext cx, String propertyKey, Object value, Object receiver) {
        /* step 1 (implicit) */
        /* steps 2-3 */
        Property ownDesc = getOwnProperty(cx, propertyKey);
        /* step 4 */
        if (ownDesc == null) {
            ScriptObject parent = getPrototypeOf(cx);
            if (parent != null) {
                return parent.set(cx, propertyKey, value, receiver);
            } else {
                ownDesc = new Property(UNDEFINED, true, true, true);
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
                return CreateDataProperty(cx, _receiver, propertyKey, value);
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

    /** 9.1.9 [[Set] (P, V, Receiver) */
    @Override
    public boolean set(ExecutionContext cx, Symbol propertyKey, Object value, Object receiver) {
        /* step 1 (implicit) */
        /* steps 2-3 */
        Property ownDesc = getOwnProperty(cx, propertyKey);
        /* step 4 */
        if (ownDesc == null) {
            ScriptObject parent = getPrototypeOf(cx);
            if (parent != null) {
                return parent.set(cx, propertyKey, value, receiver);
            } else {
                ownDesc = new Property(UNDEFINED, true, true, true);
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
                return CreateDataProperty(cx, _receiver, propertyKey, value);
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

    /** 9.1.10 [[Delete]] (P) */
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

    /** 9.1.10 [[Delete]] (P) */
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

    /** 9.1.11 [[Enumerate]] () */
    @Override
    public final ScriptObject enumerate(ExecutionContext cx) {
        return CreateListIterator(cx, new EnumKeysIterator(cx, this));
    }

    /** 9.1.11 [[Enumerate]] () */
    protected List<String> enumerateKeys() {
        List<String> propList = new ArrayList<>();
        for (Object key : __keys__()) {
            if (key instanceof String) {
                propList.add((String) key);
            }
        }
        return propList;
    }

    /**
     * Subclasses need to override this method if they have virtual, enumerable properties
     */
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
                ScriptObject proto = this.obj.getPrototypeOf(cx);
                if (proto != null) {
                    if (proto instanceof OrdinaryObject) {
                        this.obj = ((OrdinaryObject) proto);
                        this.keys = ((OrdinaryObject) proto).enumerateKeys().iterator();
                        return tryNext();
                    } else {
                        this.obj = null;
                        this.keys = null;
                        this.protoKeys = FromScriptIterator(cx, proto.enumerate(cx));
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

    /** 9.1.12 [[OwnPropertyKeys]] ( ) */
    @Override
    public final ScriptObject ownPropertyKeys(ExecutionContext cx) {
        return CreateListIterator(cx, enumerateOwnKeys());
    }

    /** 9.1.12 [[OwnPropertyKeys]] ( ) */
    protected List<Object> enumerateOwnKeys() {
        return new ArrayList<>(__keys__());
    }

    private static final class DefaultAllocator implements ObjectAllocator<OrdinaryObject> {
        static final ObjectAllocator<OrdinaryObject> INSTANCE = new DefaultAllocator();

        @Override
        public OrdinaryObject newInstance(Realm realm) {
            return new OrdinaryObject(realm);
        }
    }

    /** 9.1.13 ObjectCreate Abstract Operation */
    public static final OrdinaryObject ObjectCreate(ExecutionContext cx) {
        return ObjectCreate(cx, Intrinsics.ObjectPrototype, DefaultAllocator.INSTANCE);
    }

    /** 9.1.13 ObjectCreate Abstract Operation */
    public static final OrdinaryObject ObjectCreate(ExecutionContext cx, ScriptObject proto) {
        return ObjectCreate(cx, proto, DefaultAllocator.INSTANCE);
    }

    /** 9.1.13 ObjectCreate Abstract Operation */
    public static final OrdinaryObject ObjectCreate(ExecutionContext cx, Intrinsics proto) {
        return ObjectCreate(cx, proto, DefaultAllocator.INSTANCE);
    }

    /** 9.1.13 ObjectCreate Abstract Operation */
    public static final <OBJECT extends OrdinaryObject> OBJECT ObjectCreate(ExecutionContext cx,
            ScriptObject proto, ObjectAllocator<OBJECT> allocator) {
        OBJECT obj = allocator.newInstance(cx.getRealm());
        obj.setPrototype(proto);
        return obj;
    }

    /** 9.1.13 ObjectCreate Abstract Operation */
    public static final <OBJECT extends OrdinaryObject> OBJECT ObjectCreate(ExecutionContext cx,
            Intrinsics proto, ObjectAllocator<OBJECT> allocator) {
        OBJECT obj = allocator.newInstance(cx.getRealm());
        obj.setPrototype(cx.getIntrinsic(proto));
        return obj;
    }

    /**
     * 9.1.14 OrdinaryCreateFromConstructor ( constructor, intrinsicDefaultProto )
     */
    public static final OrdinaryObject OrdinaryCreateFromConstructor(ExecutionContext cx,
            Object constructor, Intrinsics intrinsicDefaultProto) {
        /* step 1 (not applicable) */
        /* steps 2-3 */
        ScriptObject proto = GetPrototypeFromConstructor(cx, constructor, intrinsicDefaultProto);
        /* step 4 */
        return ObjectCreate(cx, proto);
    }

    /**
     * 9.1.14 OrdinaryCreateFromConstructor ( constructor, intrinsicDefaultProto, internalDataList )
     */
    public static final <OBJECT extends OrdinaryObject> OBJECT OrdinaryCreateFromConstructor(
            ExecutionContext cx, Object constructor, Intrinsics intrinsicDefaultProto,
            ObjectAllocator<OBJECT> allocator) {
        /* step 1 (not applicable) */
        /* steps 2-3 */
        ScriptObject proto = GetPrototypeFromConstructor(cx, constructor, intrinsicDefaultProto);
        /* step 4 */
        return ObjectCreate(cx, proto, allocator);
    }
}
