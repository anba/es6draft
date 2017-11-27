/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.iteration;

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
 * <h1>25 Control Abstraction Objects</h1><br>
 * <h2>25.2 GeneratorFunction Objects</h2>
 * <ul>
 * <li>25.2.1 The GeneratorFunction Constructor
 * <li>25.2.2 Properties of the GeneratorFunction Constructor
 * </ul>
 */
public final class GeneratorFunctionConstructor extends BuiltinConstructor implements Initializable {
    /**
     * Constructs a new Generator Function constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public GeneratorFunctionConstructor(Realm realm) {
        super(realm, "GeneratorFunction", 1);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * 25.2.1.1 GeneratorFunction (p1, p2, ... , pn, body)
     */
    @Override
    public FunctionObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        /* steps 1-3 */
        return CreateDynamicFunction(callerContext, calleeContext(), this, SourceKind.Generator, args);
    }

    /**
     * 25.2.1.1 GeneratorFunction (p1, p2, ... , pn, body)
     */
    @Override
    public FunctionObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
        /* steps 1-3 */
        return CreateDynamicFunction(callerContext, calleeContext(), newTarget, SourceKind.Generator, args);
    }

    /**
     * 25.2.2 Properties of the GeneratorFunction Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.Function;

        /**
         * 25.2.2.2 GeneratorFunction.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.Generator;

        /**
         * 25.2.2.1 GeneratorFunction.length
         */
        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = "GeneratorFunction";
    }
}
