/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.IndexedMap;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.PropertyMap;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PrivateName;
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
    private static final int PRIVATE_NAMES_DEFAULT_INITIAL_CAPACITY = 4;
    private static final Object[] EMPTY_ARRAY = new Object[0];

    // Map for String valued property keys
    private final PropertyMap<String, Property> properties;
    // Map for Symbol valued property keys
    private final PropertyMap<Symbol, Property> symbolProperties;
    // Map for indexed properties [0, 2^53 - 1]
    private final IndexedMap<Property> indexedProperties;
    // Map for private names
    private final HashMap<PrivateName, Property> privateNames;

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
        this.properties = new PropertyMap<>(STRING_PROPERTIES_DEFAULT_INITIAL_CAPACITY);
        this.symbolProperties = new PropertyMap<>(SYMBOL_PROPERTIES_DEFAULT_INITIAL_CAPACITY);
        this.indexedProperties = new IndexedMap<>();
        this.privateNames = new HashMap<>(PRIVATE_NAMES_DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Constructs a new Ordinary Object instance.
     * 
     * @param realm
     *            the realm object
     * @param prototype
     *            the prototype object
     */
    protected OrdinaryObject(Realm realm, ScriptObject prototype) {
        this(realm);
        this.prototype = prototype;
    }

    @Override
    public String toString() {
        return String.format("%s@%x: indexed=%s, strings=%s, symbols=%s, private=%s, extensible=%b",
                getClass().getSimpleName(), System.identityHashCode(this), indexedProperties, properties.keySet(),
                symbolProperties.keySet(), privateNames.keySet(), extensible);
    }

    final void defineOwnPropertiesUncheckedAtFront(Consumer<BiConsumer<String, Property>> newProperties) {
        if (properties.isEmpty()) {
            // Insert new properties into properties table if table is currently empty.
            newProperties.accept(properties::put);
        } else {
            // Otherwise remove all entries from table, insert new properties and then append old properties, that way
            // the property insertion order is preserved.
            PropertyMap<String, Property> oldProperties = properties.clone();
            properties.clear();
            newProperties.accept(properties::put);
            properties.putAll(oldProperties);
        }
    }

    public final void infallibleDefineOwnProperty(String propertyKey, Property property) {
        assert extensible : "object not extensible";
        assert !properties.containsKey(propertyKey) : "illegal property = " + propertyKey;
        assert !IndexedMap.isIndex(propertyKey);
        properties.put(propertyKey, property);
    }

    public final void infallibleDefineOwnProperty(Symbol propertyKey, Property property) {
        assert extensible : "object not extensible";
        assert !symbolProperties.containsKey(propertyKey) : "illegal property = " + propertyKey;
        symbolProperties.put(propertyKey, property);
    }

    public final Property lookupOwnProperty(String propertyKey) {
        assert !IndexedMap.isIndex(propertyKey);
        return properties.get(propertyKey);
    }

    public final Property lookupOwnProperty(Symbol propertyKey) {
        return symbolProperties.get(propertyKey);
    }

    public final void infallibleSetPrototype(ScriptObject prototype) {
        this.prototype = prototype;
    }

    /**
     * Returns the indexed properties length.
     * 
     * @return the indexed properties length
     */
    long getIndexedLength() {
        return indexedProperties.getLength();
    }

    /**
     * Returns the own property value from the given index.
     * 
     * @param propertyKey
     *            the indexed property key
     * @return the property value
     */
    Object getIndexed(long propertyKey) {
        return indexedProperties.get(propertyKey).getValue();
    }

    /**
     * Set the own property value at the given index to the new value.
     * 
     * @param propertyKey
     *            the indexed property key
     * @param value
     *            the property value
     */
    final void setIndexed(long propertyKey, Object value) {
        indexedProperties.put(propertyKey, new Property(value, true, true, true));
    }

    /**
     * Deletes all indexed properties within the range {@code [startIndex, endIndex)} in reverse order, i.e. starting
     * from index {@code endIndex - 1}. The range must not be empty.
     * 
     * @param startIndex
     *            the start index (inclusive)
     * @param endIndex
     *            the end index (exclusive)
     * @return {@code -1} if all indexed properties over the requested range have been removed successfully; otherwise
     *         the index of the first property which is not deletable.
     */
    final long deleteRange(long startIndex, long endIndex) {
        assert startIndex < endIndex;
        IndexedMap<Property> indexed = indexedProperties;
        if (indexed.isEmpty()) {
            return -1;
        }
        long lastIndex;
        if (indexed.isSparse()) {
            lastIndex = deleteRangeSparse(startIndex, endIndex);
        } else {
            lastIndex = deleteRangeDense(startIndex, endIndex);
        }
        // Need to call updateLength() manually.
        indexed.updateLength();
        return lastIndex;
    }

    private long deleteRangeDense(long startIndex, long endIndex) {
        IndexedMap<Property> indexed = indexedProperties;
        for (long index = endIndex; startIndex < index;) {
            Property prop = indexed.get(--index);
            if (prop != null && !prop.isConfigurable()) {
                return index;
            }
            indexed.removeUnchecked(index);
        }
        return -1;
    }

    private long deleteRangeSparse(long startIndex, long endIndex) {
        Iterator<Map.Entry<Long, Property>> iter = indexedProperties.descendingIterator(startIndex, endIndex);
        while (iter.hasNext()) {
            Map.Entry<Long, Property> entry = iter.next();
            if (!entry.getValue().isConfigurable()) {
                return entry.getKey();
            }
            // Cannot call remove[Unchecked]() directly b/c of ConcurrentModificationException.
            iter.remove();
        }
        return -1;
    }

    /**
     * Returns the list of integer indexed properties.
     * 
     * @return the list of integer indexed properties
     */
    public final long[] indices() {
        return indexedProperties.indices();
    }

    /**
     * Returns the list of integer indexed properties.
     * 
     * @param from
     *            from index (inclusive)
     * @param to
     *            to index (exclusive)
     * @return the list of integer indexed properties
     */
    public final long[] indices(long from, long to) {
        return indexedProperties.indices(from, to);
    }

    /**
     * Returns {@code true} if this object has indexed properties.
     * 
     * @return {@code true} if this object has indexed properties
     */
    public final boolean hasIndexedProperties() {
        return !indexedProperties.isEmpty();
    }

    /**
     * Returns {@code true} if the object has indexed accessors.
     * 
     * @return {@code true} if the object has indexed accessors
     */
    public boolean hasIndexedAccessors() {
        if (indexedProperties.isEmpty()) {
            return false;
        }
        for (Iterator<Property> iter = indexedProperties.valuesIterator(); iter.hasNext();) {
            if (iter.next().isAccessorDescriptor()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the object's own "length" property.
     * 
     * @return the length or {@code -1} if not available
     */
    public long getLength() {
        Property length = ordinaryGetOwnProperty("length");
        if (length == null || !length.isDataDescriptor() || !Type.isNumber(length.getValue())) {
            return -1;
        }
        return ToLength(Type.numberValue(length.getValue()));
    }

    /**
     * Returns the number of indexed properties.
     * 
     * @return the number of indexed properties
     */
    public int getIndexedSize() {
        return indexedProperties.size();
    }

    /**
     * Returns {@code true} if the array is dense and has no indexed accessors.
     * 
     * @return {@code true} if the array is dense
     */
    public final boolean isDenseArray() {
        return isDenseArray(getLength());
    }

    /**
     * Returns {@code true} if the array is dense and has no indexed accessors.
     * 
     * @param length
     *            the array length
     * @return {@code true} if the array is dense
     */
    public final boolean isDenseArray(long length) {
        assert !hasSpecialIndexedProperties() : "cannot report dense if special indexed present";
        IndexedMap<Property> ix = indexedProperties;
        return !hasIndexedAccessors() && ix.getLength() == length && !ix.isSparse() && !ix.hasHoles();
    }

    /**
     * Returns {@code true} if this object has "special" indexed properties.
     * 
     * @return {@code true} if this object has special indexed properties
     */
    public boolean hasSpecialIndexedProperties() {
        return false;
    }

    /**
     * Returns the array's indexed property values. Only applicable for dense arrays.
     * 
     * @return the array's indexed values
     */
    public final Object[] toArray() {
        return toArray(getLength());
    }

    /**
     * Returns the array's indexed property values. Only applicable for dense arrays.
     * 
     * @param length
     *            the array length
     * @return the array's indexed values
     */
    public final Object[] toArray(long length) {
        assert isDenseArray(length);
        assert 0 <= length && length <= Integer.MAX_VALUE : "length=" + length;
        int len = (int) length;
        if (len == 0) {
            return EMPTY_ARRAY;
        }
        Object[] values = new Object[len];
        for (int i = 0; i < len; ++i) {
            values[i] = getIndexed(i);
        }
        return values;
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
    protected final void setPrototype(ScriptObject prototype) {
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
    @Override
    public boolean hasOwnProperty(ExecutionContext cx, long propertyKey) {
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
    @Override
    public boolean hasOwnProperty(ExecutionContext cx, String propertyKey) {
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
    @Override
    public boolean hasOwnProperty(ExecutionContext cx, Symbol propertyKey) {
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
        assert !IndexedMap.isIndex(propertyKey);
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
        return ordinaryGetPrototypeOf();
    }

    /**
     * 9.1.1.1 OrdinaryGetPrototypeOf (O)
     * 
     * @return the prototype object or {@code null}
     */
    protected final ScriptObject ordinaryGetPrototypeOf() {
        /* step 1 */
        return prototype;
    }

    /** 9.1.2 [[SetPrototypeOf]] (V) */
    @Override
    public boolean setPrototypeOf(ExecutionContext cx, ScriptObject prototype) {
        /* step 1 */
        return ordinarySetPrototypeOf(prototype);
    }

    /**
     * 9.1.2.1 OrdinarySetPrototypeOf (O, V)
     * 
     * @param prototype
     *            the new prototype object (or {@code null})
     * @return {@code true} on success
     */
    protected final boolean ordinarySetPrototypeOf(ScriptObject prototype) {
        /* step 1 (implicit) */
        /* step 2 */
        boolean extensible = this.extensible;
        /* step 3 */
        ScriptObject current = this.prototype;
        /* step 4 */
        if (prototype == current) { // SameValue(prototype, current)
            return true;
        }
        /* step 5 */
        if (!extensible) {
            return false;
        }
        /* steps 6-8 */
        for (ScriptObject p = prototype; p != null;) {
            if (p == this) { // SameValue(p, O)
                return false;
            }
            if (!(p instanceof OrdinaryObject)) {
                break;
            }
            p = ((OrdinaryObject) p).prototype;
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
        return ordinaryIsExtensible();
    }

    /**
     * 9.1.3.1 ordinaryIsExtensible (O)
     * 
     * @return {@code true} if the object is extensible
     */
    protected final boolean ordinaryIsExtensible() {
        /* step 1 */
        return extensible;
    }

    /** 9.1.4 [[PreventExtensions]] ( ) */
    @Override
    public boolean preventExtensions(ExecutionContext cx) {
        /* step 1 */
        return ordinaryPreventExtensions();
    }

    /**
     * 9.1.4.1 OrdinaryPreventExtensions (O)
     * 
     * @return {@code true} on success
     */
    protected final boolean ordinaryPreventExtensions() {
        /* step 1 */
        this.extensible = false;
        /* step 2 */
        return true;
    }

    /** 9.1.5 [[GetOwnProperty]] (P) */
    @Override
    public Property getOwnProperty(ExecutionContext cx, long propertyKey) {
        /* step 1 */
        return ordinaryGetOwnProperty(propertyKey);
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
    protected final Property ordinaryGetOwnProperty(long propertyKey) {
        /* steps 1-9 (NB: returns live view on the property, not a property descriptor object!) */
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
        assert !IndexedMap.isIndex(propertyKey);
        /* steps 1-9 (NB: returns live view on the property, not a property descriptor object!) */
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
        /* steps 1-9 (NB: returns live view on the property, not a property descriptor object!) */
        return symbolProperties.get(propertyKey);
    }

    /** 9.1.6 [[DefineOwnProperty]] (P, Desc) */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, long propertyKey, PropertyDescriptor desc) {
        /* step 1 */
        return ordinaryDefineOwnProperty(cx, propertyKey, desc);
    }

    /** 9.1.6 [[DefineOwnProperty]] (P, Desc) */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, String propertyKey, PropertyDescriptor desc) {
        /* step 1 */
        return ordinaryDefineOwnProperty(cx, propertyKey, desc);
    }

    /** 9.1.6 [[DefineOwnProperty]] (P, Desc) */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, Symbol propertyKey, PropertyDescriptor desc) {
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
    protected final boolean ordinaryDefineOwnProperty(ExecutionContext cx, long propertyKey, PropertyDescriptor desc) {
        /* step 1 */
        Property current = getOwnProperty(cx, propertyKey);
        /* step 2 */
        boolean extensible = isExtensible();
        /* step 3 */
        return validateAndApplyPropertyDescriptor(indexedProperties, propertyKey, extensible, desc, current);
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
        assert !IndexedMap.isIndex(propertyKey);
        /* step 1 */
        Property current = getOwnProperty(cx, propertyKey);
        /* step 2 */
        boolean extensible = isExtensible();
        /* step 3 */
        return validateAndApplyPropertyDescriptor(properties, propertyKey, extensible, desc, current);
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
        return validateAndApplyPropertyDescriptor(symbolProperties, propertyKey, extensible, desc, current);
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
    protected static final boolean IsCompatiblePropertyDescriptor(boolean extensible, PropertyDescriptor desc,
            Property current) {
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
    private static final <KEY> boolean validateAndApplyPropertyDescriptor(PropertyMap<KEY, Property> object,
            KEY propertyKey, boolean extensible, PropertyDescriptor desc, Property current) {
        /* step 1 */
        assert object == null || propertyKey != null;
        /* step 2 */
        if (current == null) {
            if (!extensible) {
                return false;
            }
            if (object != null) {
                object.put(propertyKey, desc.toProperty());
            }
            return true;
        }
        /* step 3 */
        if (desc.isEmpty()) {
            return true;
        }
        /* step 4 */
        if (!current.isConfigurable()) {
            if (desc.isConfigurable()) {
                return false;
            }
            if (desc.hasEnumerable() && desc.isEnumerable() != current.isEnumerable()) {
                return false;
            }
        }
        if (desc.isGenericDescriptor()) {
            /* step 5 */
            // no further validation required, proceed below...
        } else if (desc.isDataDescriptor() != current.isDataDescriptor()) {
            /* step 6 */
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
        } else if (desc.isDataDescriptor()) {
            /* step 7 */
            assert current.isDataDescriptor();
            if (!current.isConfigurable() && !current.isWritable()) {
                if (desc.isWritable()) {
                    return false;
                }
                if (desc.hasValue() && !SameValue(desc.getValue(), current.getValue())) {
                    return false;
                }
                return true;
            }
        } else {
            /* step 8 */
            assert desc.isAccessorDescriptor() && current.isAccessorDescriptor();
            if (!current.isConfigurable()) {
                if (desc.hasSetter() && desc.getSetter() != current.getSetter()) {
                    return false;
                }
                if (desc.hasGetter() && desc.getGetter() != current.getGetter()) {
                    return false;
                }
                return true;
            }
        }
        /* step 9 */
        if (object != null) {
            object.get(propertyKey).apply(desc);
        }
        /* step 10 */
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
    private static final boolean validateAndApplyPropertyDescriptor(IndexedMap<Property> object, long propertyKey,
            boolean extensible, PropertyDescriptor desc, Property current) {
        /* step 1 */
        assert object == null || IndexedMap.isIndex(propertyKey);
        /* step 2 */
        if (current == null) {
            if (!extensible) {
                return false;
            }
            if (object != null) {
                object.put(propertyKey, desc.toProperty());
            }
            return true;
        }
        /* step 3 */
        if (desc.isEmpty()) {
            return true;
        }
        /* step 4 */
        if (!current.isConfigurable()) {
            if (desc.isConfigurable()) {
                return false;
            }
            if (desc.hasEnumerable() && desc.isEnumerable() != current.isEnumerable()) {
                return false;
            }
        }
        if (desc.isGenericDescriptor()) {
            /* step 5 */
            // no further validation required, proceed below...
        } else if (desc.isDataDescriptor() != current.isDataDescriptor()) {
            /* step 6 */
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
        } else if (desc.isDataDescriptor()) {
            /* step 7 */
            assert current.isDataDescriptor();
            if (!current.isConfigurable() && !current.isWritable()) {
                if (desc.isWritable()) {
                    return false;
                }
                if (desc.hasValue() && !SameValue(desc.getValue(), current.getValue())) {
                    return false;
                }
                return true;
            }
        } else {
            /* step 8 */
            assert desc.isAccessorDescriptor() && current.isAccessorDescriptor();
            if (!current.isConfigurable()) {
                if (desc.hasSetter() && desc.getSetter() != current.getSetter()) {
                    return false;
                }
                if (desc.hasGetter() && desc.getGetter() != current.getGetter()) {
                    return false;
                }
                // FIXME: spec issue - remove the explicit `return true` and simply fall-through similar to the generic
                // property descriptor case?
                return true;
            }
        }
        /* step 9 */
        if (object != null) {
            object.get(propertyKey).apply(desc);
        }
        /* step 10 */
        return true;
    }

    /**
     * 9.1.7 [[HasProperty]](P)
     */
    @Override
    public boolean hasProperty(ExecutionContext cx, long propertyKey) {
        /* step 1 */
        return ordinaryHasProperty(cx, propertyKey);
    }

    /**
     * 9.1.7 [[HasProperty]](P)
     */
    @Override
    public boolean hasProperty(ExecutionContext cx, String propertyKey) {
        /* step 1 */
        return ordinaryHasProperty(cx, propertyKey);
    }

    /**
     * 9.1.7 [[HasProperty]](P)
     */
    @Override
    public boolean hasProperty(ExecutionContext cx, Symbol propertyKey) {
        /* step 1 */
        return ordinaryHasProperty(cx, propertyKey);
    }

    /**
     * 9.1.7.1 OrdinaryHasProperty (O, P)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return {@code true} if the property was found
     */
    protected final boolean ordinaryHasProperty(ExecutionContext cx, long propertyKey) {
        /* step 1 (implicit) */
        /* step 2 */
        boolean hasOwn = hasOwnProperty(cx, propertyKey);
        /* step 3 */
        if (hasOwn) {
            return true;
        }
        /* step 4 */
        ScriptObject parent = getPrototypeOf(cx);
        /* step 5 */
        if (parent != null) {
            return parent.hasProperty(cx, propertyKey);
        }
        /* step 6 */
        return false;
    }

    /**
     * 9.1.7.1 OrdinaryHasProperty (O, P)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return {@code true} if the property was found
     */
    protected final boolean ordinaryHasProperty(ExecutionContext cx, String propertyKey) {
        /* step 1 (implicit) */
        /* step 2 */
        boolean hasOwn = hasOwnProperty(cx, propertyKey);
        /* step 3 */
        if (hasOwn) {
            return true;
        }
        /* step 4 */
        ScriptObject parent = getPrototypeOf(cx);
        /* step 5 */
        if (parent != null) {
            return parent.hasProperty(cx, propertyKey);
        }
        /* step 6 */
        return false;
    }

    /**
     * 9.1.7.1 OrdinaryHasProperty (O, P)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return {@code true} if the property was found
     */
    protected final boolean ordinaryHasProperty(ExecutionContext cx, Symbol propertyKey) {
        /* step 1 (implicit) */
        /* step 2 */
        boolean hasOwn = hasOwnProperty(cx, propertyKey);
        /* step 3 */
        if (hasOwn) {
            return true;
        }
        /* step 4 */
        ScriptObject parent = getPrototypeOf(cx);
        /* step 5 */
        if (parent != null) {
            return parent.hasProperty(cx, propertyKey);
        }
        /* step 6 */
        return false;
    }

    /** 9.1.8 [[Get]] (P, Receiver) */
    @Override
    public Object get(ExecutionContext cx, long propertyKey, Object receiver) {
        /* step 1 */
        return ordinaryGet(cx, propertyKey, receiver);
    }

    /** 9.1.8 [[Get]] (P, Receiver) */
    @Override
    public Object get(ExecutionContext cx, String propertyKey, Object receiver) {
        /* step 1 */
        return ordinaryGet(cx, propertyKey, receiver);
    }

    /** 9.1.8 [[Get]] (P, Receiver) */
    @Override
    public Object get(ExecutionContext cx, Symbol propertyKey, Object receiver) {
        /* step 1 */
        return ordinaryGet(cx, propertyKey, receiver);
    }

    /**
     * 9.1.8.1 OrdinaryGet (O, P, Receiver)
     *
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @param receiver
     *            the receiver object
     * @return the property value
     */
    protected final Object ordinaryGet(ExecutionContext cx, long propertyKey, Object receiver) {
        /* step 1 (implicit) */
        /* step 2 */
        Property desc = getOwnProperty(cx, propertyKey);
        /* step 3 */
        if (desc == null) {
            ScriptObject parent = getPrototypeOf(cx);
            if (parent == null) {
                return UNDEFINED;
            }
            return parent.get(cx, propertyKey, receiver);
        }
        /* step 4 */
        if (desc.isDataDescriptor()) {
            return desc.getValue();
        }
        /* step 5 */
        assert desc.isAccessorDescriptor();
        /* step 6 */
        Callable getter = desc.getGetter();
        /* step 7 */
        if (getter == null) {
            return UNDEFINED;
        }
        /* step 8 */
        return getter.call(cx, receiver, EMPTY_ARRAY);
    }

    /**
     * 9.1.8.1 OrdinaryGet (O, P, Receiver)
     *
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @param receiver
     *            the receiver object
     * @return the property value
     */
    protected final Object ordinaryGet(ExecutionContext cx, String propertyKey, Object receiver) {
        /* step 1 (implicit) */
        /* step 2 */
        Property desc = getOwnProperty(cx, propertyKey);
        /* step 3 */
        if (desc == null) {
            ScriptObject parent = getPrototypeOf(cx);
            if (parent == null) {
                return UNDEFINED;
            }
            return parent.get(cx, propertyKey, receiver);
        }
        /* step 4 */
        if (desc.isDataDescriptor()) {
            return desc.getValue();
        }
        /* step 5 */
        assert desc.isAccessorDescriptor();
        /* step 6 */
        Callable getter = desc.getGetter();
        /* step 7 */
        if (getter == null) {
            return UNDEFINED;
        }
        /* step 8 */
        return getter.call(cx, receiver, EMPTY_ARRAY);
    }

    /**
     * 9.1.8.1 OrdinaryGet (O, P, Receiver)
     *
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @param receiver
     *            the receiver object
     * @return the property value
     */
    protected final Object ordinaryGet(ExecutionContext cx, Symbol propertyKey, Object receiver) {
        /* step 1 (implicit) */
        /* step 2 */
        Property desc = getOwnProperty(cx, propertyKey);
        /* step 3 */
        if (desc == null) {
            ScriptObject parent = getPrototypeOf(cx);
            if (parent == null) {
                return UNDEFINED;
            }
            return parent.get(cx, propertyKey, receiver);
        }
        /* step 4 */
        if (desc.isDataDescriptor()) {
            return desc.getValue();
        }
        /* step 5 */
        assert desc.isAccessorDescriptor();
        /* step 6 */
        Callable getter = desc.getGetter();
        /* step 7 */
        if (getter == null) {
            return UNDEFINED;
        }
        /* step 8 */
        return getter.call(cx, receiver, EMPTY_ARRAY);
    }

    /** 9.1.9 [[Set] (P, V, Receiver) */
    @Override
    public boolean set(ExecutionContext cx, long propertyKey, Object value, Object receiver) {
        /* step 1 */
        return ordinarySet(cx, propertyKey, value, receiver);
    }

    /** 9.1.9 [[Set] (P, V, Receiver) */
    @Override
    public boolean set(ExecutionContext cx, String propertyKey, Object value, Object receiver) {
        /* step 1 */
        return ordinarySet(cx, propertyKey, value, receiver);
    }

    /** 9.1.9 [[Set] (P, V, Receiver) */
    @Override
    public boolean set(ExecutionContext cx, Symbol propertyKey, Object value, Object receiver) {
        /* step 1 */
        return ordinarySet(cx, propertyKey, value, receiver);
    }

    /**
     * 9.1.9.1 OrdinarySet (O, P, V, Receiver)
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
    protected final boolean ordinarySet(ExecutionContext cx, long propertyKey, Object value, Object receiver) {
        /* step 1 (implicit) */
        /* step 2 */
        Property ownDesc = getOwnProperty(cx, propertyKey);
        /* step 3 */
        if (ownDesc == null) {
            ScriptObject parent = getPrototypeOf(cx);
            if (parent != null) {
                return parent.set(cx, propertyKey, value, receiver);
            } else {
                ownDesc = new Property(UNDEFINED, true, true, true);
            }
        } else if (receiver == this && ownDesc.isWritable()) {
            // Optimize the common case of own, writable properties.
            ownDesc.setValue(value);
            return true;
        }
        /* step 4 */
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
                if (existingDescriptor.isAccessorDescriptor() || !existingDescriptor.isWritable()) {
                    return false;
                }
                PropertyDescriptor valueDesc = new PropertyDescriptor(value);
                return _receiver.defineOwnProperty(cx, propertyKey, valueDesc);
            } else {
                return CreateDataProperty(cx, _receiver, propertyKey, value);
            }
        }
        /* step 5 */
        assert ownDesc.isAccessorDescriptor();
        /* step 6 */
        Callable setter = ownDesc.getSetter();
        /* step 7 */
        if (setter == null) {
            return false;
        }
        /* step 8 */
        setter.call(cx, receiver, value);
        /* step 9 */
        return true;
    }

    /**
     * 9.1.9.1 OrdinarySet (O, P, V, Receiver)
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
    protected final boolean ordinarySet(ExecutionContext cx, String propertyKey, Object value, Object receiver) {
        /* step 1 (implicit) */
        /* step 2 */
        Property ownDesc = getOwnProperty(cx, propertyKey);
        /* step 3 */
        if (ownDesc == null) {
            ScriptObject parent = getPrototypeOf(cx);
            if (parent != null) {
                return parent.set(cx, propertyKey, value, receiver);
            } else {
                ownDesc = new Property(UNDEFINED, true, true, true);
            }
        } else if (receiver == this && ownDesc.isWritable()) {
            // Optimize the common case of own, writable properties.
            ownDesc.setValue(value);
            return true;
        }
        /* step 4 */
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
                if (existingDescriptor.isAccessorDescriptor() || !existingDescriptor.isWritable()) {
                    return false;
                }
                PropertyDescriptor valueDesc = new PropertyDescriptor(value);
                return _receiver.defineOwnProperty(cx, propertyKey, valueDesc);
            } else {
                return CreateDataProperty(cx, _receiver, propertyKey, value);
            }
        }
        /* step 5 */
        assert ownDesc.isAccessorDescriptor();
        /* step 6 */
        Callable setter = ownDesc.getSetter();
        /* step 7 */
        if (setter == null) {
            return false;
        }
        /* step 8 */
        setter.call(cx, receiver, value);
        /* step 9 */
        return true;
    }

    /**
     * 9.1.9.1 OrdinarySet (O, P, V, Receiver)
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
    protected final boolean ordinarySet(ExecutionContext cx, Symbol propertyKey, Object value, Object receiver) {
        /* step 1 (implicit) */
        /* step 2 */
        Property ownDesc = getOwnProperty(cx, propertyKey);
        /* step 3 */
        if (ownDesc == null) {
            ScriptObject parent = getPrototypeOf(cx);
            if (parent != null) {
                return parent.set(cx, propertyKey, value, receiver);
            } else {
                ownDesc = new Property(UNDEFINED, true, true, true);
            }
        } else if (receiver == this && ownDesc.isWritable()) {
            // Optimize the common case of own, writable properties.
            ownDesc.setValue(value);
            return true;
        }
        /* step 4 */
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
                if (existingDescriptor.isAccessorDescriptor() || !existingDescriptor.isWritable()) {
                    return false;
                }
                PropertyDescriptor valueDesc = new PropertyDescriptor(value);
                return _receiver.defineOwnProperty(cx, propertyKey, valueDesc);
            } else {
                return CreateDataProperty(cx, _receiver, propertyKey, value);
            }
        }
        /* step 5 */
        assert ownDesc.isAccessorDescriptor();
        /* step 6 */
        Callable setter = ownDesc.getSetter();
        /* step 7 */
        if (setter == null) {
            return false;
        }
        /* step 8 */
        setter.call(cx, receiver, value);
        /* step 9 */
        return true;
    }

    /** 9.1.10 [[Delete]] (P) */
    @Override
    public boolean delete(ExecutionContext cx, long propertyKey) {
        /* step 1 */
        return ordinaryDelete(cx, propertyKey);
    }

    /** 9.1.10 [[Delete]] (P) */
    @Override
    public boolean delete(ExecutionContext cx, String propertyKey) {
        /* step 1 */
        return ordinaryDelete(cx, propertyKey);
    }

    /** 9.1.10 [[Delete]] (P) */
    @Override
    public boolean delete(ExecutionContext cx, Symbol propertyKey) {
        /* step 1 */
        return ordinaryDelete(cx, propertyKey);
    }

    /**
     * 9.1.10.1 OrdinaryDelete (O, P)
     *
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return {@code true} if the property was successfully deleted
     */
    protected final boolean ordinaryDelete(ExecutionContext cx, long propertyKey) {
        /* step 1 (not applicable) */
        /* step 2 */
        Property desc = getOwnProperty(cx, propertyKey);
        /* step 3 */
        if (desc == null) {
            return true;
        }
        /* step 4 */
        if (desc.isConfigurable()) {
            indexedProperties.remove(propertyKey);
            return true;
        }
        /* step 5 */
        return false;
    }

    /**
     * 9.1.10.1 OrdinaryDelete (O, P)
     *
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return {@code true} if the property was successfully deleted
     */
    protected final boolean ordinaryDelete(ExecutionContext cx, String propertyKey) {
        assert !IndexedMap.isIndex(propertyKey);
        /* step 1 (not applicable) */
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

    /**
     * 9.1.10.1 OrdinaryDelete (O, P)
     *
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return {@code true} if the property was successfully deleted
     */
    protected final boolean ordinaryDelete(ExecutionContext cx, Symbol propertyKey) {
        /* step 1 (not applicable) */
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

    /** 9.1.11 [[OwnPropertyKeys]] ( ) */
    @Override
    public List<?> ownPropertyKeys(ExecutionContext cx) {
        return ordinaryOwnPropertyKeys();
    }

    @Override
    public List<String> ownPropertyNames(ExecutionContext cx) {
        ArrayList<String> keys = new ArrayList<>();
        ownPropertyIndices(keys);
        ownPropertyNames(keys);
        return keys;
    }

    @Override
    public List<Symbol> ownPropertySymbols(ExecutionContext cx) {
        ArrayList<Symbol> symbols = new ArrayList<>();
        ownPropertySymbols(symbols);
        return new ArrayList<>(symbols);
    }

    /**
     * Appends all own integer indexed properties to the target list.
     * 
     * @param list
     *            the target list
     */
    protected void ownPropertyIndices(List<? super String> list) {
        if (!indexedProperties.isEmpty()) {
            list.addAll(indexedProperties.keys());
        }
    }

    /**
     * Appends all own string valued properties to the target list.
     * 
     * @param list
     *            the target list
     */
    protected void ownPropertyNames(List<? super String> list) {
        if (!properties.isEmpty()) {
            list.addAll(properties.keySet());
        }
    }

    /**
     * Appends all own symbol valued properties to the target list.
     * 
     * @param list
     *            the target list
     */
    protected void ownPropertySymbols(List<? super Symbol> list) {
        if (!symbolProperties.isEmpty()) {
            list.addAll(symbolProperties.keySet());
        }
    }

    /**
     * 9.1.11.1 OrdinaryOwnPropertyKeys (O)
     * 
     * @return the list of own property keys
     */
    protected final List<Object> ordinaryOwnPropertyKeys() {
        /* step 1 */
        ArrayList<Object> ownKeys = new ArrayList<>();
        /* step 2 */
        ownPropertyIndices(ownKeys);
        /* step 3 */
        ownPropertyNames(ownKeys);
        /* step 4 */
        ownPropertySymbols(ownKeys);
        /* step 5 */
        return ownKeys;
    }

    /** 9.1.11 [[OwnPropertyKeys]] ( ) */
    @Override
    public Iterator<String> ownEnumerablePropertyKeys(ExecutionContext cx) {
        ArrayList<String> keys = new ArrayList<>();
        ownPropertyIndices(keys);
        ownPropertyNames(keys);
        return keys.iterator();
    }

    /**
     * Subclasses can override this method if they have virtual properties.
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return the property enumerable status
     */
    @Override
    public Enumerability isEnumerableOwnProperty(ExecutionContext cx, String propertyKey) {
        Property prop;
        long index = IndexedMap.toIndex(propertyKey);
        if (IndexedMap.isIndex(index)) {
            prop = getOwnProperty(cx, index);
        } else {
            prop = getOwnProperty(cx, propertyKey);
        }
        if (prop == null) {
            return Enumerability.Deleted;
        }
        return Enumerability.isEnumerable(prop.isEnumerable());
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

    /**
     * 9.1.12 ObjectCreate(proto, internalSlotsList)
     *
     * @param cx
     *            the execution context
     * @param proto
     *            the prototype object
     * @return the new object
     */
    public static final OrdinaryObject ObjectCreate(ExecutionContext cx, ScriptObject proto) {
        return new OrdinaryObject(cx.getRealm(), proto);
    }

    /**
     * 9.1.12 ObjectCreate(proto, internalSlotsList)
     *
     * @param cx
     *            the execution context
     * @param proto
     *            the prototype object
     * @return the new object
     */
    public static final OrdinaryObject ObjectCreate(ExecutionContext cx, Intrinsics proto) {
        return new OrdinaryObject(cx.getRealm(), cx.getIntrinsic(proto));
    }

    /**
     * 9.1.12 ObjectCreate(proto, internalSlotsList)
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
    public static final <OBJECT extends OrdinaryObject> OBJECT ObjectCreate(ExecutionContext cx, ScriptObject proto,
            ObjectAllocator<OBJECT> allocator) {
        OBJECT obj = allocator.newInstance(cx.getRealm());
        obj.setPrototype(proto);
        return obj;
    }

    /**
     * 9.1.12 ObjectCreate(proto, internalSlotsList)
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
    public static final <OBJECT extends OrdinaryObject> OBJECT ObjectCreate(ExecutionContext cx, Intrinsics proto,
            ObjectAllocator<OBJECT> allocator) {
        OBJECT obj = allocator.newInstance(cx.getRealm());
        obj.setPrototype(cx.getIntrinsic(proto));
        return obj;
    }

    /**
     * 9.1.12 ObjectCreate(proto, internalSlotsList)
     *
     * @param realm
     *            the realm instance
     * @param proto
     *            the prototype object
     * @return the new object
     */
    public static final OrdinaryObject ObjectCreate(Realm realm, ScriptObject proto) {
        return new OrdinaryObject(realm, proto);
    }

    /**
     * 9.1.12 ObjectCreate(proto, internalSlotsList)
     *
     * @param realm
     *            the realm instance
     * @param proto
     *            the prototype object
     * @return the new object
     */
    public static final OrdinaryObject ObjectCreate(Realm realm, Intrinsics proto) {
        return new OrdinaryObject(realm, realm.getIntrinsic(proto));
    }

    /**
     * 9.1.12 ObjectCreate(proto, internalSlotsList)
     *
     * @param <OBJECT>
     *            the object type
     * @param realm
     *            the realm instance
     * @param proto
     *            the prototype object
     * @param allocator
     *            the object allocator
     * @return the new object
     */
    public static final <OBJECT extends OrdinaryObject> OBJECT ObjectCreate(Realm realm, ScriptObject proto,
            ObjectAllocator<OBJECT> allocator) {
        OBJECT obj = allocator.newInstance(realm);
        obj.setPrototype(proto);
        return obj;
    }

    /**
     * 9.1.12 ObjectCreate(proto, internalSlotsList)
     *
     * @param <OBJECT>
     *            the object type
     * @param realm
     *            the realm instance
     * @param proto
     *            the prototype object
     * @param allocator
     *            the object allocator
     * @return the new object
     */
    public static final <OBJECT extends OrdinaryObject> OBJECT ObjectCreate(Realm realm, Intrinsics proto,
            ObjectAllocator<OBJECT> allocator) {
        OBJECT obj = allocator.newInstance(realm);
        obj.setPrototype(realm.getIntrinsic(proto));
        return obj;
    }

    /**
     * 9.1.13 OrdinaryCreateFromConstructor (constructor, intrinsicDefaultProto, internalSlotsList)
     * 
     * @param cx
     *            the execution context
     * @param constructor
     *            the constructor function
     * @param intrinsicDefaultProto
     *            the default prototype
     * @return the new object
     */
    public static final OrdinaryObject OrdinaryCreateFromConstructor(ExecutionContext cx, Callable constructor,
            Intrinsics intrinsicDefaultProto) {
        /* step 1 (not applicable) */
        /* step 2 */
        ScriptObject proto = GetPrototypeFromConstructor(cx, constructor, intrinsicDefaultProto);
        /* step 3 */
        return ObjectCreate(cx, proto);
    }

    /**
     * 9.1.13 OrdinaryCreateFromConstructor (constructor, intrinsicDefaultProto, internalSlotsList)
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
    public static final <OBJECT extends OrdinaryObject> OBJECT OrdinaryCreateFromConstructor(ExecutionContext cx,
            Callable constructor, Intrinsics intrinsicDefaultProto, ObjectAllocator<OBJECT> allocator) {
        /* step 1 (not applicable) */
        /* step 2 */
        ScriptObject proto = GetPrototypeFromConstructor(cx, constructor, intrinsicDefaultProto);
        /* step 3 */
        return ObjectCreate(cx, proto, allocator);
    }

    /**
     * 9.1.14 GetPrototypeFromConstructor ( constructor, intrinsicDefaultProto )
     * 
     * @param cx
     *            the execution context
     * @param constructor
     *            the constructor object
     * @param intrinsicDefaultProto
     *            the default prototype
     * @return the prototype object
     */
    public static final ScriptObject GetPrototypeFromConstructor(ExecutionContext cx, Callable constructor,
            Intrinsics intrinsicDefaultProto) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        Object proto = Get(cx, constructor, "prototype");
        /* step 4 */
        if (!Type.isObject(proto)) {
            /* step 4.a */
            Realm realm = GetFunctionRealm(cx, constructor);
            /* step 4.b */
            proto = realm.getIntrinsic(intrinsicDefaultProto);
        }
        /* step 5 */
        return Type.objectValue(proto);
    }
}
