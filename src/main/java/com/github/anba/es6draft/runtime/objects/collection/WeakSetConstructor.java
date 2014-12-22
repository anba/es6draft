/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.collection;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
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
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Creatable;
import com.github.anba.es6draft.runtime.types.CreateAction;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
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
public final class WeakSetConstructor extends BuiltinConstructor implements Initializable,
        Creatable<WeakSetObject> {
    /**
     * Constructs a new WeakSet constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public WeakSetConstructor(Realm realm) {
        super(realm, "WeakSet", 1);
    }

    @Override
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
    }

    @Override
    public WeakSetConstructor clone() {
        return new WeakSetConstructor(getRealm());
    }

    /**
     * 23.4.1.1 WeakSet ([ iterable ])
     */
    @Override
    public WeakSetObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object iterable = argument(args, 0);

        /* steps 1-4 */
        if (!Type.isObject(thisValue)) {
            throw newTypeError(calleeContext, Messages.Key.NotObjectType);
        }
        if (!(thisValue instanceof WeakSetObject)) {
            throw newTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        WeakSetObject set = (WeakSetObject) thisValue;
        if (set.isInitialized()) {
            throw newTypeError(calleeContext, Messages.Key.InitializedObject);
        }

        /* steps 5-7 */
        ScriptObject iter;
        Callable adder = null;
        if (Type.isUndefinedOrNull(iterable)) {
            iter = null;
        } else {
            Object _adder = Get(calleeContext, set, "add");
            if (!IsCallable(_adder)) {
                throw newTypeError(calleeContext, Messages.Key.PropertyNotCallable, "add");
            }
            adder = (Callable) _adder;
            iter = GetIterator(calleeContext, iterable);
        }

        /* steps 8-9 */
        if (set.isInitialized()) {
            throw newTypeError(calleeContext, Messages.Key.InitializedObject);
        }

        /* step 10 */
        set.initialize();

        /* step 11 */
        if (iter == null) {
            return set;
        }
        /* step 12 */
        for (;;) {
            ScriptObject next = IteratorStep(calleeContext, iter);
            if (next == null) {
                return set;
            }
            Object nextValue = IteratorValue(calleeContext, next);
            adder.call(calleeContext, set, nextValue);
        }
    }

    /**
     * 23.4.1.2 new WeakSet (...argumentsList)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return Construct(callerContext, this, args);
    }

    @Override
    public CreateAction<WeakSetObject> createAction() {
        return WeakSetCreate.INSTANCE;
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
        public static final int length = 1;

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

    private static final class WeakSetObjectAllocator implements ObjectAllocator<WeakSetObject> {
        static final ObjectAllocator<WeakSetObject> INSTANCE = new WeakSetObjectAllocator();

        @Override
        public WeakSetObject newInstance(Realm realm) {
            return new WeakSetObject(realm);
        }
    }

    private static class WeakSetCreate implements CreateAction<WeakSetObject> {
        static final CreateAction<WeakSetObject> INSTANCE = new WeakSetCreate();

        @Override
        public WeakSetObject create(ExecutionContext cx, Constructor constructor, Object... args) {
            return OrdinaryCreateFromConstructor(cx, constructor, Intrinsics.WeakSetPrototype,
                    WeakSetObjectAllocator.INSTANCE);
        }
    }
}
