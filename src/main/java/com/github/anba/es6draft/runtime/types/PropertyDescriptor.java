/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.throwTypeError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;

import com.github.anba.es6draft.runtime.Realm;

/**
 * <h1>8 Types</h1><br>
 * <h2>8.2 ECMAScript Specification Types</h2>
 * <ul>
 * <li>8.2.5 The Property Descriptor Specification Type
 * </ul>
 */
public final class PropertyDescriptor {
    private static final int VALUE = 0x01;
    private static final int GET = 0x02;
    private static final int SET = 0x04;
    private static final int WRITABLE = 0x08;
    private static final int ENUMERABLE = 0x10;
    private static final int CONFIGURABLE = 0x20;

    private static final int POPULATED_ACCESSOR_DESC = GET | SET | ENUMERABLE | CONFIGURABLE;
    private static final int POPULATED_DATA_DESC = VALUE | WRITABLE | ENUMERABLE | CONFIGURABLE;

    private int present = 0;

    // package-private for PropertyDescriptorView
    final int getPresent() {
        return present;
    }

    // default attribute values per 8.1.6.1, table 7
    private Object value = UNDEFINED;
    private Callable getter = null; // = Undefined
    private Callable setter = null; // = Undefined
    private boolean writable = false;
    private boolean enumerable = false;
    private boolean configurable = false;

    // [[Origin]]
    private Scriptable origin = null;

    public PropertyDescriptor() {
    }

    /**
     * Creates a shallow copy of the supplied property descriptor
     */
    public PropertyDescriptor(PropertyDescriptor original) {
        present = original.present;
        value = original.value;
        getter = original.getter;
        setter = original.setter;
        writable = original.writable;
        enumerable = original.enumerable;
        configurable = original.configurable;
        origin = original.origin;
    }

    // package-private for PropertyDescriptorView
    PropertyDescriptor(Property original) {
        present = original.isDataDescriptor() ? POPULATED_DATA_DESC : POPULATED_ACCESSOR_DESC;
        value = original.getValue();
        getter = original.getGetter();
        setter = original.getSetter();
        writable = original.isWritable();
        enumerable = original.isEnumerable();
        configurable = original.isConfigurable();
        origin = null;
    }

    public Property toProperty() {
        return new Property(this);
    }

    /**
     * Creates a new data property descriptor with an initial value:<br>
     * <code>{[[Value]]: ?}</code>
     */
    public PropertyDescriptor(Object value) {
        this.value = value;
        this.present = VALUE;
    }

    /**
     * Creates a new data property descriptor with an initial value and initial attributes:<br>
     * <code>{[[Value]]: ?, [[Writable]]: ?, [[Enumerable]]: ?,
     * [[Configurable]]: ?}</code>
     */
    public PropertyDescriptor(Object value, boolean writable, boolean enumerable,
            boolean configurable) {
        this.value = value;
        this.writable = writable;
        this.enumerable = enumerable;
        this.configurable = configurable;
        this.present = VALUE | WRITABLE | ENUMERABLE | CONFIGURABLE;
    }

    /**
     * Creates a new accessor property descriptor with initial getter and setter:<br>
     * <code>{[[Get]]: ?, [[Set]]: ?}</code>
     */
    public PropertyDescriptor(Callable getter, Callable setter) {
        this.getter = getter;
        this.setter = setter;
        this.present = GET | SET;
    }

    /**
     * Creates a new accessor property descriptor with initial getter and setter and initial
     * attributes:<br>
     * <code>{[[Get]]: ?, [[Set]]: ?, [[Enumerable]]: ?, [[Configurable]]: ?}
     * </code>
     */
    public PropertyDescriptor(Callable getter, Callable setter, boolean enumerable,
            boolean configurable) {
        this.getter = getter;
        this.setter = setter;
        this.enumerable = enumerable;
        this.configurable = configurable;
        this.present = GET | SET | ENUMERABLE | CONFIGURABLE;
    }

    /**
     * 8.2.5.1 IsAccessorDescriptor ( Desc )
     */
    public static boolean IsAccessorDescriptor(PropertyDescriptor desc) {
        // FIXME: spec bug (step 1 missing)
        /* step 1 */
        if (desc == null) {
            return false;
        }
        /* step 2-3 */
        return desc.isAccessorDescriptor();
    }

    /**
     * 8.2.5.1 IsAccessorDescriptor ( Desc )<br>
     * Returns {@code true} if this object is an accessor property descriptor
     */
    public final boolean isAccessorDescriptor() {
        return (present & (GET | SET)) != 0;
    }

    /**
     * 8.2.5.2 IsDataDescriptor ( Desc )
     */
    public static boolean IsDataDescriptor(PropertyDescriptor desc) {
        /* step 1 */
        if (desc == null) {
            return false;
        }
        /* step 2-3 */
        return desc.isDataDescriptor();
    }

    /**
     * 8.2.5.2 IsDataDescriptor ( Desc )<br>
     * Returns {@code true} if this object is a data property descriptor
     */
    public final boolean isDataDescriptor() {
        return (present & (VALUE | WRITABLE)) != 0;
    }

    /**
     * 8.2.5.3 IsGenericDescriptor ( Desc )
     */
    public static boolean IsGenericDescriptor(PropertyDescriptor desc) {
        /* step 1 */
        if (desc == null) {
            return false;
        }
        /* step 2-3 */
        return desc.isGenericDescriptor();
    }

    /**
     * 8.2.5.3 IsGenericDescriptor ( Desc )<br>
     * Returns {@code true} if this object is a generic property descriptor
     */
    public final boolean isGenericDescriptor() {
        return (present & (GET | SET | VALUE | WRITABLE)) == 0;
    }

    /**
     * 8.2.5.4 FromPropertyDescriptor ( Desc )
     * <p>
     * Returns {@code undefined} if the input property descriptor is {@code null}, otherwise returns
     * a {@link Scriptable} representing the fields of this property descriptor.
     */
    public static Object FromPropertyDescriptor(Realm realm, Property desc)
            throws IllegalArgumentException {
        /* step 1 */
        if (desc == null) {
            return UNDEFINED;
        }
        /* step 3-4 */
        Scriptable obj = ObjectCreate(realm);
        /* step 5-10 */
        // TODO: OrdinaryDefineOwnProperty() instead of [[DefineOwnProperty]]
        if (desc.isDataDescriptor()) {
            obj.defineOwnProperty("value", _p(desc.getValue()));
            obj.defineOwnProperty("writable", _p(desc.isWritable()));
        } else {
            obj.defineOwnProperty("get", _p(undefinedIfNull(desc.getGetter())));
            obj.defineOwnProperty("set", _p(undefinedIfNull(desc.getSetter())));
        }
        obj.defineOwnProperty("enumerable", _p(desc.isEnumerable()));
        obj.defineOwnProperty("configurable", _p(desc.isConfigurable()));
        /* step 11 */
        return obj;
    }

    /**
     * 8.2.5.4 FromPropertyDescriptor ( Desc )
     * <p>
     * Returns {@code undefined} if the input property descriptor is {@code null}, otherwise returns
     * a {@link Scriptable} representing the fields of this property descriptor.
     */
    public static Object FromPropertyDescriptor(Realm realm, PropertyDescriptor desc)
            throws IllegalArgumentException {
        // FromPropertyDescriptor assumes that Desc is a fully populated descriptor
        if (desc != null) {
            int present = desc.present;
            if ((present & ~POPULATED_ACCESSOR_DESC) != 0 && (present & ~POPULATED_DATA_DESC) != 0) {
                throw new IllegalArgumentException(String.valueOf(present));
            }
        }

        /* step 1 */
        if (desc == null) {
            return UNDEFINED;
        }
        /* step 2 */
        if (desc.origin != null) {
            return desc.origin;
        }
        /* step 3-4 */
        Scriptable obj = ObjectCreate(realm);
        /* step 5-10 */
        // TODO: OrdinaryDefineOwnProperty() instead of [[DefineOwnProperty]]
        if (desc.isDataDescriptor()) {
            obj.defineOwnProperty("value", _p(desc.getValue()));
            obj.defineOwnProperty("writable", _p(desc.isWritable()));
        } else {
            obj.defineOwnProperty("get", _p(undefinedIfNull(desc.getGetter())));
            obj.defineOwnProperty("set", _p(undefinedIfNull(desc.getSetter())));
        }
        obj.defineOwnProperty("enumerable", _p(desc.isEnumerable()));
        obj.defineOwnProperty("configurable", _p(desc.isConfigurable()));
        /* step 11 */
        return obj;
    }

    private static PropertyDescriptor _p(Object value) {
        return new PropertyDescriptor(value, true, true, true);
    }

    private static Object undefinedIfNull(Callable value) {
        return (value != null ? value : UNDEFINED);
    }

    /**
     * 8.2.5.5 ToPropertyDescriptor ( Obj )
     * <p>
     * Returns a new property descriptor from the input argument {@code object}, if {@code object}
     * is not an instance of {@link Scriptable}, a TypeError is thrown.
     */
    public static PropertyDescriptor ToPropertyDescriptor(Realm realm, Object object) {
        /* step 1-2 */
        if (!Type.isObject(object)) {
            throwTypeError(realm, "expected object");
        }
        Scriptable obj = Type.objectValue(object);
        /* step 3-9 */
        PropertyDescriptor desc = new PropertyDescriptor();
        if (HasProperty(obj, "enumerable")) {
            Object enumerable = Get(obj, "enumerable");
            desc.setEnumerable(ToBoolean(enumerable));
        }
        if (HasProperty(obj, "configurable")) {
            Object configurable = Get(obj, "configurable");
            desc.setConfigurable(ToBoolean(configurable));
        }
        if (HasProperty(obj, "value")) {
            Object value = Get(obj, "value");
            desc.setValue(value);
        }
        if (HasProperty(obj, "writable")) {
            Object writable = Get(obj, "writable");
            desc.setWritable(ToBoolean(writable));
        }
        if (HasProperty(obj, "get")) {
            Object getter = Get(obj, "get");
            if (!(IsCallable(getter) || Type.isUndefined(getter))) {
                throwTypeError(realm, "getter is not a function");
            }
            desc.setGetter(callableOrNull(getter));
        }
        if (HasProperty(obj, "set")) {
            Object setter = Get(obj, "set");
            if (!(IsCallable(setter) || Type.isUndefined(setter))) {
                throwTypeError(realm, "setter is not a function");
            }
            desc.setSetter(callableOrNull(setter));
        }
        /* step 10 */
        if ((desc.present & (GET | SET)) != 0 && (desc.present & (VALUE | WRITABLE)) != 0) {
            throwTypeError(realm, "invalid property descriptor");
        }
        /* step 11 */
        desc.origin = obj;
        /* step 12 */
        return desc;
    }

    private static Callable callableOrNull(Object value) {
        if (!(value instanceof Callable)) {
            return null;
        }
        return (Callable) value;
    }

    /**
     * 8.2.5.6 CompletePropertyDescriptor ( Desc, LikeDesc )
     */
    public static PropertyDescriptor CompletePropertyDescriptor(PropertyDescriptor desc,
            Property likeDesc) {
        /* step 1-3 (implicit) */
        /* step 4 */
        if (likeDesc == null) {
            likeDesc = new PropertyDescriptor().toProperty();
        }
        if (IsGenericDescriptor(desc) || IsDataDescriptor(desc)) {
            /* step 5 */
            if (!desc.hasValue()) {
                desc.setValue(likeDesc.getValue());
            }
            if (!desc.hasWritable()) {
                desc.setWritable(likeDesc.isWritable());
            }
        } else {
            /* step 6 */
            if (!desc.hasGetter()) {
                desc.setGetter(likeDesc.getGetter());
            }
            if (!desc.hasSetter()) {
                desc.setSetter(likeDesc.getSetter());
            }
        }
        /* step 7 */
        if (!desc.hasEnumerable()) {
            desc.setEnumerable(likeDesc.isEnumerable());
        }
        /* step 8 */
        if (!desc.hasConfigurable()) {
            desc.setConfigurable(likeDesc.isConfigurable());
        }
        /* step 9 */
        return desc;
    }

    /**
     * Returns {@code true} if every field of this property descriptor is absent
     */
    public final boolean isEmpty() {
        return present == 0;
    }

    /**
     * Returns {@code true} if every field of {@code desc} also occurs in this property descriptor
     * and every present field has the same value. That means {@code true} is returned iff
     * {@code desc} &#8838; {@code this} holds.
     */
    public final boolean isSubset(PropertyDescriptor desc) {
        if (desc.hasValue() && !(hasValue() && SameValue(desc.value, value))) {
            return false;
        }
        if (desc.hasGetter() && !(hasGetter() && SameValue(desc.getter, getter))) {
            return false;
        }
        if (desc.hasSetter() && !(hasSetter() && SameValue(desc.setter, setter))) {
            return false;
        }
        if (desc.hasWritable() && !(hasWritable() && desc.writable == writable)) {
            return false;
        }
        if (desc.hasEnumerable() && !(hasEnumerable() && desc.enumerable == enumerable)) {
            return false;
        }
        if (desc.hasConfigurable() && !(hasConfigurable() && desc.configurable == configurable)) {
            return false;
        }
        return true;
    }

    /**
     * Returns {@code true} if the <tt>[[Value]]</tt> field is present
     */
    public final boolean hasValue() {
        return (present & VALUE) != 0;
    }

    /**
     * Returns {@code true} if the <tt>[[Get]]</tt> field is present
     */
    public final boolean hasGetter() {
        return (present & GET) != 0;
    }

    /**
     * Returns {@code true} if the <tt>[[Set]]</tt> field is present
     */
    public final boolean hasSetter() {
        return (present & SET) != 0;
    }

    /**
     * Returns {@code true} if the <tt>[[Writable]]</tt> field is present
     */
    public final boolean hasWritable() {
        return (present & WRITABLE) != 0;
    }

    /**
     * Returns {@code true} if the <tt>[[Enumerable]]</tt> field is present
     */
    public final boolean hasEnumerable() {
        return (present & ENUMERABLE) != 0;
    }

    /**
     * Returns {@code true} if the <tt>[[Configurable]]</tt> field is present
     */
    public final boolean hasConfigurable() {
        return (present & CONFIGURABLE) != 0;
    }

    /**
     * Returns the <tt>[[Value]]</tt> field or its default value
     */
    public final Object getValue() {
        return value;
    }

    /**
     * Sets the <tt>[[Value]]</tt> field to the argument value
     */
    public final void setValue(Object value) {
        present |= VALUE;
        this.value = value;
    }

    /**
     * Returns the <tt>[[Get]]</tt> field or its default value
     */
    public final Callable getGetter() {
        return getter;
    }

    /**
     * Sets the <tt>[[Get]]</tt> field to the argument value
     */
    public final void setGetter(Callable getter) {
        present |= GET;
        this.getter = getter;
    }

    /**
     * Returns the <tt>[[Set]]</tt> field or its default value
     */
    public final Callable getSetter() {
        return setter;
    }

    /**
     * Sets the <tt>[[Set]]</tt> field to the argument value
     */
    public final void setSetter(Callable setter) {
        present |= SET;
        this.setter = setter;
    }

    /**
     * Returns the <tt>[[Writable]]</tt> field or its default value
     */
    public final boolean isWritable() {
        return writable;
    }

    /**
     * Sets the <tt>[[Writable]]</tt> field to the argument value
     */
    public final void setWritable(boolean writable) {
        present |= WRITABLE;
        this.writable = writable;
    }

    /**
     * Returns the <tt>[[Enumerable]]</tt> field or its default value
     */
    public final boolean isEnumerable() {
        return enumerable;
    }

    /**
     * Sets the <tt>[[Enumerable]]</tt> field to the argument value
     */
    public final void setEnumerable(boolean enumerable) {
        present |= ENUMERABLE;
        this.enumerable = enumerable;
    }

    /**
     * Returns the <tt>[[Configurable]]</tt> field or its default value
     */
    public final boolean isConfigurable() {
        return configurable;
    }

    /**
     * Sets the <tt>[[Configurable]]</tt> field to the argument value
     */
    public final void setConfigurable(boolean configurable) {
        present |= CONFIGURABLE;
        this.configurable = configurable;
    }
}
