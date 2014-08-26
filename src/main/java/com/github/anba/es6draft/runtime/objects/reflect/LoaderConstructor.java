/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.reflect;

import static com.github.anba.es6draft.runtime.AbstractOperations.Construct;
import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataPropertyOrThrow;
import static com.github.anba.es6draft.runtime.AbstractOperations.GetOption;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsCallable;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.modules.Loader.CreateLoader;
import static java.util.Arrays.asList;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.modules.Loader;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;

/**
 * <h1>26 Reflection</h1><br>
 * <h2>26.3 Loader Objects</h2>
 * <ul>
 * <li>26.3.1 The Reflect.Loader Constructor
 * <li>26.3.2 Properties of the Reflect.Loader Constructor
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
        super(realm, "Loader");
    }

    @Override
    public void initialize(ExecutionContext cx) {
        addRestrictedFunctionProperties(cx);
        createProperties(cx, this, Properties.class);
    }

    @Override
    public LoaderConstructor clone() {
        return new LoaderConstructor(getRealm());
    }

    /**
     * 26.3.1.1 Reflect.Loader (options = { })
     */
    @Override
    public LoaderObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object options = argument(args, 0);
        /* steps 2-3 */
        if (!(thisValue instanceof LoaderObject)) {
            throw newTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        /* step 1 */
        LoaderObject loader = (LoaderObject) thisValue;
        /* step 4 */
        if (loader.getLoader() != null) {
            throw newTypeError(calleeContext, Messages.Key.InitializedObject);
        }
        /* steps 5-6 */
        Object realmObject = GetOption(calleeContext, options, "realm");
        /* steps 7-8 */
        Realm realm;
        if (Type.isUndefined(realmObject)) {
            realm = calleeContext.getRealm();
        } else if (!(realmObject instanceof RealmObject)) {
            throw newTypeError(calleeContext, Messages.Key.IncompatibleObject);
        } else {
            realm = ((RealmObject) realmObject).getRealm();
            if (realm == null) {
                throw newTypeError(calleeContext, Messages.Key.UninitializedObject);
            }
        }
        /* step 9 */
        for (String name : asList("normalize", "locate", "fetch", "translate", "instantiate")) {
            Object hook = GetOption(calleeContext, options, name);
            if (!Type.isUndefined(hook)) {
                if (!IsCallable(hook)) {
                    throw newTypeError(calleeContext, Messages.Key.PropertyNotCallable, name);
                }
                CreateDataPropertyOrThrow(calleeContext, loader, name, hook);
            }
        }
        /* steps 10-11 */
        if (loader.getLoader() != null) {
            throw newTypeError(calleeContext, Messages.Key.InitializedObject);
        }
        /* step 12 */
        Loader loaderRecord = CreateLoader(realm, loader);
        /* step 13 */
        loader.setLoader(loaderRecord);
        /* step 14 */
        return loader;
    }

    /**
     * 26.3.1.2 new Reflect.Loader ( ... argumentsList )
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return Construct(callerContext, this, args);
    }

    /**
     * 26.3.2 Properties of the Reflect.Loader Constructor
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
         * 26.3.2.1 Reflect.Loader.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.LoaderPrototype;

        /**
         * 26.3.2.2 Reflect.Loader [ @@create ] ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the new uninitialized loader object
         */
        @Function(name = "[Symbol.create]", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.LoaderPrototype,
                    LoaderObjectAllocator.INSTANCE);
        }
    }

    private static final class LoaderObjectAllocator implements ObjectAllocator<LoaderObject> {
        static final ObjectAllocator<LoaderObject> INSTANCE = new LoaderObjectAllocator();

        @Override
        public LoaderObject newInstance(Realm realm) {
            return new LoaderObject(realm);
        }
    }
}
