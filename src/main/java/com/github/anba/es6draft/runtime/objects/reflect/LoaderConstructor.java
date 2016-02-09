/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.reflect;

import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;

/**
 * <h1>26 Reflection</h1><br>
 * <h2>26.? Loader Objects</h2>
 * <ul>
 * <li>26.?.1 The Reflect.Loader Constructor
 * <li>26.?.2 Properties of the Reflect.Loader Constructor
 * </ul>
 */
public final class LoaderConstructor extends BuiltinConstructor implements Initializable {
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
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    @Override
    public LoaderConstructor clone() {
        return new LoaderConstructor(getRealm());
    }

    /**
     * 26.?.1.1 Reflect.Loader (options = { })
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        throw newTypeError(calleeContext(), Messages.Key.InvalidCall, "Loader");
    }

    /**
     * 26.?.1.1 Reflect.Loader (options = { })
     */
    @Override
    public LoaderObject construct(ExecutionContext callerContext, Constructor newTarget,
            Object... args) {
        throw newTypeError(calleeContext(), Messages.Key.IncompatibleObject);
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
}
