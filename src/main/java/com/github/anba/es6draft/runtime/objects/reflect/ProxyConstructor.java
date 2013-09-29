/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.reflect;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticProxy.CreateProxy;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.github.anba.es6draft.runtime.types.builtins.ExoticProxy;

/**
 * <h1>26 The Reflect Module</h1><br>
 * <h2>26.2 Proxy Objects</h2>
 */
public class ProxyConstructor extends BuiltinConstructor implements Initialisable {
    public ProxyConstructor(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    @Override
    public ExoticProxy call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object target = args.length > 0 ? args[0] : UNDEFINED;
        Object handler = args.length > 1 ? args[1] : UNDEFINED;
        return CreateProxy(calleeContext, target, handler);
    }

    @Override
    public ExoticProxy construct(ExecutionContext callerContext, Object... args) {
        return call(callerContext, null, args);
    }

    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 2;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final String name = "Proxy";

        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = "Proxy";
    }
}
