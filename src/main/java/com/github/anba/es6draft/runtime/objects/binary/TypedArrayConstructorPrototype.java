/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.binary.TypedArrayConstructor.TypedArrayCreate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.ScriptIterator;
import com.github.anba.es6draft.runtime.internal.ScriptIterators;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;

/**
 * <h1>22 Indexed Collections</h1><br>
 * <h2>22.2 TypedArray Objects</h2>
 * <ul>
 * <li>22.2.1 The %TypedArray% Intrinsic Object
 * <li>22.2.2 Properties of the %TypedArray% Intrinsic Object
 * </ul>
 */
public final class TypedArrayConstructorPrototype extends BuiltinConstructor implements Initializable {
    /**
     * Constructs a new TypedArray constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public TypedArrayConstructorPrototype(Realm realm) {
        super(realm, "TypedArray", 0);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * 22.2.1.1 %TypedArray% ( )
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        throw newTypeError(calleeContext(), Messages.Key.InvalidCall, "TypedArray");
    }

    /**
     * 22.2.1.1 %TypedArray% ( )
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
        /* step 1 */
        throw newTypeError(calleeContext(), Messages.Key.TypedArrayCreate);
    }

    /**
     * 22.2.2 Properties of the %TypedArray% Intrinsic Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 0;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = "TypedArray";

        /**
         * 22.2.2.3 %TypedArray%.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.TypedArrayPrototype;

        /**
         * 22.2.2.1 %TypedArray%.from ( source [ , mapfn [ , thisArg ] ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param source
         *            the source object
         * @param mapfn
         *            the optional mapper function
         * @param thisArg
         *            the optional this-argument for the mapper
         * @return the new typed array object
         */
        @Function(name = "from", arity = 1)
        public static Object from(ExecutionContext cx, Object thisValue, Object source, Object mapfn, Object thisArg) {
            /* step 1 */
            Object c = thisValue;
            /* step 2 */
            if (!IsConstructor(c)) {
                throw newTypeError(cx, Messages.Key.NotConstructor);
            }
            /* steps 3-4 */
            Callable mapper;
            boolean mapping;
            if (!Type.isUndefined(mapfn)) {
                if (!IsCallable(mapfn)) {
                    throw newTypeError(cx, Messages.Key.NotCallable);
                }
                mapper = (Callable) mapfn;
                mapping = true;
            } else {
                mapper = null;
                mapping = false;
            }
            /* step 5 (omitted) */
            /* step 6 */
            Callable usingIterator = GetMethod(cx, source, BuiltinSymbol.iterator.get());
            /* step 7 */
            if (usingIterator != null) {
                /* step 7.a */
                List<?> values = IterableToList(cx, source, usingIterator);
                /* step 7.b */
                int len = values.size();
                /* step 7.c */
                TypedArrayObject targetObj = TypedArrayCreate(cx, "%TypedArray%.from", (Constructor) c, len);
                /* steps 7.d-e */
                for (int k = 0; k < len; ++k) {
                    /* step 7.e.i */
                    int pk = k;
                    /* step 7.e.ii */
                    Object kValue = values.get(pk);
                    /* steps 7.e.iii-iv */
                    Object mappedValue;
                    if (mapping) {
                        mappedValue = mapper.call(cx, thisArg, kValue, k);
                    } else {
                        mappedValue = kValue;
                    }
                    /* step 7.e.v */
                    targetObj.elementSetMaybeDetached(cx, pk, mappedValue);
                }
                /* step 7.f (not applicable) */
                /* step 7.g */
                return targetObj;
            }
            /* step 8 (note) */
            /* step 9 */
            ScriptObject arrayLike = ToObject(cx, source);
            /* step 10 */
            long len = ToLength(cx, Get(cx, arrayLike, "length"));
            /* step 11 */
            TypedArrayObject targetObj = TypedArrayCreate(cx, "%TypedArray%.from", (Constructor) c, len);
            /* steps 12-13 */
            for (long k = 0; k < len; ++k) {
                /* step 13.a */
                long pk = k;
                /* step 13.b */
                Object kValue = Get(cx, arrayLike, pk);
                /* steps 13.c-d */
                Object mappedValue;
                if (mapping) {
                    mappedValue = mapper.call(cx, thisArg, kValue, k);
                } else {
                    mappedValue = kValue;
                }
                /* step 13.e */
                targetObj.elementSetMaybeDetached(cx, pk, mappedValue);
            }
            /* step 14 */
            return targetObj;
        }

        /**
         * 22.2.2.2 %TypedArray%.of ( ...items )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param items
         *            the element values
         * @return the new typed array object
         */
        @Function(name = "of", arity = 0)
        public static Object of(ExecutionContext cx, Object thisValue, Object... items) {
            /* steps 1-2 */
            int len = items.length;
            /* step 3 */
            Object c = thisValue;
            /* step 4 */
            if (!IsConstructor(c)) {
                throw newTypeError(cx, Messages.Key.NotConstructor);
            }
            /* step 5 */
            TypedArrayObject newObj = TypedArrayCreate(cx, "%TypedArray%.of", (Constructor) c, len);
            /* steps 6-7 */
            for (int k = 0; k < len; ++k) {
                /* steps 7.a-c */
                newObj.elementSetMaybeDetached(cx, k, items[k]);
            }
            /* step 8 */
            return newObj;
        }

        /**
         * 22.2.2.4 get %TypedArray% [ @@species ]
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the species object
         */
        @Accessor(name = "get [Symbol.species]", symbol = BuiltinSymbol.species, type = Accessor.Type.Getter)
        public static Object species(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            return thisValue;
        }
    }

    /**
     * 22.2.2.1.1 Runtime Semantics: IterableToList( items, method )
     * 
     * @param cx
     *            the execution context
     * @param items
     *            the items object
     * @param method
     *            the iterator method
     * @return the items list
     */
    public static List<?> IterableToList(ExecutionContext cx, Object items, Callable method) {
        if (items instanceof ArrayObject) {
            ArrayObject array = (ArrayObject) items;
            if (ScriptIterators.isBuiltinArrayIterator(cx, array, method)) {
                return Arrays.asList(array.toArray());
            }
        } else if (items instanceof TypedArrayObject) {
            TypedArrayObject array = (TypedArrayObject) items;
            if (ScriptIterators.isBuiltinTypedArrayIterator(cx, array, method)) {
                return array.toList();
            }
        }
        /* step 1 */
        ScriptIterator<?> iterator = GetIterator(cx, items, method);
        /* step 2 */
        ArrayList<Object> values = new ArrayList<>();
        /* steps 3-4 */
        iterator.forEachRemaining(values::add);
        /* step 5 */
        return values;
    }
}
