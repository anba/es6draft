/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.modules;

import static com.github.anba.es6draft.runtime.AbstractOperations.DefinePropertyOrThrow;
import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.GetOwnEnumerablePropertyNames;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.modules.ModuleAbstractOperations.CreateLinkedModuleInstance;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.OrdinaryConstruct;

import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.modules.ModuleObject;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;

/**
 * <h1>1 Modules: Semantics</h1><br>
 * <h2>1.4 Module Objects</h2>
 * <ul>
 * <li>1.4.1 The Module Factory Function
 * </ul>
 */
public class ModuleFactoryFunction extends BuiltinConstructor implements Initialisable {
    public ModuleFactoryFunction(Realm realm) {
        super(realm, "Module");
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    /**
     * 1.4.1.1 Constant Functions
     */
    public static class ConstantFunction extends BuiltinFunction {
        /** [[ConstantValue]] */
        private final Object constantValue;

        public ConstantFunction(Realm realm, String name, Object constantValue) {
            super(realm, name, 0);
            this.constantValue = constantValue;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            return constantValue;
        }
    }

    /**
     * 1.4.1.2 CreateConstantGetter(key, value) Abstract Operation
     */
    public static Callable CreateConstantGetter(ExecutionContext cx, String key, Object value) {
        /* steps 1-3 */
        return new ConstantFunction(cx.getRealm(), "get " + key, value);
    }

    /**
     * 1.4.1.3 Module ( obj )
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object obj = args.length > 0 ? args[0] : UNDEFINED;
        /* step 1 */
        if (!Type.isObject(obj)) {
            throw newTypeError(calleeContext, Messages.Key.NotObjectType);
        }
        ScriptObject object = Type.objectValue(obj);
        /* step 2 */
        ModuleObject mod = CreateLinkedModuleInstance(calleeContext);
        /* steps 3-4 */
        List<String> keys = GetOwnEnumerablePropertyNames(calleeContext, object);
        /* step 5 */
        for (String key : keys) {
            Object value = Get(calleeContext, object, key);
            Callable f = CreateConstantGetter(calleeContext, key, value);
            PropertyDescriptor desc = new PropertyDescriptor(f, null, true, false);
            DefinePropertyOrThrow(calleeContext, mod, key, desc);
        }
        /* step 6 */
        mod.preventExtensions(calleeContext);
        /* step 7 */
        return mod;
    }

    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        // FIXME: tests assume that Module has [[Construct]]
        return OrdinaryConstruct(callerContext, this, args);
    }

    /**
     * Properties of the Module Factory Function
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
        public static final String name = "Module";

        /**
         * 1.4.1.4 Module.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = null;
    }
}
