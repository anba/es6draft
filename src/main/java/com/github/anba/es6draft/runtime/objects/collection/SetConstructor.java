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
 * <h2>23.2 Set Objects</h2>
 * <ul>
 * <li>23.2.1 The Set Constructor
 * <li>23.2.2 Properties of the Set Constructor
 * </ul>
 */
public final class SetConstructor extends BuiltinConstructor implements Initializable,
        Creatable<SetObject> {
    /**
     * Constructs a new Set constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public SetConstructor(Realm realm) {
        super(realm, "Set", 1);
    }

    @Override
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
    }

    @Override
    public SetConstructor clone() {
        return new SetConstructor(getRealm());
    }

    /**
     * 23.2.1.1 Set ([ iterable ])
     */
    @Override
    public SetObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object iterable = argument(args, 0);

        /* steps 1-4 */
        if (!Type.isObject(thisValue)) {
            throw newTypeError(calleeContext, Messages.Key.NotObjectType);
        }
        if (!(thisValue instanceof SetObject)) {
            throw newTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        SetObject set = (SetObject) thisValue;
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
            iter = GetIterator(calleeContext, ToObject(calleeContext, iterable));
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
     * 23.2.1.2 new Set (...argumentsList)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return Construct(callerContext, this, args);
    }

    @Override
    public CreateAction<SetObject> createAction() {
        return SetCreate.INSTANCE;
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
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = "Set";

        /**
         * 23.2.2.1 Set.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.SetPrototype;
    }

    private static final class SetObjectAllocator implements ObjectAllocator<SetObject> {
        static final ObjectAllocator<SetObject> INSTANCE = new SetObjectAllocator();

        @Override
        public SetObject newInstance(Realm realm) {
            return new SetObject(realm);
        }
    }

    private static class SetCreate implements CreateAction<SetObject> {
        static final CreateAction<SetObject> INSTANCE = new SetCreate();

        @Override
        public SetObject create(ExecutionContext cx, Constructor constructor, Object... args) {
            return OrdinaryCreateFromConstructor(cx, constructor, Intrinsics.SetPrototype,
                    SetObjectAllocator.INSTANCE);
        }
    }
}
