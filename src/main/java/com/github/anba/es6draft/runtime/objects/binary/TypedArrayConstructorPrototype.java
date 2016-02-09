/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.binary.TypedArrayConstructor.TypedArrayCreate;

import java.util.AbstractList;
import java.util.ArrayList;
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
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
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

    @Override
    public TypedArrayConstructorPrototype clone() {
        return new TypedArrayConstructorPrototype(getRealm());
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
        ExecutionContext calleeContext = calleeContext();
        /* step 1 (not applicable) */
        /* step 2 (omitted) */
        /* step 3 */
        if (newTarget == this) {
            throw newTypeError(calleeContext, Messages.Key.TypedArrayCreate);
        }
        /* step 4 */
        ScriptObject super_ = getPrototypeOf(calleeContext);
        /* step 5 */
        if (!IsConstructor(super_)) {
            throw newTypeError(calleeContext, Messages.Key.NotConstructor);
        }
        /* steps 6-7 */
        return ((Constructor) super_).construct(calleeContext, newTarget, args);
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
        @Value(name = "prototype",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
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
            List<Object> arrayLike = IterableToArrayLike(cx, source);
            /* step 7 */
            int len = arrayLike.size();
            /* step 8 */
            TypedArrayObject targetObj = TypedArrayCreate(cx, (Constructor) c, len);
            /* steps 9-10 */
            for (int k = 0; k < len; ++k) {
                /* step 10.a */
                int pk = k;
                /* step 10.b */
                Object kValue = arrayLike.get(pk);
                /* steps 10.c-d */
                Object mappedValue;
                if (mapping) {
                    mappedValue = mapper.call(cx, thisArg, kValue, k);
                } else {
                    mappedValue = kValue;
                }
                /* step 10.e */
                targetObj.elementSetDirect(cx, pk, ToNumber(cx, mappedValue));
            }
            /* step 11 */
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
            TypedArrayObject newObj = TypedArrayCreate(cx, (Constructor) c, len);
            /* steps 6-7 */
            for (int k = 0; k < len; ++k) {
                /* step 7.a */
                Object value = items[k];
                /* step 7.b */
                int pk = k;
                /* step 7.c */
                newObj.elementSetDirect(cx, pk, ToNumber(cx, value));
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
     * 22.2.2.1.1 Runtime Semantics: IterableToArrayLike( items )
     * 
     * @param cx
     *            the execution context
     * @param items
     *            the items object
     * @return the items list
     */
    public static List<Object> IterableToArrayLike(ExecutionContext cx, Object items) {
        /* step 1 */
        Callable usingIterator = GetMethod(cx, items, BuiltinSymbol.iterator.get());
        /* step 2 */
        if (usingIterator != null) {
            /* step 2.a */
            ScriptIterator<?> iterator = GetScriptIterator(cx, items, usingIterator);
            /* step 2.b */
            ArrayList<Object> values = new ArrayList<>();
            /* steps 2.c-d */
            while (iterator.hasNext()) {
                Object nextValue = iterator.next();
                values.add(nextValue);
            }
            /* step 2.e */
            return values;
        }
        /* step 3 (note) */
        /* step 4 */
        return new ScriptArrayList(cx, ToObject(cx, items));
    }

    private final static class ScriptArrayList extends AbstractList<Object> {
        private final ExecutionContext cx;
        private final ScriptObject arrayLike;
        private final long length;

        ScriptArrayList(ExecutionContext cx, ScriptObject arrayLike) {
            this.cx = cx;
            this.arrayLike = arrayLike;
            this.length = ToLength(cx, Get(cx, arrayLike, "length"));
        }

        @Override
        public int size() {
            return (int) Math.min(length, Integer.MAX_VALUE);
        }

        @Override
        public Object get(int index) {
            if (index < 0 || index >= length) {
                throw new IndexOutOfBoundsException();
            }
            return Get(cx, arrayLike, index);
        }
    }
}
