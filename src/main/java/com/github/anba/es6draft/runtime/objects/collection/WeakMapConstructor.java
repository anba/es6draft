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
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.ScriptIterator;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;

/**
 * <h1>23 Keyed Collection</h1><br>
 * <h2>23.3 WeakMap Objects</h2>
 * <ul>
 * <li>23.3.1 The WeakMap Constructor
 * <li>23.3.2 Properties of the WeakMap Constructor
 * </ul>
 */
public final class WeakMapConstructor extends BuiltinConstructor implements Initializable {
    /**
     * Constructs a new WeakMap constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public WeakMapConstructor(Realm realm) {
        super(realm, "WeakMap", 0);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    @Override
    public WeakMapConstructor clone() {
        return new WeakMapConstructor(getRealm());
    }

    /**
     * 23.3.1.1 WeakMap ([ iterable ])
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        /* step 1 */
        throw newTypeError(calleeContext(), Messages.Key.InvalidCall, "WeakMap");
    }

    /**
     * 23.3.1.1 WeakMap ([ iterable ])
     */
    @Override
    public WeakMapObject construct(ExecutionContext callerContext, Constructor newTarget,
            Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object iterable = argument(args, 0);

        /* step 1 (not applicable) */
        /* steps 2-4 */
        WeakMapObject map = OrdinaryCreateFromConstructor(calleeContext, newTarget,
                Intrinsics.WeakMapPrototype, WeakMapObjectAllocator.INSTANCE);
        /* steps 5-6, 8 */
        if (Type.isUndefinedOrNull(iterable)) {
            return map;
        }
        /* step 7 */
        Object _adder = Get(calleeContext, map, "set");
        if (!IsCallable(_adder)) {
            throw newTypeError(calleeContext, Messages.Key.PropertyNotCallable, "set");
        }
        Callable adder = (Callable) _adder;
        ScriptIterator<?> iter = GetScriptIterator(calleeContext, iterable);
        /* step 9 */
        try {
            while (iter.hasNext()) {
                Object nextItem = iter.next();
                if (!Type.isObject(nextItem)) {
                    throw newTypeError(calleeContext, Messages.Key.NotObjectType);
                }
                ScriptObject item = Type.objectValue(nextItem);
                Object k = Get(calleeContext, item, 0);
                Object v = Get(calleeContext, item, 1);
                adder.call(calleeContext, map, k, v);
            }
            return map;
        } catch (ScriptException e) {
            iter.close(e);
            throw e;
        }
    }

    private static final class WeakMapObjectAllocator implements ObjectAllocator<WeakMapObject> {
        static final ObjectAllocator<WeakMapObject> INSTANCE = new WeakMapObjectAllocator();

        @Override
        public WeakMapObject newInstance(Realm realm) {
            return new WeakMapObject(realm);
        }
    }

    /**
     * 23.3.2 Properties of the WeakMap Constructor
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
        public static final String name = "WeakMap";

        /**
         * 23.3.2.1 WeakMap.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.WeakMapPrototype;
    }
}
