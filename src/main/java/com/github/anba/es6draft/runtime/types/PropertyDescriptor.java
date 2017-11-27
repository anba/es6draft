/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.HasProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsCallable;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToBoolean;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>6 ECMAScript Data Types and Values</h1><br>
 * <h2>6.2 ECMAScript Specification Types</h2>
 * <ul>
 * <li>6.2.4 The Property Descriptor Specification Type
 * </ul>
 */
public final class PropertyDescriptor implements Cloneable {
    private static final int VALUE = 0x01;
    private static final int GET = 0x02;
    private static final int SET = 0x04;
    private static final int WRITABLE = 0x08;
    private static final int ENUMERABLE = 0x10;
    private static final int CONFIGURABLE = 0x20;

    private static final int POPULATED_ACCESSOR_DESC = GET | SET | ENUMERABLE | CONFIGURABLE;
    private static final int POPULATED_DATA_DESC = VALUE | WRITABLE | ENUMERABLE | CONFIGURABLE;

    private int present = 0;

    // default attribute values per 6.1.7.1, table 3
    private Object value = UNDEFINED;
    private Callable getter = null; // = Undefined
    private Callable setter = null; // = Undefined
    private boolean writable = false;
    private boolean enumerable = false;
    private boolean configurable = false;

    /**
     * Constructs a new empty property descriptor record.
     */
    public PropertyDescriptor() {
    }

    /**
     * Constructs a shallow copy of the supplied property descriptor.
     * 
     * @param original
     *            the original property descriptor
     */
    private PropertyDescriptor(PropertyDescriptor original) {
        present = original.present;
        value = original.value;
        getter = original.getter;
        setter = original.setter;
        writable = original.writable;
        enumerable = original.enumerable;
        configurable = original.configurable;
    }

    /**
     * Constructs a shallow copy of the supplied property.
     * 
     * @param original
     *            the original property
     */
    PropertyDescriptor(Property original) {
        present = original.isDataDescriptor() ? POPULATED_DATA_DESC : POPULATED_ACCESSOR_DESC;
        value = original.getValue();
        getter = original.getGetter();
        setter = original.getSetter();
        writable = original.isWritable();
        enumerable = original.isEnumerable();
        configurable = original.isConfigurable();
    }

    /**
     * Constructs a new data property descriptor with an initial value:<br>
     * <code>{[[Value]]: ?}</code>
     * 
     * @param value
     *            the property value
     */
    public PropertyDescriptor(Object value) {
        this.value = value;
        this.present = VALUE;
    }

    /**
     * Constructs a new data property descriptor with an initial value and initial attributes:<br>
     * <code>{[[Value]]: ?, [[Writable]]: ?, [[Enumerable]]: ?, [[Configurable]]: ?}</code>
     * 
     * @param value
     *            the property value
     * @param writable
     *            the enumerable flag
     * @param enumerable
     *            the writable flag
     * @param configurable
     *            the configurable flag
     */
    public PropertyDescriptor(Object value, boolean writable, boolean enumerable, boolean configurable) {
        this.value = value;
        this.writable = writable;
        this.enumerable = enumerable;
        this.configurable = configurable;
        this.present = VALUE | WRITABLE | ENUMERABLE | CONFIGURABLE;
    }

    /**
     * Constructs a new accessor property descriptor with initial getter and setter and initial attributes:<br>
     * <code>{[[Get]]: ?, [[Set]]: ?, [[Enumerable]]: ?, [[Configurable]]: ?}</code>
     * 
     * @param getter
     *            the accessor getter function, may be {@code null}
     * @param setter
     *            the accessor setter function, may be {@code null}
     * @param enumerable
     *            the writable flag
     * @param configurable
     *            the configurable flag
     */
    public PropertyDescriptor(Callable getter, Callable setter, boolean enumerable, boolean configurable) {
        this.getter = getter;
        this.setter = setter;
        this.enumerable = enumerable;
        this.configurable = configurable;
        this.present = GET | SET | ENUMERABLE | CONFIGURABLE;
    }

    /**
     * Constructs a new accessor property descriptor with initial getter and setter and initial attributes:<br>
     * <code>{[[Get]]: ?, [[Set]]: ?, [[Enumerable]]: ?, [[Configurable]]: ?}</code>
     * 
     * @param getter
     *            the accessor getter function, may be {@code null}
     * @param setter
     *            the accessor setter function, may be {@code null}
     * @param enumerable
     *            the writable flag
     * @param configurable
     *            the configurable flag
     * @return the new accessor property descriptor
     */
    public static PropertyDescriptor AccessorPropertyDescriptor(Callable getter, Callable setter, boolean enumerable,
            boolean configurable) {
        PropertyDescriptor desc = new PropertyDescriptor(getter, setter, enumerable, configurable);
        if (getter == null) {
            desc.present &= ~GET;
        }
        if (setter == null) {
            desc.present &= ~SET;
        }
        return desc;
    }

    /**
     * Converts this property descriptor to a {@link Property} object.
     * 
     * @return the property record for this descriptor
     */
    public Property toProperty() {
        return new Property(this);
    }

    @Override
    public PropertyDescriptor clone() {
        return new PropertyDescriptor(this);
    }

    /**
     * 6.2.4.1 IsAccessorDescriptor ( Desc )
     * 
     * @param desc
     *            the property descriptor
     * @return {@code true} if the descriptor is an accessor property
     */
    public static boolean IsAccessorDescriptor(PropertyDescriptor desc) {
        /* step 1 */
        if (desc == null) {
            return false;
        }
        /* steps 2-3 */
        return desc.isAccessorDescriptor();
    }

    /**
     * 6.2.4.1 IsAccessorDescriptor ( Desc )<br>
     * Returns {@code true} if this object is an accessor property descriptor.
     * 
     * @return {@code true} if this descriptor is an accessor property
     */
    public boolean isAccessorDescriptor() {
        return (present & (GET | SET)) != 0;
    }

    /**
     * 6.2.4.2 IsDataDescriptor ( Desc )
     * 
     * @param desc
     *            the property descriptor
     * @return {@code true} if the descriptor is a data property
     */
    public static boolean IsDataDescriptor(PropertyDescriptor desc) {
        /* step 1 */
        if (desc == null) {
            return false;
        }
        /* steps 2-3 */
        return desc.isDataDescriptor();
    }

    /**
     * 6.2.4.2 IsDataDescriptor ( Desc )<br>
     * Returns {@code true} if this object is a data property descriptor.
     * 
     * @return {@code true} if this descriptor is a data property
     */
    public boolean isDataDescriptor() {
        return (present & (VALUE | WRITABLE)) != 0;
    }

    /**
     * 6.2.4.3 IsGenericDescriptor ( Desc )
     * 
     * @param desc
     *            the property descriptor
     * @return {@code true} if the descriptor is a generic property
     */
    public static boolean IsGenericDescriptor(PropertyDescriptor desc) {
        /* step 1 */
        if (desc == null) {
            return false;
        }
        /* steps 2-3 */
        return desc.isGenericDescriptor();
    }

    /**
     * 6.2.4.3 IsGenericDescriptor ( Desc )<br>
     * Returns {@code true} if this object is a generic property descriptor.
     * 
     * @return {@code true} if this descriptor is a generic property
     */
    public boolean isGenericDescriptor() {
        return (present & (GET | SET | VALUE | WRITABLE)) == 0;
    }

    /**
     * 6.2.4.4 FromPropertyDescriptor ( Desc )
     * <p>
     * Returns {@code undefined} if the input property descriptor is {@code null}, otherwise returns a
     * {@link ScriptObject} representing the fields of this property descriptor.
     * 
     * @param cx
     *            the execution context
     * @param desc
     *            the property record
     * @return a script object for the property, or undefined if <var>desc</var> is {@code null}
     */
    public static Object FromPropertyDescriptor(ExecutionContext cx, Property desc) throws IllegalArgumentException {
        /* step 1 */
        if (desc == null) {
            return UNDEFINED;
        }
        /* steps 2-3 */
        OrdinaryObject obj = ObjectCreate(cx, Intrinsics.ObjectPrototype);
        /* steps 4-9 */
        if (desc.isDataDescriptor()) {
            obj.defineOwnProperty(cx, "value", _p(desc.getValue()));
            obj.defineOwnProperty(cx, "writable", _p(desc.isWritable()));
        } else {
            obj.defineOwnProperty(cx, "get", _p(undefinedIfNull(desc.getGetter())));
            obj.defineOwnProperty(cx, "set", _p(undefinedIfNull(desc.getSetter())));
        }
        obj.defineOwnProperty(cx, "enumerable", _p(desc.isEnumerable()));
        obj.defineOwnProperty(cx, "configurable", _p(desc.isConfigurable()));
        /* step 10 */
        return obj;
    }

    /**
     * 6.2.4.4 FromPropertyDescriptor ( Desc )
     * <p>
     * Returns {@code undefined} if the input property descriptor is {@code null}, otherwise returns a
     * {@link ScriptObject} representing the fields of this property descriptor.
     * 
     * @param cx
     *            the execution context
     * @param desc
     *            the property descriptor
     * @return a script object for the property, or undefined if <var>desc</var> is {@code null}
     */
    public static Object FromPropertyDescriptor(ExecutionContext cx, PropertyDescriptor desc)
            throws IllegalArgumentException {
        assert desc == null || !(desc.isDataDescriptor() && desc.isAccessorDescriptor());
        /* step 1 */
        if (desc == null) {
            return UNDEFINED;
        }
        /* steps 2-3 */
        OrdinaryObject obj = ObjectCreate(cx, Intrinsics.ObjectPrototype);
        /* steps 4-9 */
        if (desc.hasValue()) {
            obj.defineOwnProperty(cx, "value", _p(desc.getValue()));
        }
        if (desc.hasWritable()) {
            obj.defineOwnProperty(cx, "writable", _p(desc.isWritable()));
        }
        if (desc.hasGetter()) {
            obj.defineOwnProperty(cx, "get", _p(undefinedIfNull(desc.getGetter())));
        }
        if (desc.hasSetter()) {
            obj.defineOwnProperty(cx, "set", _p(undefinedIfNull(desc.getSetter())));
        }
        if (desc.hasEnumerable()) {
            obj.defineOwnProperty(cx, "enumerable", _p(desc.isEnumerable()));
        }
        if (desc.hasConfigurable()) {
            obj.defineOwnProperty(cx, "configurable", _p(desc.isConfigurable()));
        }
        /* step 10 */
        return obj;
    }

    private static PropertyDescriptor _p(Object value) {
        return new PropertyDescriptor(value, true, true, true);
    }

    private static Object undefinedIfNull(Callable value) {
        return value != null ? value : UNDEFINED;
    }

    /**
     * 6.2.4.5 ToPropertyDescriptor ( Obj )
     * <p>
     * Returns a new property descriptor from the input argument {@code object}, if {@code object} is not an instance of
     * {@link ScriptObject}, a TypeError is thrown.
     * 
     * @param cx
     *            the execution context
     * @param object
     *            the property descriptor script object
     * @return the property descriptor record
     */
    public static PropertyDescriptor ToPropertyDescriptor(ExecutionContext cx, Object object) {
        /* steps 1-2 */
        if (!Type.isObject(object)) {
            throw newTypeError(cx, Messages.Key.NotObjectType);
        }
        ScriptObject obj = Type.objectValue(object);
        /* steps 3-9 */
        PropertyDescriptor desc = new PropertyDescriptor();
        if (HasProperty(cx, obj, "enumerable")) {
            boolean enumerable = ToBoolean(Get(cx, obj, "enumerable"));
            desc.setEnumerable(enumerable);
        }
        if (HasProperty(cx, obj, "configurable")) {
            boolean configurable = ToBoolean(Get(cx, obj, "configurable"));
            desc.setConfigurable(configurable);
        }
        if (HasProperty(cx, obj, "value")) {
            Object value = Get(cx, obj, "value");
            desc.setValue(value);
        }
        if (HasProperty(cx, obj, "writable")) {
            boolean writable = ToBoolean(Get(cx, obj, "writable"));
            desc.setWritable(writable);
        }
        if (HasProperty(cx, obj, "get")) {
            Object getter = Get(cx, obj, "get");
            if (!(IsCallable(getter) || Type.isUndefined(getter))) {
                throw newTypeError(cx, Messages.Key.InvalidGetter);
            }
            desc.setGetter(callableOrNull(getter));
        }
        if (HasProperty(cx, obj, "set")) {
            Object setter = Get(cx, obj, "set");
            if (!(IsCallable(setter) || Type.isUndefined(setter))) {
                throw newTypeError(cx, Messages.Key.InvalidSetter);
            }
            desc.setSetter(callableOrNull(setter));
        }
        /* step 10 */
        if ((desc.present & (GET | SET)) != 0 && (desc.present & (VALUE | WRITABLE)) != 0) {
            throw newTypeError(cx, Messages.Key.InvalidDescriptor);
        }
        /* step 11 */
        return desc;
    }

    private static Callable callableOrNull(Object value) {
        return value instanceof Callable ? (Callable) value : null;
    }

    /**
     * 6.2.4.6 CompletePropertyDescriptor ( Desc, LikeDesc )
     * 
     * @param desc
     *            the property descriptor
     * @return the <var>desc</var> parameter
     */
    public static PropertyDescriptor CompletePropertyDescriptor(PropertyDescriptor desc) {
        /* steps 1-2 (implicit) */
        /* step 3 (omitted) */
        /* steps 4-5 */
        if (IsGenericDescriptor(desc) || IsDataDescriptor(desc)) {
            /* step 4 */
            if (!desc.hasValue()) {
                desc.setValue(UNDEFINED);
            }
            if (!desc.hasWritable()) {
                desc.setWritable(false);
            }
        } else {
            /* step 5 */
            if (!desc.hasGetter()) {
                desc.setGetter(null);
            }
            if (!desc.hasSetter()) {
                desc.setSetter(null);
            }
        }
        /* step 6 */
        if (!desc.hasEnumerable()) {
            desc.setEnumerable(false);
        }
        /* step 7 */
        if (!desc.hasConfigurable()) {
            desc.setConfigurable(false);
        }
        /* step 8 */
        return desc;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append('{');
        if (hasWritable()) {
            sb.append("[[Writable]]: ").append(isWritable()).append(", ");
        }
        if (hasEnumerable()) {
            sb.append("[[Enumerable]]: ").append(isEnumerable()).append(", ");
        }
        if (hasConfigurable()) {
            sb.append("[[Configurable]]: ").append(isConfigurable()).append(", ");
        }
        if (hasValue()) {
            sb.append("[[Value]]: ").append(getValue()).append(", ");
        }
        if (hasGetter()) {
            sb.append("[[Get]]: ").append(getGetter()).append(", ");
        }
        if (hasSetter()) {
            sb.append("[[Set]]: ").append(getSetter()).append(", ");
        }
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 2);
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * Returns {@code true} if every field of this property descriptor is absent.
     * 
     * @return {@code true} if every field is absent
     */
    public boolean isEmpty() {
        return present == 0;
    }

    /**
     * Returns {@code true} if the <code>[[Value]]</code> field is present.
     * 
     * @return {@code true} if the value field is present
     */
    public boolean hasValue() {
        return (present & VALUE) != 0;
    }

    /**
     * Returns {@code true} if the <code>[[Get]]</code> field is present.
     * 
     * @return {@code true} if the getter field is present
     */
    public boolean hasGetter() {
        return (present & GET) != 0;
    }

    /**
     * Returns {@code true} if the <code>[[Set]]</code> field is present.
     * 
     * @return {@code true} if the setter field is present
     */
    public boolean hasSetter() {
        return (present & SET) != 0;
    }

    /**
     * Returns {@code true} if the <code>[[Writable]]</code> field is present.
     * 
     * @return {@code true} if the writable field is present
     */
    public boolean hasWritable() {
        return (present & WRITABLE) != 0;
    }

    /**
     * Returns {@code true} if the <code>[[Enumerable]]</code> field is present.
     * 
     * @return {@code true} if the enumerable field is present
     */
    public boolean hasEnumerable() {
        return (present & ENUMERABLE) != 0;
    }

    /**
     * Returns {@code true} if the <code>[[Configurable]]</code> field is present.
     * 
     * @return {@code true} if the configurable field is present
     */
    public boolean hasConfigurable() {
        return (present & CONFIGURABLE) != 0;
    }

    /**
     * Returns the <code>[[Value]]</code> field or its default value.
     * 
     * @return the value field
     */
    public Object getValue() {
        return value;
    }

    /**
     * Sets the <code>[[Value]]</code> field to the argument value.
     * 
     * @param value
     *            the new value
     */
    public void setValue(Object value) {
        present |= VALUE;
        this.value = value;
    }

    /**
     * Returns the <code>[[Get]]</code> field or its default value.
     * 
     * @return the getter accessor field
     */
    public Callable getGetter() {
        return getter;
    }

    /**
     * Sets the <code>[[Get]]</code> field to the argument value.
     * 
     * @param getter
     *            the new getter function
     */
    public void setGetter(Callable getter) {
        present |= GET;
        this.getter = getter;
    }

    /**
     * Returns the <code>[[Set]]</code> field or its default value.
     * 
     * @return the setter accessor field
     */
    public Callable getSetter() {
        return setter;
    }

    /**
     * Sets the <code>[[Set]]</code> field to the argument value.
     * 
     * @param setter
     *            the new setter function
     */
    public void setSetter(Callable setter) {
        present |= SET;
        this.setter = setter;
    }

    /**
     * Returns the <code>[[Writable]]</code> field or its default value.
     * 
     * @return the writable field
     */
    public boolean isWritable() {
        return writable;
    }

    /**
     * Sets the <code>[[Writable]]</code> field to the argument value.
     * 
     * @param writable
     *            the new writable mode
     */
    public void setWritable(boolean writable) {
        present |= WRITABLE;
        this.writable = writable;
    }

    /**
     * Returns the <code>[[Enumerable]]</code> field or its default value.
     * 
     * @return the enumerable field
     */
    public boolean isEnumerable() {
        return enumerable;
    }

    /**
     * Sets the <code>[[Enumerable]]</code> field to the argument value.
     * 
     * @param enumerable
     *            the new enumerable mode
     */
    public void setEnumerable(boolean enumerable) {
        present |= ENUMERABLE;
        this.enumerable = enumerable;
    }

    /**
     * Returns the <code>[[Configurable]]</code> field or its default value.
     * 
     * @return the configurable field
     */
    public boolean isConfigurable() {
        return configurable;
    }

    /**
     * Sets the <code>[[Configurable]]</code> field to the argument value.
     * 
     * @param configurable
     *            the new configurable mode
     */
    public void setConfigurable(boolean configurable) {
        present |= CONFIGURABLE;
        this.configurable = configurable;
    }
}
