/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.modules;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.GetMethod;
import static com.github.anba.es6draft.runtime.AbstractOperations.OrdinaryCreateFromConstructor;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.OrdinaryConstruct;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.Eval;
import com.github.anba.es6draft.runtime.objects.GlobalObject;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;

/**
 * <h1>1 Modules: Semantics</h1><br>
 * <h2>1.5 Realm Objects</h2>
 * <ul>
 * <li>1.5.1 The Realm Constructor
 * </ul>
 */
public class RealmConstructor extends BuiltinConstructor implements Initialisable {
    public RealmConstructor(Realm realm) {
        super(realm, "Realm");
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    /**
     * Abstract Operation: CreateRealm (realmObject)
     */
    public static Realm CreateRealm(ExecutionContext cx, RealmObject realmObject) {
        // TODO: not yet specified
        return cx.getRealm().getWorld().newRealm(realmObject);
    }

    /**
     * Abstract Operation: IndirectEval (realm, source)
     */
    public static Object IndirectEval(Realm realm, Object source) {
        // TODO: not yet specified
        // TODO: also subject to indirect-eval hook customisation?
        return Eval.indirectEval(realm.defaultContext(), source);
    }

    /**
     * Abstract Operation: DefineBuiltinProperties (realm, builtins)
     */
    public static void DefineBuiltinProperties(Realm realm, ScriptObject builtins) {
        // TODO: not yet specified
        realm.defineBuiltinProperties(builtins);
    }

    /**
     * 1.5.1.1 Realm ( options )
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object options = args.length > 0 ? args[0] : UNDEFINED;
        /* steps 2-3 */
        if (!(thisValue instanceof RealmObject)) {
            throw newTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        /* step 1 */
        RealmObject realmObject = (RealmObject) thisValue;
        /* step 4 */
        if (realmObject.getRealm() != null) {
            throw newTypeError(calleeContext, Messages.Key.InitialisedObject);
        }

        /* steps 5-6 */
        if (Type.isUndefined(options)) {
            options = ObjectCreate(calleeContext, (ScriptObject) null);
        } else if (!Type.isObject(options)) {
            throw newTypeError(calleeContext, Messages.Key.NotObjectType);
        }
        ScriptObject optionsObject = Type.objectValue(options);
        /* steps 12-13 */
        Object directEval = Get(calleeContext, optionsObject, "directEval");
        /* steps 14-15 */
        if (Type.isUndefined(directEval)) {
            // TODO: change to `ObjectCreate(null, ())` just like above?
            directEval = ObjectCreate(calleeContext, Intrinsics.ObjectPrototype);
        } else if (!Type.isObject(directEval)) {
            throw newTypeError(calleeContext, Messages.Key.NotObjectType);
        }
        ScriptObject directEvalObject = Type.objectValue(directEval);
        /* steps 16-18 */
        Callable translate = GetMethod(calleeContext, directEvalObject, "translate");
        /* steps 20-22 */
        Callable fallback = GetMethod(calleeContext, directEvalObject, "fallback");
        /* steps 24-26 */
        Callable indirectEval = GetMethod(calleeContext, optionsObject, "indirectEval");
        /* step ? */
        Callable initializer = GetMethod(calleeContext, optionsObject, "init");

        // FIXME: as usual, reentrancy checks
        if (realmObject.getRealm() != null) {
            throw newTypeError(calleeContext, Messages.Key.InitialisedObject);
        }

        /* step 7 */
        Realm realm = CreateRealm(calleeContext, realmObject);
        /* steps 19, 23, 27 */
        realm.setExtensionHooks(translate, fallback, indirectEval);
        /* step 28 */
        realmObject.setRealm(realm);
        /* step 29 */
        if (initializer != null) {
            ScriptObject builtins = ObjectCreate(realm.defaultContext(), Intrinsics.ObjectPrototype);
            DefineBuiltinProperties(realm, builtins);
            // TODO: spec bug? necessary to provide realm object as thisArgument and 1st parameter?
            initializer.call(calleeContext, realmObject, realmObject, builtins);
        } else {
            // default global object environment
            GlobalObject globalObject = realm.getGlobalThis();
            globalObject.setPrototype(realm.getIntrinsic(Intrinsics.ObjectPrototype));
            DefineBuiltinProperties(realm, globalObject);
        }
        /* step 30 */
        return realmObject;
    }

    /**
     * new Realm (... argumentsList)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return OrdinaryConstruct(callerContext, this, args);
    }

    /**
     * Properties of the Realm Constructor
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
        public static final String name = "Realm";

        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.RealmPrototype;

        /**
         * 1.5.2.3 Realm [ @@create ] ( )
         */
        @Function(name = "[Symbol.create]", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            // FIXME: spec bug - wrong variable name 'realm' instead of 'realmObject'
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.RealmPrototype,
                    RealmObjectAllocator.INSTANCE);
        }
    }

    private static class RealmObjectAllocator implements ObjectAllocator<RealmObject> {
        static final ObjectAllocator<RealmObject> INSTANCE = new RealmObjectAllocator();

        @Override
        public RealmObject newInstance(Realm realm) {
            return new RealmObject(realm);
        }
    }
}
