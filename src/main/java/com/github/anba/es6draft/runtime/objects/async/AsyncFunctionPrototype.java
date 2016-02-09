/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.async;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>Extension: Async Function Definitions</h1><br>
 * <h2>Async Function Objects</h2>
 * <ul>
 * <li>Properties of the AsyncFunction Prototype Object
 * </ul>
 */
public final class AsyncFunctionPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new AsyncFunction prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public AsyncFunctionPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * Properties of the AsyncFunction Prototype Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        /**
         * AsyncFunction.prototype.constructor
         */
        @Value(name = "constructor",
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final Intrinsics constructor = Intrinsics.AsyncFunction;

        /**
         * AsyncFunction.prototype[@@toStringTag]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "AsyncFunction";
    }
}
