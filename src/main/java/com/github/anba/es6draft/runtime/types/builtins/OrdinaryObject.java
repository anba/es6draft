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
    private final LinkedHashMap<String, Property> properties;
    private final LinkedHashMap<Symbol, Property> symbolProperties;

    /** [[Realm]] */
    @SuppressWarnings("unused")
    private final Realm realm;

    /** [[Prototype]] */
    private ScriptObject prototype = null;

    /** [[Extensible]] */
    private boolean extensible = true;

    public OrdinaryObject(Realm realm) {
        this.realm = realm;
        this.properties = new LinkedHashMap<>();
        this.symbolProperties = new LinkedHashMap<>(4);
    }

    /* package */OrdinaryObject(Realm realm, Void empty) {
        this.realm = realm;
        this.properties = null;
        this.symbolProperties = null;
    }

    /**
     * Internal hook for ExoticArray.
     * 
     * @param propertyKey
     *            the property key
     * @param property
     *            the property record
     */
    final void addProperty(int propertyKey, Property property) {
        properties.put(Integer.toString(propertyKey), property);
    }

    /**
     * [[Prototype]]
     * 
     * @return the prototype object
     */
    public final ScriptObject getPrototype() {
        return prototype;
    }

    /**
     * [[Prototype]]
     * 
     * @param prototype
     *            the new prototype object
     */
    public final void setPrototype(ScriptObject prototype) {
        this.prototype = prototype;
    }

    /**
     * [[Extensible]]
     * 
     * @return {@code true} if this object is extensible
     */
    protected final boolean isExtensible() {
        return extensible;
    }

    /**
     * [[Extensible]]
     * 
     * @param extensible
     *            the new extensible mode
     */
    protected final void setExtensible(boolean extensible) {
        assert this.extensible || !extensible;
        this.extensible = extensible;
    }

    /**
     * [[HasOwnProperty]] (P)
     *
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return {@code true} if an own property was found
     */
    protected boolean hasOwnProperty(ExecutionContext cx, String propertyKey) {
        return ordinaryHasOwnProperty(propertyKey);
    }

    /**
     * [[HasOwnProperty]] (P)
     *
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return {@code true} if an own property was found
     */
    protected boolean hasOwnProperty(ExecutionContext cx, Symbol propertyKey) {
        return ordinaryHasOwnProperty(propertyKey);
    }

    /**
     * OrdinaryHasOwnProperty (P) (not in spec)
     * 
     * @param propertyKey
     *            the property key
     * @return {@code true} if an own property was found
     */
    protected final boolean ordinaryHasOwnProperty(String propertyKey) {
        // optimized: HasOwnProperty(cx, this, propertyKey)
        return properties.containsKey(propertyKey);
    }

    /**
     * OrdinaryHasOwnProperty (P) (not in spec)
     * 
     * @param propertyKey
     *            the property key
     * @return {@code true} if an own property was found
     */
    protected final boolean ordinaryHasOwnProperty(Symbol propertyKey) {
        // optimized: HasOwnProperty(cx, this, propertyKey)
        return symbolProperties.containsKey(propertyKey);
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
        extensible = this.extensible;
        /* step 8 */
        if (!extensible) {
            ScriptObject current2 = this.prototype;
            if (prototype == current2) {
                return true;
            }
            return false;
        }
        /* step 9 */
        this.prototype = prototype;
        /* step 10 */
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
     * 
     * @param propertyKey
     *            the property key
     * @return the property record or {@code null} if none found
     */
    protected final Property ordinaryGetOwnProperty(String propertyKey) {
        /* step 1 (implicit) */
        Property desc = properties.get(propertyKey);
        /* step 2 */
        if (desc == null) {
            return null;
        }
        /* steps 3-9 (altered: returns live view) */
        return desc;
    }

    /**
     * 9.1.5.1 OrdinaryGetOwnProperty (O, P)
     * 
     * @param propertyKey
     *            the property key
     * @return the property record or {@code null} if none found
     */
    protected final Property ordinaryGetOwnProperty(Symbol propertyKey) {
        /* step 1 (implicit) */
        Property desc = symbolProperties.get(propertyKey);
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
        return ordinaryDefineOwnProperty(cx, propertyKey, desc);
    }

    /** 9.1.6 [[DefineOwnProperty]] (P, Desc) */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, Symbol propertyKey,
            PropertyDescriptor desc) {
        /* step 1 */
        return ordinaryDefineOwnProperty(cx, propertyKey, desc);
    }

    /**
     * 9.1.6.1 OrdinaryDefineOwnProperty (O, P, Desc)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @param desc
     *            the property descriptor
     * @return {@code true} on success
     */
    protected final boolean ordinaryDefineOwnProperty(ExecutionContext cx, String propertyKey,
            PropertyDescriptor desc) {
        /* step 1 */
        Property current = getOwnProperty(cx, propertyKey);
        /* step 2 */
        boolean extensible = isExtensible();
        /* step 3 */
        return __validateAndApplyPropertyDescriptor(this.properties, propertyKey, extensible, desc,
                current);
    }

    /**
     * 9.1.6.1 OrdinaryDefineOwnProperty (O, P, Desc)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @param desc
     *            the property descriptor
     * @return {@code true} on success
     */
    protected final boolean ordinaryDefineOwnProperty(ExecutionContext cx, Symbol propertyKey,
            PropertyDescriptor desc) {
        /* step 1 */
        Property current = getOwnProperty(cx, propertyKey);
        /* step 2 */
        boolean extensible = isExtensible();
        /* step 3 */
        return __validateAndApplyPropertyDescriptor(this.symbolProperties, propertyKey, extensible,
                desc, current);
    }

    /**
     * 9.1.6.2 IsCompatiblePropertyDescriptor (Extensible, Desc, Current)
     * 
     * @param extensible
     *            the extensible mode
     * @param desc
     *            the property descriptor
     * @param current
     *            the current property
     * @return {@code true} if <var>desc</var> is compatible
     */
    protected static final boolean IsCompatiblePropertyDescriptor(boolean extensible,
            PropertyDescriptor desc, Property current) {
        /* step 1 */
        return __validateAndApplyPropertyDescriptor(null, null, extensible, desc, current);
    }

    /**
     * 9.1.6.3 ValidateAndApplyPropertyDescriptor (O, P, extensible, Desc, current)
     * 
     * @param object
     *            the script object
     * @param propertyKey
     *            the property key
     * @param extensible
     *            the extensible mode
     * @param desc
     *            the property descriptor
     * @param current
     *            the current property
     * @return {@code true} on success
     */
    protected static final boolean ValidateAndApplyPropertyDescriptor(OrdinaryObject object,
            String propertyKey, boolean extensible, PropertyDescriptor desc, Property current) {
        return __validateAndApplyPropertyDescriptor(object.properties, propertyKey, extensible,
                desc, current);
    }

    /**
     * 9.1.6.3 ValidateAndApplyPropertyDescriptor (O, P, extensible, Desc, current)
     * 
     * @param object
     *            the script object
     * @param propertyKey
     *            the property key
     * @param extensible
     *            the extensible mode
     * @param desc
     *            the property descriptor
     * @param current
     *            the current property
     * @return {@code true} on success
     */
    protected static final boolean ValidateAndApplyPropertyDescriptor(OrdinaryObject object,
            Symbol propertyKey, boolean extensible, PropertyDescriptor desc, Property current) {
        return __validateAndApplyPropertyDescriptor(object.symbolProperties, propertyKey,
                extensible, desc, current);
    }

    /**
     * 9.1.6.3 ValidateAndApplyPropertyDescriptor (O, P, extensible, Desc, current)
     * 
     * @param object
     *            the script object
     * @param propertyKey
     *            the property key
     * @param extensible
     *            the extensible mode
     * @param desc
     *            the property descriptor
     * @param current
     *            the current property
     * @return {@code true} on success
     */
    private static final <KEY> boolean __validateAndApplyPropertyDescriptor(
            LinkedHashMap<KEY, Property> object, KEY propertyKey, boolean extensible,
            PropertyDescriptor desc, Property current) {
        /* step 1 */
        assert (object == null || propertyKey != null);
        /* step 2 */
        if (current == null) {
            if (!extensible) {
                return false;
            }
            if (desc.isGenericDescriptor() || desc.isDataDescriptor()) {
                if (object != null) {
                    object.put(propertyKey, desc.toPlainProperty());
                }
            } else {
                assert desc.isAccessorDescriptor();
                if (object != null) {
                    object.put(propertyKey, desc.toPlainProperty());
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
                return false;
            }
            if (desc.hasEnumerable() && desc.isEnumerable() != current.isEnumerable()) {
                return false;
            }
        }
        if (desc.isGenericDescriptor()) {
            /* step 6 */
            // no further validation required, proceed below...
        } else if (desc.isDataDescriptor() != current.isDataDescriptor()) {
            /* step 7 */
            if (!current.isConfigurable()) {
                return false;
            }
            if (current.isDataDescriptor()) {
                if (object != null) {
                    object.get(propertyKey).toAccessorProperty();
                }
            } else {
                if (object != null) {
                    object.get(propertyKey).toDataProperty();
                }
            }
        } else if (desc.isDataDescriptor() && current.isDataDescriptor()) {
            /* step 8 */
            if (!current.isConfigurable()) {
                if (!current.isWritable() && desc.isWritable()) {
                    return false;
                }
                if (!current.isWritable()) {
                    if (desc.hasValue() && !SameValue(desc.getValue(), current.getValue())) {
                        return false;
                    }
                }
            }
        } else {
            /* step 9 */
            assert desc.isAccessorDescriptor() && current.isAccessorDescriptor();
            if (!current.isConfigurable()) {
                if (desc.hasSetter() && !SameValue(desc.getSetter(), current.getSetter())) {
                    return false;
                }
                if (desc.hasGetter() && !SameValue(desc.getGetter(), current.getGetter())) {
                    return false;
                }
            }
        }
        /* step 10 */
        if (object != null) {
            object.get(propertyKey).apply(desc);
        }
        /* step 11 */
        return true;
    }

    /**
     * 9.1.7 [[HasProperty]](P)
     */
    @Override
    public boolean hasProperty(ExecutionContext cx, String propertyKey) {
        /* step 1 (implicit) */
        /* steps 2-3 */
        boolean hasOwn = hasOwnProperty(cx, propertyKey);
        /* step 4 */
        if (hasOwn) {
            return true;
        }
        /* steps 5-6 */
        ScriptObject parent = getPrototypeOf(cx);
        /* step 7 */
        if (parent != null) {
            return parent.hasProperty(cx, propertyKey);
        }
        /* step 8 */
        return false;
    }

    /**
     * 9.1.7 [[HasProperty]](P)
     */
    @Override
    public boolean hasProperty(ExecutionContext cx, Symbol propertyKey) {
        /* step 1 (implicit) */
        /* steps 2-3 */
        boolean hasOwn = hasOwnProperty(cx, propertyKey);
        /* step 4 */
        if (hasOwn) {
            return true;
        }
        /* steps 5-6 */
        ScriptObject parent = getPrototypeOf(cx);
        /* step 7 */
        if (parent != null) {
            return parent.hasProperty(cx, propertyKey);
        }
        /* step 8 */
        return false;
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
            properties.remove(propertyKey);
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
            symbolProperties.remove(propertyKey);
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

    /**
     * 9.1.11 [[Enumerate]] ()
     *
     * @param cx
     *            the execution context
     * @return the list of enumerable string valued property keys
     */
    protected List<String> getEnumerableKeys(ExecutionContext cx) {
        return new ArrayList<>(properties.keySet());
    }

    /**
     * Note: Subclasses need to override this method if they have virtual, enumerable properties.
     * 
     * @param propertyKey
     *            the property key
     * @return {@code true} if the property is enumerable
     */
    protected boolean isEnumerableOwnProperty(String propertyKey) {
        Property prop = ordinaryGetOwnProperty(propertyKey);
        return prop != null && prop.isEnumerable();
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
            this.keys = obj.getEnumerableKeys(cx).iterator();
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
                ScriptObject proto = obj.getPrototypeOf(cx);
                if (proto != null) {
                    if (proto instanceof OrdinaryObject) {
                        this.obj = ((OrdinaryObject) proto);
                        this.keys = ((OrdinaryObject) proto).getEnumerableKeys(cx).iterator();
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
        // FIXME: array
        return CreateListIterator(cx, getOwnPropertyKeys(cx));
    }

    /** 9.1.12 [[OwnPropertyKeys]] ( ) */
    @Override
    public final Iterator<?> ownKeys(ExecutionContext cx) {
        return getOwnPropertyKeys(cx).iterator();
    }

    /**
     * 9.1.12 [[OwnPropertyKeys]] ( )
     * 
     * @param cx
     *            the execution context
     * @return the list of own property keys
     */
    protected List<Object> getOwnPropertyKeys(ExecutionContext cx) {
        // TODO: sort indexed property keys
        ArrayList<Object> ownKeys = new ArrayList<>(properties.keySet());
        if (!symbolProperties.isEmpty()) {
            ownKeys.addAll(symbolProperties.keySet());
        }
        return ownKeys;
    }

    private static final class DefaultAllocator implements ObjectAllocator<OrdinaryObject> {
        static final ObjectAllocator<OrdinaryObject> INSTANCE = new DefaultAllocator();

        @Override
        public OrdinaryObject newInstance(Realm realm) {
            return new OrdinaryObject(realm);
        }
    }

    /**
     * 9.1.13 ObjectCreate(proto, internalSlotsList) Abstract Operation
     *
     * @param cx
     *            the execution context
     * @return the new object
     */
    public static final OrdinaryObject ObjectCreate(ExecutionContext cx) {
        return ObjectCreate(cx, Intrinsics.ObjectPrototype, DefaultAllocator.INSTANCE);
    }

    /**
     * 9.1.13 ObjectCreate(proto, internalSlotsList) Abstract Operation
     *
     * @param cx
     *            the execution context
     * @param proto
     *            the prototype object
     * @return the new object
     */
    public static final OrdinaryObject ObjectCreate(ExecutionContext cx, ScriptObject proto) {
        return ObjectCreate(cx, proto, DefaultAllocator.INSTANCE);
    }

    /**
     * 9.1.13 ObjectCreate(proto, internalSlotsList) Abstract Operation
     *
     * @param cx
     *            the execution context
     * @param proto
     *            the prototype object
     * @return the new object
     */
    public static final OrdinaryObject ObjectCreate(ExecutionContext cx, Intrinsics proto) {
        return ObjectCreate(cx, proto, DefaultAllocator.INSTANCE);
    }

    /**
     * 9.1.13 ObjectCreate(proto, internalSlotsList) Abstract Operation
     *
     * @param <OBJECT>
     *            the object type
     * @param cx
     *            the execution context
     * @param proto
     *            the prototype object
     * @param allocator
     *            the object allocator
     * @return the new object
     */
    public static final <OBJECT extends OrdinaryObject> OBJECT ObjectCreate(ExecutionContext cx,
            ScriptObject proto, ObjectAllocator<OBJECT> allocator) {
        OBJECT obj = allocator.newInstance(cx.getRealm());
        obj.setPrototype(proto);
        return obj;
    }

    /**
     * 9.1.13 ObjectCreate(proto, internalSlotsList) Abstract Operation
     *
     * @param <OBJECT>
     *            the object type
     * @param cx
     *            the execution context
     * @param proto
     *            the prototype object
     * @param allocator
     *            the object allocator
     * @return the new object
     */
    public static final <OBJECT extends OrdinaryObject> OBJECT ObjectCreate(ExecutionContext cx,
            Intrinsics proto, ObjectAllocator<OBJECT> allocator) {
        OBJECT obj = allocator.newInstance(cx.getRealm());
        obj.setPrototype(cx.getIntrinsic(proto));
        return obj;
    }

    /**
     * 9.1.14 OrdinaryCreateFromConstructor (constructor, intrinsicDefaultProto, internalSlotsList)
     * 
     * @param cx
     *            the execution context
     * @param constructor
     *            the constructor function
     * @param intrinsicDefaultProto
     *            the default prototype
     * @return the new object
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
     * 9.1.14 OrdinaryCreateFromConstructor (constructor, intrinsicDefaultProto, internalSlotsList)
     * 
     * @param <OBJECT>
     *            the object type
     * @param cx
     *            the execution context
     * @param constructor
     *            the constructor function
     * @param intrinsicDefaultProto
     *            the default prototype
     * @param allocator
     *            the object allocator
     * @return the new object
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
