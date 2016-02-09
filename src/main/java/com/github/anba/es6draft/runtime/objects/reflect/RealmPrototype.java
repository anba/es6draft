/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.reflect;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.CreateListFromArrayLike;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsCallable;
import static com.github.anba.es6draft.runtime.Realm.SetDefaultGlobalBindings;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.PrepareForTailCall;
import static com.github.anba.es6draft.runtime.objects.reflect.RealmConstructor.IndirectEval;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.FromPropertyDescriptor;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.TailCall;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.GlobalObject;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>26 Reflection</h1><br>
 * <h2>26.? Realm Objects</h2>
 * <ul>
 * <li>26.?.3 Properties of the Reflect.Realm Prototype Object
 * </ul>
 */
public final class RealmPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new Realm prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public RealmPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * 26.?.3 Properties of the Reflect.Realm Prototype Object
     */
    public enum Properties {
        ;

        /**
         * Abstract Operation: thisRealmObject(value)
         * 
         * @param cx
         *            the execution context
         * @param value
         *            the argument value
         * @return the realm object
         */
        private static RealmObject thisRealmObject(ExecutionContext cx, Object value) {
            if (value instanceof RealmObject) {
                return (RealmObject) value;
            }
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }

        /**
         * Abstract Operation: thisRealmValue(value)
         * 
         * @param cx
         *            the execution context
         * @param value
         *            the argument value
         * @return the realm record
         */
        private static Realm thisRealmValue(ExecutionContext cx, Object value) {
            return thisRealmObject(cx, value).getRealm();
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 26.?.3.1 Reflect.Realm.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Realm;

        /**
         * 26.?.3.2 Reflect.Realm.prototype.eval (source)
         * 
         * @param cx
         *            the execution context
         * @param caller
         *            the caller context
         * @param thisValue
         *            the function this-value
         * @param source
         *            the source string
         * @return the evaluation result
         */
        @Function(name = "eval", arity = 1)
        public static Object eval(ExecutionContext cx, ExecutionContext caller, Object thisValue,
                Object source) {
            /* steps 1-4 */
            Realm realm = thisRealmValue(cx, thisValue);
            /* step 5 */
            return IndirectEval(caller, realm, source);
        }

        /**
         * 26.?.3.3 get Reflect.Realm.prototype.global
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the global object instance
         */
        @Accessor(name = "global", type = Accessor.Type.Getter)
        public static Object global(ExecutionContext cx, Object thisValue) {
            /* steps 1-4 */
            Realm realm = thisRealmValue(cx, thisValue);
            /* step 5 */
            return realm.getGlobalThis();
        }

        /**
         * 26.?.3.4 get Reflect.Realm.prototype.intrinsics
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the intrinsics object instance
         */
        @Accessor(name = "intrinsics", type = Accessor.Type.Getter)
        public static Object intrinsics(ExecutionContext cx, Object thisValue) {
            /* steps 1-4 */
            Realm realm = thisRealmValue(cx, thisValue);
            /* step 5 */
            OrdinaryObject table = ObjectCreate(cx, Intrinsics.ObjectPrototype);
            /* step 6 */
            for (Intrinsics intrinsic : Intrinsics.values()) {
                if (intrinsic.isInternal()) {
                    continue;
                }
                String intrinsicKey = intrinsic.getKey();
                OrdinaryObject intrinsicValue = realm.getIntrinsic(intrinsic);
                if (intrinsicValue == null) {
                    continue;
                }
                CreateDataProperty(cx, table, intrinsicKey, intrinsicValue);
            }
            /* step 7 */
            return table;
        }

        /**
         * 26.?.3.5 get Reflect.Realm.prototype.stdlib
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the stdlib object descriptor
         */
        @Accessor(name = "stdlib", type = Accessor.Type.Getter)
        public static Object stdlib(ExecutionContext cx, Object thisValue) {
            /* steps 1-4 */
            Realm realm = thisRealmValue(cx, thisValue);
            /* step 5 */
            OrdinaryObject props = ObjectCreate(cx, Intrinsics.ObjectPrototype);
            /* step 6 */
            GlobalObject globalObject = realm.getGlobalObject();
            for (Object key : globalObject.ownPropertyKeys(cx)) {
                if (key instanceof String) {
                    String propertyKey = (String) key;
                    Property prop = globalObject.getOwnProperty(cx, propertyKey);
                    if (prop != null) {
                        CreateDataProperty(cx, props, propertyKey, FromPropertyDescriptor(cx, prop));
                    }
                } else {
                    Symbol propertyKey = (Symbol) key;
                    Property prop = globalObject.getOwnProperty(cx, propertyKey);
                    if (prop != null) {
                        CreateDataProperty(cx, props, propertyKey, FromPropertyDescriptor(cx, prop));
                    }
                }
            }
            /* step 7 */
            return props;
        }

        /**
         * 26.?.3.6 Reflect.Realm.prototype [ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "Reflect.Realm";

        /**
         * 26.?.3.7 Realm Subclass Extension Properties
         * <p>
         * 26.?.3.7.1 Reflect.Realm.prototype.directEval ( source )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param source
         *            the source string
         * @return the translated source
         */
        @Function(name = "directEval", arity = 1)
        public static Object directEval(ExecutionContext cx, Object thisValue, Object source) {
            /* step 1 */
            return source;
        }

        /**
         * 26.?.3.7 Realm Subclass Extension Properties
         * <p>
         * 26.?.3.7.2 Reflect.Realm.prototype.indirectEval ( source )
         * 
         * @param cx
         *            the execution context
         * @param caller
         *            the caller context
         * @param thisValue
         *            the function this-value
         * @param source
         *            the source string
         * @return the evaluation result
         */
        @Function(name = "indirectEval", arity = 1)
        public static Object indirectEval(ExecutionContext cx, ExecutionContext caller,
                Object thisValue, Object source) {
            /* step 1 */
            return source;
        }

        /**
         * 26.?.3.7 Realm Subclass Extension Properties
         * <p>
         * 26.?.3.7.3 Reflect.Realm.prototype.initGlobal ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the global this object
         */
        @Function(name = "initGlobal", arity = 0)
        public static Object initGlobal(ExecutionContext cx, Object thisValue) {
            /* steps 1-4 */
            Realm realm = thisRealmValue(cx, thisValue);
            /* step 5 */
            return SetDefaultGlobalBindings(cx, realm);
        }

        /**
         * 26.?.3.7 Realm Subclass Extension Properties
         * <p>
         * 26.?.3.7.4 Reflect.Realm.prototype.nonEval (function, thisValue, argumentsList )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param function
         *            the non-eval function
         * @param thisArgument
         *            the this-argument for the non-eval function
         * @param argumentsList
         *            the arguments for the non-eval function
         * @return the evaluation result
         */
        @TailCall
        @Function(name = "nonEval", arity = 3)
        public static Object nonEval(ExecutionContext cx, Object thisValue, Object function,
                Object thisArgument, Object argumentsList) {
            /* step 1 */
            if (!IsCallable(function)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            /* steps 2-3 */
            Object[] args = CreateListFromArrayLike(cx, argumentsList);
            /* steps 4-5 */
            return PrepareForTailCall((Callable) function, thisArgument, args);
        }
    }
}
