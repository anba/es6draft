/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.collection;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.GetIterator;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsCallable;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.collection.CollectionAbstractOperations.CollectionCreate;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.CompatibilityExtension;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.ScriptIterator;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;

/**
 * <h1>23 Keyed Collection</h1><br>
 * <h2>23.4 WeakSet Objects</h2>
 * <ul>
 * <li>23.4.1 The WeakSet Constructor
 * <li>23.4.2 Properties of the WeakSet Constructor
 * </ul>
 */
public final class WeakSetConstructor extends BuiltinConstructor implements Initializable {
    /**
     * Constructs a new WeakSet constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public WeakSetConstructor(Realm realm) {
        super(realm, "WeakSet", 0);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
        createProperties(realm, this, OfAndFromProperties.class);
    }

    /**
     * 23.4.1.1 WeakSet ([ iterable ])
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        /* step 1 */
        throw newTypeError(calleeContext(), Messages.Key.InvalidCall, "WeakSet");
    }

    /**
     * 23.4.1.1 WeakSet ([ iterable ])
     */
    @Override
    public WeakSetObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object iterable = argument(args, 0);

        /* step 1 (not applicable) */
        /* steps 2-3 */
        WeakSetObject set = OrdinaryCreateFromConstructor(calleeContext, newTarget, Intrinsics.WeakSetPrototype,
                WeakSetObject::new);
        /* steps 4-5, 7 */
        if (Type.isUndefinedOrNull(iterable)) {
            return set;
        }
        /* step 6 */
        Object _adder = Get(calleeContext, set, "add");
        if (!IsCallable(_adder)) {
            throw newTypeError(calleeContext, Messages.Key.PropertyNotCallable, "add");
        }
        Callable adder = (Callable) _adder;
        boolean isBuiltin = WeakSetPrototype.isBuiltinAdd(adder);
        ScriptIterator<?> iter = GetIterator(calleeContext, iterable);
        /* step 8 */
        try {
            if (isBuiltin) {
                iter.forEachRemaining(nextValue -> {
                    if (!Type.isObject(nextValue)) {
                        throw newTypeError(calleeContext, Messages.Key.WeakSetKeyNotObject);
                    }
                    set.getWeakSetData().put(Type.objectValue(nextValue), Boolean.TRUE);
                });
            } else {
                while (iter.hasNext()) {
                    /* steps 8.a-c */
                    Object nextValue = iter.next();
                    /* steps 8.d-e */
                    adder.call(calleeContext, set, nextValue);
                }
            }
            return set;
        } catch (ScriptException e) {
            iter.close(e);
            throw e;
        }
    }

    /**
     * 23.4.2 Properties of the WeakSet Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 0;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = "WeakSet";

        /**
         * 23.4.2.1 WeakSet.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.WeakSetPrototype;
    }

    /**
     * Properties of the WeakSet Constructor
     */
    @CompatibilityExtension(CompatibilityOption.CollectionsOfAndFrom)
    public enum OfAndFromProperties {
        ;

        /**
         * WeakSet.of ( ...items )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param items
         *            the element values
         * @return the new WeakSet object
         */
        @Function(name = "of", arity = 1)
        public static Object of(ExecutionContext cx, Object thisValue, Object... items) {
            /* steps 1-4 */
            return CollectionCreate(cx, thisValue, items);
        }

        /**
         * WeakSet.from ( source [ , mapFn [ , thisArg ] ] )
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
         * @return the new WeakSet object
         */
        @Function(name = "from", arity = 1)
        public static Object from(ExecutionContext cx, Object thisValue, Object source, Object mapfn, Object thisArg) {
            /* steps 1-2 */
            return CollectionCreate(cx, thisValue, source, mapfn, thisArg);
        }
    }
}
