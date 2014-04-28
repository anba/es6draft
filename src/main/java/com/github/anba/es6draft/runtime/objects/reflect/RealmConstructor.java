/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.reflect;

import static com.github.anba.es6draft.runtime.AbstractOperations.Construct;
import static com.github.anba.es6draft.runtime.AbstractOperations.GetOption;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsCallable;
import static com.github.anba.es6draft.runtime.Realm.CreateRealm;
import static com.github.anba.es6draft.runtime.internal.Errors.newError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
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
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

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
        // TODO: also subject to indirect-eval hook customisation?
        return Eval.indirectEval(realm.defaultContext(), source);
    }

    /**
     * Abstract Operation: DefineBuiltinProperties (realm, builtins)
     * 
     * @param realm
     *            the realm instance
     * @param builtins
     *            the builtins script object
     */
    public static void DefineBuiltinProperties(Realm realm, OrdinaryObject builtins) {
        // TODO: not yet specified
        realm.defineBuiltinProperties(builtins);

        // Run any initialization scripts, if required
        try {
            realm.getGlobalThis().initialize(builtins);
        } catch (ParserException | CompilationException e) {
            throw e.toScriptException(realm.defaultContext());
        } catch (IOException | URISyntaxException e) {
            throw newError(realm.defaultContext(), e.getMessage());
        }
    }

    private static Callable getFunctionOption(ExecutionContext cx, Object options, String name) {
        Object option = GetOption(cx, options, name);
        if (Type.isUndefined(option)) {
            return null;
        }
        if (!IsCallable(option)) {
            throw newTypeError(cx, Messages.Key.NotCallable);
        }
        return (Callable) option;
    }

    /**
     * 26.2.1.1 Reflect.Realm ([ options [, initializer ] ])
     */
    @Override
    public RealmObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
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
            throw newTypeError(calleeContext, Messages.Key.InitializedObject);
        }
        /* steps 5-6 (superseded by newer Realm API) */
        /* steps 7-8 */
        Object directEval = GetOption(calleeContext, options, "directEval");
        /* steps 9-11 */
        Callable translate = getFunctionOption(calleeContext, directEval, "translate");
        /* steps 12-14 */
        Callable fallback = getFunctionOption(calleeContext, directEval, "fallback");
        /* steps 15-17 */
        Callable indirectEval = getFunctionOption(calleeContext, options, "indirectEval");
        /* steps 18-20 (superseded by newer Realm API) */
        /* step ? */
        Callable initializer = getFunctionOption(calleeContext, options, "init");
        /* steps 21-22 */
        if (realmObject.getRealm() != null) {
            throw newTypeError(calleeContext, Messages.Key.InitializedObject);
        }
        /* step 23 */
        Realm realm = CreateRealm(calleeContext, realmObject);
        /* steps 24-27 */
        realm.setExtensionHooks(translate, fallback, indirectEval);
        /* step 28 */
        realmObject.setRealm(realm);
        /* step 29 */
        if (initializer != null) {
            OrdinaryObject builtins = ObjectCreate(realm.defaultContext(),
                    Intrinsics.ObjectPrototype);
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
