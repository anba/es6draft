/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.IndexedMap;
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
    private static final int STRING_PROPERTIES_DEFAULT_INITIAL_CAPACITY = 16;
    private static final int SYMBOL_PROPERTIES_DEFAULT_INITIAL_CAPACITY = 4;
    // Maps for String and Symbol valued property keys
    private final LinkedHashMap<String, Property> properties;
    private final LinkedHashMap<Symbol, Property> symbolProperties;
    // Map for indexed properties [0, 2^53 - 1]
    private final IndexedMap<Property> indexedProperties;

    /** [[Realm]] */
    @SuppressWarnings("unused")
    private final Realm realm;

    /** [[Prototype]] */
    private ScriptObject prototype = null;

    /** [[Extensible]] */
    private boolean extensible = true;

    /**
     * Constructs a new Ordinary Object instance.
     * 
     * @param realm
     *            the realm object
     */
    public OrdinaryObject(Realm realm) {
        this.realm = realm;
        this.properties = new LinkedHashMap<>(STRING_PROPERTIES_DEFAULT_INITIAL_CAPACITY);
        this.symbolProperties = new LinkedHashMap<>(SYMBOL_PROPERTIES_DEFAULT_INITIAL_CAPACITY);
        this.indexedProperties = new IndexedMap<>();
    }

    /**
     * Constructs a new Ordinary Object instance.
     * 
     * @param realm
     *            the realm object
     * @param empty
     *            unused placeholder
     */
    /* package */OrdinaryObject(Realm realm, Void empty) {
        this.realm = realm;
        this.properties = null;
        this.symbolProperties = null;
        this.indexedProperties = null;
    }

    @Override
    public String toString() {
        return String.format("%s@%x: indexed=%s, strings=%s, symbols=%s, extensible=%b", getClass()
                .getSimpleName(), System.identityHashCode(this), indexedProperties, properties
                .keySet(), symbolProperties.keySet(), extensible);
    }

    /**
     * Returns the string valued properties.
     * 
     * @return the string valued properties
     */
    final LinkedHashMap<String, Property> properties() {
        return properties;
    }

    /**
     * Returns the symbol valued properties.
     * 
     * @return the symbol valued properties
     */
    final LinkedHashMap<Symbol, Property> symbolProperties() {
        return symbolProperties;
    }

    /**
     * Returns the integer indexed properties.
     * 
     * @return the integer indexed properties
     */
    final IndexedMap<Property> indexedProperties() {
        return indexedProperties;
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
    protected boolean hasOwnProperty(ExecutionContext cx, long propertyKey) {
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
    protected final boolean ordinaryHasOwnProperty(long propertyKey) {
        // optimized: HasOwnProperty(cx, this, propertyKey)
        return indexedProperties.containsKey(propertyKey);
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
    public final Property getOwnProperty(ExecutionContext cx, long propertyKey) {
        if (IndexedMap.isIndex(propertyKey)) {
            return getProperty(cx, propertyKey);
        }
        return getProperty(cx, ToString(propertyKey));
    }

    /** 9.1.5 [[GetOwnProperty]] (P) */
    @Override
    public final Property getOwnProperty(ExecutionContext cx, String propertyKey) {
        long index = IndexedMap.toIndex(propertyKey);
        if (IndexedMap.isIndex(index)) {
            return getProperty(cx, index);
        }
        return getProperty(cx, propertyKey);
    }

    /** 9.1.5 [[GetOwnProperty]] (P) */
    @Override
    public final Property getOwnProperty(ExecutionContext cx, Symbol propertyKey) {
        /* step 1 */
        return getProperty(cx, propertyKey);
    }

    /**
     * 9.1.5 [[GetOwnProperty]] (P)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return the property or {@code null} if none found
     */
    protected Property getProperty(ExecutionContext cx, long propertyKey) {
        return ordinaryGetOwnProperty(propertyKey);
    }

    /**
     * 9.1.5 [[GetOwnProperty]] (P)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return the property or {@code null} if none found
     */
    protected Property getProperty(ExecutionContext cx, String propertyKey) {
        return ordinaryGetOwnProperty(propertyKey);
    }

    /**
     * 9.1.5 [[GetOwnProperty]] (P)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return the property or {@code null} if none found
     */
    protected Property getProperty(ExecutionContext cx, Symbol propertyKey) {
        return ordinaryGetOwnProperty(propertyKey);
    }

    /**
     * 9.1.5.1 OrdinaryGetOwnProperty (O, P)
     * 
     * @param propertyKey
     *            the property key
     * @return the property record or {@code null} if none found
     */
    protected final Property ordinaryGetOwnProperty(long propertyKey) {
        /* steps 1-9 (altered: returns live view) */
        return indexedProperties.get(propertyKey);
    }

    /**
     * 9.1.5.1 OrdinaryGetOwnProperty (O, P)
     * 
     * @param propertyKey
     *            the property key
     * @return the property record or {@code null} if none found
     */
    protected final Property ordinaryGetOwnProperty(String propertyKey) {
        /* steps 1-9 (altered: returns live view) */
        return properties.get(propertyKey);
    }

    /**
     * 9.1.5.1 OrdinaryGetOwnProperty (O, P)
     * 
     * @param propertyKey
     *            the property key
     * @return the property record or {@code null} if none found
     */
    protected final Property ordinaryGetOwnProperty(Symbol propertyKey) {
        /* steps 1-9 (altered: returns live view) */
        return symbolProperties.get(propertyKey);
    }

    /** 9.1.6 [[DefineOwnProperty]] (P, Desc) */
    @Override
    public final boolean defineOwnProperty(ExecutionContext cx, long propertyKey,
            PropertyDescriptor desc) {
        if (IndexedMap.isIndex(propertyKey)) {
            return defineProperty(cx, propertyKey, desc);
        }
        return defineProperty(cx, ToString(propertyKey), desc);
    }

    /** 9.1.6 [[DefineOwnProperty]] (P, Desc) */
    @Override
    public final boolean defineOwnProperty(ExecutionContext cx, String propertyKey,
            PropertyDescriptor desc) {
        long index = IndexedMap.toIndex(propertyKey);
        if (IndexedMap.isIndex(index)) {
            return defineProperty(cx, index, desc);
        }
        return defineProperty(cx, propertyKey, desc);
    }

    /** 9.1.6 [[DefineOwnProperty]] (P, Desc) */
    @Override
    public final boolean defineOwnProperty(ExecutionContext cx, Symbol propertyKey,
            PropertyDescriptor desc) {
        return defineProperty(cx, propertyKey, desc);
    }

    /**
     * 9.1.6 [[DefineOwnProperty]] (P, Desc)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @param desc
     *            the property descriptor
     * @return {@code true} if the property was successfully defined
     */
    protected boolean defineProperty(ExecutionContext cx, long propertyKey, PropertyDescriptor desc) {
        return ordinaryDefineOwnProperty(cx, propertyKey, desc);
    }

    /**
     * 9.1.6 [[DefineOwnProperty]] (P, Desc)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @param desc
     *            the property descriptor
     * @return {@code true} if the property was successfully defined
     */
    protected boolean defineProperty(ExecutionContext cx, String propertyKey,
            PropertyDescriptor desc) {
        return ordinaryDefineOwnProperty(cx, propertyKey, desc);
    }

    /**
     * 9.1.6 [[DefineOwnProperty]] (P, Desc)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @param desc
     *            the property descriptor
     * @return {@code true} if the property was successfully defined
     */
    protected boolean defineProperty(ExecutionContext cx, Symbol propertyKey,
            PropertyDescriptor desc) {
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
    protected final boolean ordinaryDefineOwnProperty(ExecutionContext cx, long propertyKey,
            PropertyDescriptor desc) {
        /* steps 1-2 */
        Property current = getProperty(cx, propertyKey);
        /* step 3 */
        boolean extensible = isExtensible();
        /* step 4 */
        return validateAndApplyPropertyDescriptor(indexedProperties, propertyKey, extensible, desc,
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
    protected final boolean ordinaryDefineOwnProperty(ExecutionContext cx, String propertyKey,
            PropertyDescriptor desc) {
        /* steps 1-2 */
        Property current = getProperty(cx, propertyKey);
        /* step 3 */
        boolean extensible = isExtensible();
        /* step 4 */
        return validateAndApplyPropertyDescriptor(properties, propertyKey, extensible, desc,
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
        /* steps 1-2 */
        Property current = getProperty(cx, propertyKey);
        /* step 3 */
        boolean extensible = isExtensible();
        /* step 4 */
        return validateAndApplyPropertyDescriptor(symbolProperties, propertyKey, extensible, desc,
                current);
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
        return validateAndApplyPropertyDescriptor(null, null, extensible, desc, current);
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
            long propertyKey, boolean extensible, PropertyDescriptor desc, Property current) {
        return validateAndApplyPropertyDescriptor(object.indexedProperties, propertyKey,
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
    protected static final boolean ValidateAndApplyPropertyDescriptor(OrdinaryObject object,
            String propertyKey, boolean extensible, PropertyDescriptor desc, Property current) {
        return validateAndApplyPropertyDescriptor(object.properties, propertyKey, extensible, desc,
                current);
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
        return validateAndApplyPropertyDescriptor(object.symbolProperties, propertyKey, extensible,
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
    private static final <KEY> boolean validateAndApplyPropertyDescriptor(
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
                    object.put(propertyKey, desc.toProperty());
                }
            } else {
                assert desc.isAccessorDescriptor();
                if (object != null) {
                    object.put(propertyKey, desc.toProperty());
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
    private static final boolean validateAndApplyPropertyDescriptor(IndexedMap<Property> object,
            long propertyKey, boolean extensible, PropertyDescriptor desc, Property current) {
        /* step 1 */
        assert (object == null || IndexedMap.isIndex(propertyKey));
        /* step 2 */
        if (current == null) {
            if (!extensible) {
                return false;
            }
            if (desc.isGenericDescriptor() || desc.isDataDescriptor()) {
                if (object != null) {
                    object.put(propertyKey, desc.toProperty());
                }
            } else {
                assert desc.isAccessorDescriptor();
                if (object != null) {
                    object.put(propertyKey, desc.toProperty());
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
    public final boolean hasProperty(ExecutionContext cx, long propertyKey) {
        if (IndexedMap.isIndex(propertyKey)) {
            return hasProp(cx, propertyKey);
        }
        return hasProp(cx, ToString(propertyKey));
    }

    /**
     * 9.1.7 [[HasProperty]](P)
     */
    @Override
    public final boolean hasProperty(ExecutionContext cx, String propertyKey) {
        long index = IndexedMap.toIndex(propertyKey);
        if (IndexedMap.isIndex(index)) {
            return hasProp(cx, index);
        }
        return hasProp(cx, propertyKey);
    }

    /**
     * 9.1.7 [[HasProperty]](P)
     */
    @Override
    public final boolean hasProperty(ExecutionContext cx, Symbol propertyKey) {
        return hasProp(cx, propertyKey);
    }

    /**
     * 9.1.7 [[HasProperty]](P)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return {@code true} if the property was found
     */
    protected boolean hasProp(ExecutionContext cx, long propertyKey) {
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
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return {@code true} if the property was found
     */
    protected boolean hasProp(ExecutionContext cx, String propertyKey) {
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
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return {@code true} if the property was found
     */
    protected boolean hasProp(ExecutionContext cx, Symbol propertyKey) {
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
    public final Object get(ExecutionContext cx, long propertyKey, Object receiver) {
        if (IndexedMap.isIndex(propertyKey)) {
            return getValue(cx, propertyKey, receiver);
        }
        return getValue(cx, ToString(propertyKey), receiver);
    }

    /** 9.1.8 [[Get]] (P, Receiver) */
    @Override
    public final Object get(ExecutionContext cx, String propertyKey, Object receiver) {
        long index = IndexedMap.toIndex(propertyKey);
        if (IndexedMap.isIndex(index)) {
            return getValue(cx, index, receiver);
        }
        return getValue(cx, propertyKey, receiver);
    }

    /** 9.1.8 [[Get]] (P, Receiver) */
    @Override
    public final Object get(ExecutionContext cx, Symbol propertyKey, Object receiver) {
        return getValue(cx, propertyKey, receiver);
    }

    /**
     * 9.1.8 [[Get]] (P, Receiver)
     *
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @param receiver
     *            the receiver object
     * @return the property value
     */
    protected Object getValue(ExecutionContext cx, long propertyKey, Object receiver) {
        /* step 1 (implicit) */
        /* steps 2-3 */
        Property desc = getProperty(cx, propertyKey);
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

    /**
     * 9.1.8 [[Get]] (P, Receiver)
     *
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @param receiver
     *            the receiver object
     * @return the property value
     */
    protected Object getValue(ExecutionContext cx, String propertyKey, Object receiver) {
        /* step 1 (implicit) */
        /* steps 2-3 */
        Property desc = getProperty(cx, propertyKey);
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

    /**
     * 9.1.8 [[Get]] (P, Receiver)
     *
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @param receiver
     *            the receiver object
     * @return the property value
     */
    protected Object getValue(ExecutionContext cx, Symbol propertyKey, Object receiver) {
        /* step 1 (implicit) */
        /* steps 2-3 */
        Property desc = getProperty(cx, propertyKey);
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
    public final boolean set(ExecutionContext cx, long propertyKey, Object value, Object receiver) {
        if (IndexedMap.isIndex(propertyKey)) {
            return setValue(cx, propertyKey, value, receiver);
        }
        return setValue(cx, ToString(propertyKey), value, receiver);
    }

    /** 9.1.9 [[Set] (P, V, Receiver) */
    @Override
    public final boolean set(ExecutionContext cx, String propertyKey, Object value, Object receiver) {
        long index = IndexedMap.toIndex(propertyKey);
        if (IndexedMap.isIndex(index)) {
            return setValue(cx, index, value, receiver);
        }
        return setValue(cx, propertyKey, value, receiver);
    }

    /** 9.1.9 [[Set] (P, V, Receiver) */
    @Override
    public final boolean set(ExecutionContext cx, Symbol propertyKey, Object value, Object receiver) {
        return setValue(cx, propertyKey, value, receiver);
    }

    /**
     * 9.1.9 [[Set] (P, V, Receiver)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @param value
     *            the new property value
     * @param receiver
     *            the receiver object
     * @return {@code true} on success
     */
    protected boolean setValue(ExecutionContext cx, long propertyKey, Object value, Object receiver) {
        /* step 1 (implicit) */
        /* steps 2-3 */
        Property ownDesc = getProperty(cx, propertyKey);
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

    /**
     * 9.1.9 [[Set] (P, V, Receiver)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @param value
     *            the new property value
     * @param receiver
     *            the receiver object
     * @return {@code true} on success
     */
    protected boolean setValue(ExecutionContext cx, String propertyKey, Object value,
            Object receiver) {
        /* step 1 (implicit) */
        /* steps 2-3 */
        Property ownDesc = getProperty(cx, propertyKey);
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

    /**
     * 9.1.9 [[Set] (P, V, Receiver)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @param value
     *            the new property value
     * @param receiver
     *            the receiver object
     * @return {@code true} on success
     */
    protected boolean setValue(ExecutionContext cx, Symbol propertyKey, Object value,
            Object receiver) {
        /* step 1 (implicit) */
        /* steps 2-3 */
        Property ownDesc = getProperty(cx, propertyKey);
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
    public final boolean delete(ExecutionContext cx, long propertyKey) {
        if (IndexedMap.isIndex(propertyKey)) {
            return deleteProperty(cx, propertyKey);
        }
        return deleteProperty(cx, ToString(propertyKey));
    }

    /** 9.1.10 [[Delete]] (P) */
    @Override
    public final boolean delete(ExecutionContext cx, String propertyKey) {
        long index = IndexedMap.toIndex(propertyKey);
        if (IndexedMap.isIndex(index)) {
            return deleteProperty(cx, index);
        }
        return deleteProperty(cx, propertyKey);
    }

    /** 9.1.10 [[Delete]] (P) */
    @Override
    public final boolean delete(ExecutionContext cx, Symbol propertyKey) {
        return deleteProperty(cx, propertyKey);
    }

    /**
     * [[Delete]] (P)
     *
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return {@code true} if the property was successfully deleted
     */
    protected boolean deleteProperty(ExecutionContext cx, long propertyKey) {
        /* step 1 (not applicable) */
        /* steps 2-3 */
        Property desc = getProperty(cx, propertyKey);
        /* step 4 */
        if (desc == null) {
            return true;
        }
        /* step 5 */
        if (desc.isConfigurable()) {
            indexedProperties.remove(propertyKey);
            return true;
        }
        /* step 6 */
        return false;
    }

    /**
     * [[Delete]] (P)
     *
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return {@code true} if the property was successfully deleted
     */
    protected boolean deleteProperty(ExecutionContext cx, String propertyKey) {
        /* step 1 (not applicable) */
        /* steps 2-3 */
        Property desc = getProperty(cx, propertyKey);
        /* step 4 */
        if (desc == null) {
            return true;
        }
        /* step 5 */
        if (desc.isConfigurable()) {
            properties.remove(propertyKey);
            return true;
        }
        /* step 6 */
        return false;
    }

    /**
     * [[Delete]] (P)
     *
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return {@code true} if the property was successfully deleted
     */
    protected boolean deleteProperty(ExecutionContext cx, Symbol propertyKey) {
        /* step 1 (not applicable) */
        /* steps 2-3 */
        Property desc = getProperty(cx, propertyKey);
        /* step 4 */
        if (desc == null) {
            return true;
        }
        /* step 5 */
        if (desc.isConfigurable()) {
            symbolProperties.remove(propertyKey);
            return true;
        }
        /* step 6 */
        return false;
    }

    /** 9.1.11 [[Enumerate]] () */
    @Override
    public final ScriptObject enumerate(ExecutionContext cx) {
        return CreateListIterator(cx, new EnumKeysIterator(cx, this));
    }

    /** 9.1.11 [[Enumerate]] () */
    @Override
    public final Iterator<?> enumerateKeys(ExecutionContext cx) {
        return new EnumKeysIterator(cx, this);
    }

    /**
     * 9.1.11 [[Enumerate]] ()
     *
     * @param cx
     *            the execution context
     * @return the list of enumerable string valued property keys
     */
    protected List<String> getEnumerableKeys(ExecutionContext cx) {
        ArrayList<String> keys = new ArrayList<>();
        if (!indexedProperties.isEmpty()) {
            keys.addAll(indexedProperties.keys());
        }
        if (!properties.isEmpty()) {
            keys.addAll(properties.keySet());
        }
        return keys;
    }

    /**
     * Note: Subclasses need to override this method if they have virtual, enumerable properties.
     * 
     * @param propertyKey
     *            the property key
     * @return {@code true} if the property is enumerable
     */
    protected boolean isEnumerableOwnProperty(String propertyKey) {
        Property prop;
        long index = IndexedMap.toIndex(propertyKey);
        if (IndexedMap.isIndex(index)) {
            prop = ordinaryGetOwnProperty(index);
        } else {
            prop = ordinaryGetOwnProperty(propertyKey);
        }
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
                        this.protoKeys = proto.enumerateKeys(cx);
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
    public final ExoticArray ownPropertyKeys(ExecutionContext cx) {
        return CreateArrayFromList(cx, getOwnPropertyKeys(cx));
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
        /* step 1 */
        ArrayList<Object> ownKeys = new ArrayList<>();
        /* step 2 */
        if (!indexedProperties.isEmpty()) {
            ownKeys.addAll(indexedProperties.keys());
        }
        /* step 3 */
        if (!properties.isEmpty()) {
            ownKeys.addAll(properties.keySet());
        }
        /* step 4 */
        if (!symbolProperties.isEmpty()) {
            ownKeys.addAll(symbolProperties.keySet());
        }
        /* step 5 */
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
