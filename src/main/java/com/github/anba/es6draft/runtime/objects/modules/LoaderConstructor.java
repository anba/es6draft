/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.modules;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.OrdinaryCreateFromConstructor;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.OrdinaryConstruct;
import static java.util.Arrays.asList;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;

/**
 * <h1>1 Modules: Semantics</h1><br>
 * <h2>1.6 Loader Objects</h2>
 * <ul>
 * <li>1.6.1 GetOption(options, name) Abstract Operation
 * <li>1.6.2 The Loader Constructor
 * </ul>
 */
public class LoaderConstructor extends BuiltinConstructor implements Initialisable {
    public LoaderConstructor(Realm realm) {
        super(realm, "Loader");
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    /**
     * 1.6.1 GetOption(options, name) Abstract Operation
     */
    public static Object GetOption(ExecutionContext cx, Object options, String name) {
        /* step 1 */
        if (Type.isUndefined(options)) {
            return UNDEFINED;
        }
        /* step 2 */
        if (!Type.isObject(options)) {
            throw throwTypeError(cx, Messages.Key.NotObjectType);
        }
        /* step 3 */
        return Get(cx, Type.objectValue(options), name);
    }

    /**
     * 1.6.2.1 Loader ( options )
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object options = args.length > 0 ? args[0] : UNDEFINED;
        /* steps 2-3 */
        if (!(thisValue instanceof LoaderObject)) {
            throw throwTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        /* step 1 */
        LoaderObject loader = (LoaderObject) thisValue;
        /* step 4 */
        if (loader.getModules() != null) {
            throw throwTypeError(calleeContext, Messages.Key.InitialisedObject);
        }
        // FIXME: tests assume options gets defaulted to empty object if undefined
        if (Type.isUndefined(options)) {
            options = ObjectCreate(calleeContext, (ScriptObject) null);
        }
        /* step 5 */
        if (!Type.isObject(options)) {
            throw throwTypeError(calleeContext, Messages.Key.NotObjectType);
        }
        ScriptObject opts = Type.objectValue(options);
        /* steps 6-7 */
        Object realmObject = Get(calleeContext, opts, "realm");
        /* steps 8-10 */
        Realm realm;
        if (Type.isUndefined(realmObject)) {
            realm = calleeContext.getRealm();
        } else if (!(realmObject instanceof RealmObject)) {
            throw throwTypeError(calleeContext, Messages.Key.IncompatibleObject);
        } else {
            realm = ((RealmObject) realmObject).getRealm();
            if (realm == null) {
                throw throwTypeError(calleeContext, Messages.Key.UninitialisedObject);
            }
        }
        /* step 11 */
        for (String name : asList("normalize", "locate", "fetch", "translate", "instantiate")) {
            Object hook = Get(calleeContext, opts, name);
            if (!Type.isUndefined(hook)) {
                CreateDataProperty(calleeContext, loader, name, hook);
            }
        }
        /* steps 12-14 */
        loader.initialise(realm);
        /* step 15 */
        return loader;
    }

    /**
     * new Loader (... argumentsList)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return OrdinaryConstruct(callerContext, this, args);
    }

    /**
     * Properties of the Loader Constructor
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
        public static final String name = "Loader";

        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.LoaderPrototype;

        @Function(name = "[Symbol.create]", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.LoaderPrototype,
                    LoaderObjectAllocator.INSTANCE);
        }
    }

    private static class LoaderObjectAllocator implements ObjectAllocator<LoaderObject> {
        static final ObjectAllocator<LoaderObject> INSTANCE = new LoaderObjectAllocator();

        @Override
        public LoaderObject newInstance(Realm realm) {
            return new LoaderObject(realm);
        }
    }
}
