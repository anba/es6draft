/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.Construct;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToBoolean;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Creatable;
import com.github.anba.es6draft.runtime.types.CreateAction;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;

/**
 * <h1>19 Fundamental Objects</h1><br>
 * <h2>19.3 Boolean Objects</h2>
 * <ul>
 * <li>19.3.1 The Boolean Constructor
 * <li>19.3.2 Properties of the Boolean Constructor
 * </ul>
 */
public final class BooleanConstructor extends BuiltinConstructor implements Initializable,
        Creatable<BooleanObject> {
    /**
     * Constructs a new Boolean constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public BooleanConstructor(Realm realm) {
        super(realm, "Boolean");
    }

    @Override
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
    }

    @Override
    public BooleanConstructor clone() {
        return new BooleanConstructor(getRealm());
    }

    /**
     * 19.3.1.1 Boolean (value)
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        /* step 1 (omitted) */
        /* step 2 */
        boolean b = args.length > 0 ? ToBoolean(args[0]) : false;
        /* step 3 */
        if (thisValue instanceof BooleanObject) {
            BooleanObject obj = (BooleanObject) thisValue;
            if (!obj.isInitialized()) {
                obj.setBooleanData(b);
                return obj;
            }
        }
        /* step 4 */
        return b;
    }

    /**
     * 19.3.1.2 new Boolean (... argumentsList)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return Construct(callerContext, this, args);
    }

    @Override
    public CreateAction<BooleanObject> createAction() {
        return BooleanCreate.INSTANCE;
    }

    /**
     * 19.3.2 Properties of the Boolean Constructor
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
        public static final String name = "Boolean";

        /**
         * 19.3.2.1 Boolean.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.BooleanPrototype;
    }

    private static final class BooleanObjectAllocator implements ObjectAllocator<BooleanObject> {
        static final ObjectAllocator<BooleanObject> INSTANCE = new BooleanObjectAllocator();

        @Override
        public BooleanObject newInstance(Realm realm) {
            return new BooleanObject(realm);
        }
    }

    private static final class BooleanCreate implements CreateAction<BooleanObject> {
        static final CreateAction<BooleanObject> INSTANCE = new BooleanCreate();

        @Override
        public BooleanObject create(ExecutionContext cx, Constructor constructor, Object... args) {
            return OrdinaryCreateFromConstructor(cx, constructor, Intrinsics.BooleanPrototype,
                    BooleanObjectAllocator.INSTANCE);
        }
    }
}
