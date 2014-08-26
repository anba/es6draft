/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.reflect;

import static com.github.anba.es6draft.runtime.AbstractOperations.Construct;
import static com.github.anba.es6draft.runtime.AbstractOperations.GetMethod;
import static com.github.anba.es6draft.runtime.Realm.CreateRealmAndSetRealmGlobalObj;
import static com.github.anba.es6draft.runtime.Realm.SetDefaultGlobalBindings;
import static com.github.anba.es6draft.runtime.internal.Errors.newError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticProxy.ProxyCreate;

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
    /**
     * Constructs a new Realm constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public RealmConstructor(Realm realm) {
        super(realm, "Realm");
    }

    @Override
    public void initialize(ExecutionContext cx) {
        addRestrictedFunctionProperties(cx);
        createProperties(cx, this, Properties.class);
    }

    @Override
    public RealmConstructor clone() {
        return new RealmConstructor(getRealm());
    }

    /**
     * Abstract Operation: IndirectEval (realm, source)
     * 
     * @param caller
     *            the caller context
     * @param realm
     *            the realm instance
     * @param source
     *            the source string
     * @return the evaluation result
     */
    public static Object IndirectEval(ExecutionContext caller, Realm realm, Object source) {
        // TODO: not yet specified
        return Eval.indirectEval(realm.defaultContext(), caller, source);
    }

    /**
     * 26.2.1.1 Reflect.Realm ( [ target , handler ] )
     */
    @Override
    public RealmObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        /* step 2 */
        if (!(thisValue instanceof RealmObject)) {
            throw newTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        /* step 1 */
        RealmObject realmObject = (RealmObject) thisValue;
        /* step 3 */
        if (realmObject.getRealm() != null) {
            throw newTypeError(calleeContext, Messages.Key.InitializedObject);
        }

        /* steps 4-5 */
        ScriptObject newGlobal;
        if (args.length != 0) {
            /* step 4 */
            Object target = argument(args, 0);
            Object handler = argument(args, 1);
            newGlobal = ProxyCreate(calleeContext, target, handler);
        } else {
            /* step 5 */
            newGlobal = null;
        }

        /* steps 6-7 */
        Realm realm = CreateRealmAndSetRealmGlobalObj(calleeContext, realmObject, newGlobal);
        /* steps 8-9 */
        Callable translate = GetMethod(calleeContext, realmObject, "directEval");
        /* steps 10-11 */
        Callable fallback = GetMethod(calleeContext, realmObject, "nonEval");
        /* steps 12-13 */
        Callable indirectEval = GetMethod(calleeContext, realmObject, "indirectEval");
        /* steps 14-16 */
        realm.setExtensionHooks(translate, fallback, indirectEval);
        /* steps 17-18 */
        if (realmObject.getRealm() != null) {
            throw newTypeError(calleeContext, Messages.Key.InitializedObject);
        }
        /* step 19 */
        realmObject.setRealm(realm);

        // Run any initialization scripts, if required. But do _not_ install extensions!
        try {
            GlobalObject globalObject = realm.getGlobalObject();
            assert globalObject != null;
            globalObject.initializeScripted();
        } catch (ParserException | CompilationException e) {
            throw e.toScriptException(calleeContext);
        } catch (IOException | URISyntaxException e) {
            throw newError(calleeContext, e.getMessage());
        }

        /* steps 20-21 */
        Callable initGlobal = GetMethod(calleeContext, realmObject, "initGlobal");
        /* steps 22-23 */
        if (initGlobal != null) {
            /* step 22 */
            initGlobal.call(calleeContext, realmObject);
        } else {
            /* step 23 */
            SetDefaultGlobalBindings(calleeContext, realm);
        }
        /* step 24 */
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
