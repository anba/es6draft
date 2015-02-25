/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.collection;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.GetIterator;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsCallable;
import static com.github.anba.es6draft.runtime.AbstractOperations.IteratorClose;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.internal.ListIterator.FromScriptIterator;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
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
    }

    @Override
    public WeakSetConstructor clone() {
        return new WeakSetConstructor(getRealm());
    }

    /**
     * 23.4.1.1 WeakSet ([ iterable ])
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        /* step 1 */
        throw newTypeError(calleeContext, Messages.Key.InvalidCall, "WeakSet");
    }

    /**
     * 23.4.1.1 WeakSet ([ iterable ])
     */
    @Override
    public WeakSetObject construct(ExecutionContext callerContext, Constructor newTarget,
            Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object iterable = argument(args, 0);

        /* step 1 (not applicable) */
        /* steps 2-4 */
        WeakSetObject set = OrdinaryCreateFromConstructor(calleeContext, newTarget,
                Intrinsics.WeakSetPrototype, WeakSetObjectAllocator.INSTANCE);

        /* steps 5-7 */
        ScriptIterator<?> iter;
        Callable adder = null;
        if (Type.isUndefinedOrNull(iterable)) {
            iter = null;
        } else {
            Object _adder = Get(calleeContext, set, "add");
            if (!IsCallable(_adder)) {
                throw newTypeError(calleeContext, Messages.Key.PropertyNotCallable, "add");
            }
            adder = (Callable) _adder;
            iter = FromScriptIterator(calleeContext, GetIterator(calleeContext, iterable));
        }

        /* step 8 */
        if (iter == null) {
            return set;
        }
        /* step 9 */
        try {
            while (iter.hasNext()) {
                Object nextValue = iter.next();
                adder.call(calleeContext, set, nextValue);
            }
            return set;
        } catch (ScriptException e) {
            IteratorClose(calleeContext, iter, true);
            throw e;
        }
    }

    private static final class WeakSetObjectAllocator implements ObjectAllocator<WeakSetObject> {
        static final ObjectAllocator<WeakSetObject> INSTANCE = new WeakSetObjectAllocator();

        @Override
        public WeakSetObject newInstance(Realm realm) {
            return new WeakSetObject(realm);
        }
    }

    /**
     * 23.4.2 Properties of the WeakSet Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 0;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = "WeakSet";

        /**
         * 23.4.2.1 WeakSet.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.WeakSetPrototype;
    }
}
