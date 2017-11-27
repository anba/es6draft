/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.async.iteration;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.FunctionConstructor.CreateDynamicFunction;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.FunctionConstructor.SourceKind;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;

/**
 * <h1>Async Generator Functions</h1><br>
 * <h2>AsyncGeneratorFunction Objects</h2>
 * <ul>
 * <li>The AsyncGeneratorFunction Constructor
 * <li>Properties of the AsyncGeneratorFunction Constructor
 * </ul>
 */
public final class AsyncGeneratorFunctionConstructor extends BuiltinConstructor implements Initializable {
    /**
     * Constructs a new AsyncGeneratorFunction constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public AsyncGeneratorFunctionConstructor(Realm realm) {
        super(realm, "AsyncGeneratorFunction", 1);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * AsyncGeneratorFunction (p1, p2, ... , pn, body)
     */
    @Override
    public FunctionObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        /* steps 1-3 */
        return CreateDynamicFunction(callerContext, calleeContext(), this, SourceKind.AsyncGenerator, args);
    }

    /**
     * AsyncGeneratorFunction (p1, p2, ... , pn, body)
     */
    @Override
    public FunctionObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
        /* steps 1-3 */
        return CreateDynamicFunction(callerContext, calleeContext(), newTarget, SourceKind.AsyncGenerator, args);
    }

    /**
     * Properties of the AsyncGeneratorFunction Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.Function;

        /**
         * AsyncGeneratorFunction.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.AsyncGenerator;

        /**
         * AsyncGeneratorFunction.length
         */
        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = "AsyncGeneratorFunction";
    }
}
