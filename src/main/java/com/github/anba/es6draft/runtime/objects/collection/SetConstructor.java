/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.collection;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.GetScriptIterator;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsCallable;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.ScriptIterator;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;

/**
 * <h1>23 Keyed Collection</h1><br>
 * <h2>23.2 Set Objects</h2>
 * <ul>
 * <li>23.2.1 The Set Constructor
 * <li>23.2.2 Properties of the Set Constructor
 * </ul>
 */
public final class SetConstructor extends BuiltinConstructor implements Initializable {
    /**
     * Constructs a new Set constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public SetConstructor(Realm realm) {
        super(realm, "Set", 0);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    @Override
    public SetConstructor clone() {
        return new SetConstructor(getRealm());
    }

    /**
     * 23.2.1.1 Set ([ iterable ])
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        /* step 1 */
        throw newTypeError(calleeContext, Messages.Key.InvalidCall, "Set");
    }

    /**
     * 23.2.1.1 Set ([ iterable ])
     */
    @Override
    public SetObject construct(ExecutionContext callerContext, Constructor newTarget,
            Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object iterable = argument(args, 0);

        /* step 1 (not applicable) */
        /* steps 2-4 */
        SetObject set = OrdinaryCreateFromConstructor(calleeContext, newTarget,
                Intrinsics.SetPrototype, SetObjectAllocator.INSTANCE);

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
            iter = GetScriptIterator(calleeContext, iterable);
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
            iter.close(e);
            throw e;
        }
    }

    private static final class SetObjectAllocator implements ObjectAllocator<SetObject> {
        static final ObjectAllocator<SetObject> INSTANCE = new SetObjectAllocator();

        @Override
        public SetObject newInstance(Realm realm) {
            return new SetObject(realm);
        }
    }

    /**
     * 23.2.2 Properties of the Set Constructor
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
        public static final String name = "Set";

        /**
         * 23.2.2.1 Set.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.SetPrototype;

        /**
         * 23.2.2.2 get Set [ @@species ]
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the species object
         */
        @Accessor(name = "get [Symbol.species]", symbol = BuiltinSymbol.species,
                type = Accessor.Type.Getter)
        public static Object species(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            return thisValue;
        }
    }
}
