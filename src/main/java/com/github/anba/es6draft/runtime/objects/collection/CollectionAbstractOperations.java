/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.collection;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.types.builtins.ArrayObject.ArrayCreate;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.ScriptIterator;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;

/**
 *
 */
final class CollectionAbstractOperations {
    private CollectionAbstractOperations() {
    }

    /**
     * CollectionCreate ( C, source [ , mapfn [ , thisArg ] ] )
     * 
     * @param cx
     *            the execution context
     * @param c
     *            the constructor function
     * @param source
     *            the source value
     * @return the new collection object
     */
    public static ScriptObject CollectionCreate(ExecutionContext cx, Object c, Object[] source) {
        /* step 1 */
        if (!IsConstructor(c)) {
            throw newTypeError(cx, Messages.Key.NotConstructor);
        }
        Constructor constructor = (Constructor) c;
        /* steps 2-4 (not applicable) */
        /* steps 5-8 */
        ArrayObject array = CreateArrayFromList(cx, source);
        /* step 8.b */
        return constructor.construct(cx, array);
    }

    /**
     * CollectionCreate ( C, source [ , mapfn [ , thisArg ] ] )
     * 
     * @param cx
     *            the execution context
     * @param c
     *            the constructor function
     * @param source
     *            the source value
     * @param mapfn
     *            the optional mapping function
     * @param thisArg
     *            the this-argument for the mapping function
     * @return the new collection object
     */
    public static ScriptObject CollectionCreate(ExecutionContext cx, Object c, Object source, Object mapfn,
            Object thisArg) {
        /* step 1 */
        if (!IsConstructor(c)) {
            throw newTypeError(cx, Messages.Key.NotConstructor);
        }
        Constructor constructor = (Constructor) c;
        /* steps 2-4 (not applicable) */
        Callable mapper;
        boolean mapping;
        if (Type.isUndefined(mapfn)) {
            mapper = null;
            mapping = false;
        } else {
            if (!IsCallable(mapfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            mapper = (Callable) mapfn;
            mapping = true;
        }
        /* step 5 */
        ArrayObject array = ArrayCreate(cx, 0);
        /* step 6 */
        ScriptIterator<?> iterator = GetIterator(cx, source);
        /* steps 7-8 */
        try {
            for (int n = 0; iterator.hasNext(); ++n) {
                // FIXME: spec issue - need to test n is less than 2^53-1
                /* step 8.c */
                Object nextItem = iterator.next();
                /* steps 8.d-e */
                Object value;
                if (mapping) {
                    value = mapper.call(cx, thisArg, nextItem, n);
                } else {
                    value = nextItem;
                }
                /* step 8.f */
                CreateDataProperty(cx, array, n, value);
                /* step 8.g */
                // FIXME: spec issue - CreateDataProperty not fallible for array
            }
        } catch (ScriptException e) {
            iterator.close(e);
            throw e;
        }
        /* step 8.b */
        return constructor.construct(cx, array);
    }
}
