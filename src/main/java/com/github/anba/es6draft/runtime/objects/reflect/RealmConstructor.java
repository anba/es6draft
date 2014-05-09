/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.reflect;

import static com.github.anba.es6draft.runtime.AbstractOperations.Construct;
import static com.github.anba.es6draft.runtime.AbstractOperations.GetMethod;
import static com.github.anba.es6draft.runtime.Realm.CreateRealm;
import static com.github.anba.es6draft.runtime.internal.Errors.newError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticProxy.ProxyCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;

import java.io.IOException;
import java.net.URISyntaxException;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
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
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;

/**
 * <h1>26 Reflection</h1><br>
 * <h2>26.2 Realm Objects</h2>
 * <ul>
 * <li>26.2.1 The Reflect.Realm Constructor
 * <li>26.2.2 Properties of the Reflect.Realm Constructor
 * </ul>
 */
public final class RealmConstructor extends BuiltinConstructor implements Initializable {
    public RealmConstructor(Realm realm) {
        super(realm, "Realm");
    }

    @Override
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    @Override
    public RealmConstructor clone() {
        return new RealmConstructor(getRealm());
    }

    /**
     * Abstract Operation: IndirectEval (realm, source)
     * 
     * @param realm
     *            the realm instance
     * @param source
     *            the source string
     * @return the evaluation result
     */
    public static Object IndirectEval(Realm realm, Object source) {
        // TODO: not yet specified
        return Eval.indirectEval(realm.defaultContext(), source);
    }

    /**
     * 26.2.1.1 Reflect.Realm ( [ target , handler ] )
     */
    @Override
    public RealmObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        /* steps 2-3 */
        if (!(thisValue instanceof RealmObject)) {
            throw newTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        /* step 1 */
        RealmObject realmObject = (RealmObject) thisValue;
        /* step 4 */
        if (realmObject.getRealm() != null) {
            throw newTypeError(calleeContext, Messages.Key.InitializedObject);
        }

        /* steps 5-6 */
        // FIXME: spec bug - invalid steps

        /* steps 8-10 (moved) */
        ScriptObject newGlobal;
        if (args.length != 0) {
            Object target = getArgument(args, 0);
            Object handler = getArgument(args, 1);
            newGlobal = ProxyCreate(calleeContext, target, handler);
        } else {
            // Note: created in CreateRealm()
            newGlobal = null;
        }

        /* steps 7, 11-13 */
        Realm realm = CreateRealm(calleeContext, realmObject, newGlobal);

        /* steps 14-15 */
        Callable translate = GetMethod(calleeContext, realmObject, "directEval");

        /* steps 16-17 */
        Callable fallback = GetMethod(calleeContext, realmObject, "nonEval");

        /* steps 18-19 */
        Callable indirectEval = GetMethod(calleeContext, realmObject, "indirectEval");

        /* steps 20-22 */
        realm.setExtensionHooks(translate, fallback, indirectEval);

        /* steps 23-24 */
        if (realmObject.getRealm() != null) {
            throw newTypeError(calleeContext, Messages.Key.InitializedObject);
        }

        /* step 25 */
        realmObject.setRealm(realm);

        // Run any initialization scripts, if required
        try {
            GlobalObject globalObject = realm.getGlobalObject();
            assert globalObject != null;
            globalObject.initialize();
        } catch (ParserException | CompilationException e) {
            throw e.toScriptException(realm.defaultContext());
        } catch (IOException | URISyntaxException e) {
            throw newError(realm.defaultContext(), e.getMessage());
        }

        /* steps 26-27 */
        Callable initGlobal = GetMethod(calleeContext, realmObject, "initGlobal");

        /* steps 28-29 */
        if (initGlobal != null) {
            /* step 28 */
            initGlobal.call(calleeContext, realmObject);
        } else {
            /* step 29 */
            ScriptObject globalThis = realm.getGlobalThis();
            GlobalObject globalObject = realm.getGlobalObject();
            assert globalThis != null && globalObject != null;

            // Define the built-in properties
            globalObject.defineBuiltinProperties(calleeContext, globalThis);
        }
        /* step 30 */
        return realmObject;
    }

    /**
     * 26.2.1.2 new Reflect.Realm ( ... argumentsList )
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return Construct(callerContext, this, args);
    }

    /**
     * 26.2.2 Properties of the Reflect.Realm Constructor
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
        public static final String name = "Realm";

        /**
         * 26.2.2.1 Reflect.Realm.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.RealmPrototype;

        /**
         * 26.2.2.2 Reflect.Realm [ @@create ] ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the new uninitialized realm object
         */
        @Function(name = "[Symbol.create]", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.RealmPrototype,
                    RealmObjectAllocator.INSTANCE);
        }
    }

    private static final class RealmObjectAllocator implements ObjectAllocator<RealmObject> {
        static final ObjectAllocator<RealmObject> INSTANCE = new RealmObjectAllocator();

        @Override
        public RealmObject newInstance(Realm realm) {
            return new RealmObject(realm);
        }
    }
}
