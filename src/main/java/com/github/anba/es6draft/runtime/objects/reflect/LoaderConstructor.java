/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.reflect;

import static com.github.anba.es6draft.runtime.AbstractOperations.Construct;
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
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Creatable;
import com.github.anba.es6draft.runtime.types.CreateAction;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;

/**
 * <h1>26 Reflection</h1><br>
 * <h2>26.? Loader Objects</h2>
 * <ul>
 * <li>26.?.1 The Reflect.Loader Constructor
 * <li>26.?.2 Properties of the Reflect.Loader Constructor
 * </ul>
 */
public final class LoaderConstructor extends BuiltinConstructor implements Initializable,
        Creatable<LoaderObject> {
    /**
     * Constructs a new Loader constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public LoaderConstructor(Realm realm) {
        super(realm, "Loader", 0);
    }

    @Override
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
    }

    @Override
    public LoaderConstructor clone() {
        return new LoaderConstructor(getRealm());
    }

    /**
     * 26.?.1.1 Reflect.Loader (options = { })
     */
    @Override
    public LoaderObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        throw newTypeError(calleeContext, Messages.Key.IncompatibleObject);
    }

    /**
     * 26.?.1.2 new Reflect.Loader ( ... argumentsList )
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return Construct(callerContext, this, args);
    }

    @Override
    public CreateAction<LoaderObject> createAction() {
        return LoaderCreate.INSTANCE;
    }

    /**
     * 26.?.2 Properties of the Reflect.Loader Constructor
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
        public static final String name = "Loader";

        /**
         * 26.?.2.1 Reflect.Loader.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.LoaderPrototype;
    }

    private static final class LoaderObjectAllocator implements ObjectAllocator<LoaderObject> {
        static final ObjectAllocator<LoaderObject> INSTANCE = new LoaderObjectAllocator();

        @Override
        public LoaderObject newInstance(Realm realm) {
            return new LoaderObject(realm);
        }
    }

    private static final class LoaderCreate implements CreateAction<LoaderObject> {
        static final CreateAction<LoaderObject> INSTANCE = new LoaderCreate();

        @Override
        public LoaderObject create(ExecutionContext cx, Constructor constructor, Object... args) {
            return OrdinaryCreateFromConstructor(cx, constructor, Intrinsics.LoaderPrototype,
                    LoaderObjectAllocator.INSTANCE);
        }
    }
}
