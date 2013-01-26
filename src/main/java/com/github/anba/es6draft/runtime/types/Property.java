/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types;

import static com.github.anba.es6draft.runtime.AbstractOperations.SameValue;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

/**
 * <h1>8 Types</h1><br>
 * <h2>8.1 ECMAScript Language Types</h2>
 * <ul>
 * <li>8.1.6.1 Property Attributes
 * </ul>
 */
public final class Property {
    private static final int VALUE = 0x01;
    private static final int GET = 0x02;
    private static final int SET = 0x04;
    private static final int WRITABLE = 0x08;
    private static final int ENUMERABLE = 0x10;
    private static final int CONFIGURABLE = 0x20;

    private static final int POPULATED_ACCESSOR_DESC = GET | SET | ENUMERABLE | CONFIGURABLE;
    private static final int POPULATED_DATA_DESC = VALUE | WRITABLE | ENUMERABLE | CONFIGURABLE;

    private int present = 0;

    // default attribute values per 8.1.6.1, table 7
    private Object value = UNDEFINED;
    private Callable getter = null; // = Undefined
    private Callable setter = null; // = Undefined
    private boolean writable = false;
    private boolean enumerable = false;
    private boolean configurable = false;

    // package-private for PropertyDescriptor
    Property(PropertyDescriptor original) {
        present = original.isAccessorDescriptor() ? POPULATED_ACCESSOR_DESC : POPULATED_DATA_DESC;
        value = original.getValue();
        getter = original.getGetter();
        setter = original.getSetter();
        writable = original.isWritable();
        enumerable = original.isEnumerable();
        configurable = original.isConfigurable();
    }

    public void toDataProperty() {
        assert present == POPULATED_ACCESSOR_DESC;
        present = POPULATED_DATA_DESC;
        value = UNDEFINED;
        getter = null;
        setter = null;
        writable = false;
    }

    public void toAccessorProperty() {
        assert present == POPULATED_DATA_DESC;
        present = POPULATED_ACCESSOR_DESC;
        value = UNDEFINED;
        getter = null;
        setter = null;
        writable = false;
    }

    public void apply(PropertyDescriptor desc) {
        if (isDataDescriptor()) {
            if (desc.hasValue()) {
                value = desc.getValue();
            }
            if (desc.hasWritable()) {
                writable = desc.isWritable();
            }
        } else {
            if (desc.hasGetter()) {
                getter = desc.getGetter();
            }
            if (desc.hasSetter()) {
                setter = desc.getSetter();
            }
        }
        if (desc.hasEnumerable()) {
            enumerable = desc.isEnumerable();
        }
        if (desc.hasConfigurable()) {
            configurable = desc.isConfigurable();
        }
    }

    public PropertyDescriptor toPropertyDescriptor() {
        return new PropertyDescriptor(this);
    }

    /**
     * 8.2.5.1 IsAccessorDescriptor ( Desc )<br>
     * Returns {@code true} if this object is an accessor property descriptor
     */
    public final boolean isAccessorDescriptor() {
        return (present & (GET | SET)) != 0;
    }

    /**
     * 8.2.5.2 IsDataDescriptor ( Desc )<br>
     * Returns {@code true} if this object is a data property descriptor
     */
    public final boolean isDataDescriptor() {
        return (present & (VALUE | WRITABLE)) != 0;
    }

    /**
     * Returns {@code true} if every field of {@code desc} also occurs in this property descriptor
     * and every present field has the same value. That means {@code true} is returned iff
     * {@code desc} &#8838; {@code this} holds.
     */
    public final boolean isSubset(PropertyDescriptor desc) {
        if (desc.hasValue() && !(hasValue() && SameValue(desc.getValue(), value))) {
            return false;
        }
        if (desc.hasGetter() && !(hasGetter() && SameValue(desc.getGetter(), getter))) {
            return false;
        }
        if (desc.hasSetter() && !(hasSetter() && SameValue(desc.getSetter(), setter))) {
            return false;
        }
        if (desc.hasWritable() && !(hasWritable() && desc.isWritable() == writable)) {
            return false;
        }
        if (desc.hasEnumerable() && !(hasEnumerable() && desc.isEnumerable() == enumerable)) {
            return false;
        }
        if (desc.hasConfigurable() && !(hasConfigurable() && desc.isConfigurable() == configurable)) {
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
     * Returns the <tt>[[Get]]</tt> field or its default value
     */
    public final Callable getGetter() {
        return getter;
    }

    /**
     * Returns the <tt>[[Set]]</tt> field or its default value
     */
    public final Callable getSetter() {
        return setter;
    }

    /**
     * Returns the <tt>[[Writable]]</tt> field or its default value
     */
    public final boolean isWritable() {
        return writable;
    }

    /**
     * Returns the <tt>[[Enumerable]]</tt> field or its default value
     */
    public final boolean isEnumerable() {
        return enumerable;
    }

    /**
     * Returns the <tt>[[Configurable]]</tt> field or its default value
     */
    public final boolean isConfigurable() {
        return configurable;
    }

}
