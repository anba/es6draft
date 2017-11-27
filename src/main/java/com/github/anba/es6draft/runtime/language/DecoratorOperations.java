/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.language;

import static com.github.anba.es6draft.runtime.AbstractOperations.DefinePropertyOrThrow;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.FromPropertyDescriptor;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.ToPropertyDescriptor;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * 
 */
public final class DecoratorOperations {
    private DecoratorOperations() {
    }

    /**
     * 
     * @param object
     *            the script object
     * @param propertyKey
     *            the property key
     * @param cx
     *            the execution context
     * @return the property descriptor or {@code undefined}
     */
    public static Object propertyDescriptor(OrdinaryObject object, Object propertyKey, ExecutionContext cx) {
        return FromPropertyDescriptor(cx, object.getOwnProperty(cx, propertyKey));
    }

    /**
     * 
     * @param object
     *            the script object
     * @param propertyKey
     *            the property key
     * @param descriptor
     *            the property descriptor object
     * @param cx
     *            the execution context
     */
    public static void defineProperty(OrdinaryObject object, Object propertyKey, Object descriptor,
            ExecutionContext cx) {
        DefinePropertyOrThrow(cx, object, propertyKey, ToPropertyDescriptor(cx, descriptor));
    }
}
