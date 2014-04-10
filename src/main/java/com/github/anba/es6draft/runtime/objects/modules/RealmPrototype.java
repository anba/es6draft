/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.modules;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.modules.RealmConstructor.IndirectEval;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.EnumSet;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>26 Reflection</h1><br>
 * <h2>26.2 Realm Objects</h2>
 * <ul>
 * <li>26.2.3 Properties of the Reflect.Realm Prototype Object
 * </ul>
 */
public final class RealmPrototype extends OrdinaryObject implements Initialisable {
    public RealmPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
    }

    /**
     * 26.2.3 Properties of the Reflect.Realm Prototype Object
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
                RealmObject realmObject = (RealmObject) value;
                if (realmObject.getRealm() != null) {
                    return realmObject;
                }
                throw newTypeError(cx, Messages.Key.UninitialisedObject);
            }
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 26.2.3.1 Reflect.Realm.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Realm;

        /**
         * 26.2.3.2 Reflect.Realm.prototype.eval (source)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param source
         *            the source string
         * @return the evaluation result
         */
        @Function(name = "eval", arity = 1)
        public static Object eval(ExecutionContext cx, Object thisValue, Object source) {
            /* steps 1-4 */
            RealmObject realmObject = thisRealmObject(cx, thisValue);
            /* step 5 */
            return IndirectEval(realmObject.getRealm(), source);
        }

        /**
         * 26.2.3.3 get Reflect.Realm.prototype.global
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
            RealmObject realmObject = thisRealmObject(cx, thisValue);
            /* step 5 */
            return realmObject.getRealm().getGlobalThis();
        }

        /**
         * 26.2.3.4 get Reflect.Realm.prototype.intrinsics
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
            RealmObject realmObject = thisRealmObject(cx, thisValue);
            Realm realm = realmObject.getRealm();
            /* step 5 */
            ScriptObject table = ObjectCreate(cx, Intrinsics.ObjectPrototype);
            /* step 6 */
            for (Intrinsics intrinsic : Intrinsics.values()) {
                if (internalIntrinsics.contains(intrinsic)) {
                    continue;
                }
                String intrinsicKey = intrinsic.getKey();
                CreateDataProperty(cx, table, intrinsicKey, realm.getIntrinsic(intrinsic));
            }
            /* step 7 */
            return table;
        }

        private static final EnumSet<Intrinsics> internalIntrinsics;
        static {
            internalIntrinsics = EnumSet.of(Intrinsics.ListIteratorNext, Intrinsics.InternalError,
                    Intrinsics.InternalErrorPrototype, Intrinsics.LegacyGeneratorPrototype);
        }

        /**
         * 26.2.3.5 get Reflect.Realm.prototype.stdlib
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
            RealmObject realmObject = thisRealmObject(cx, thisValue);
            Realm realm = realmObject.getRealm();
            /* step 5 */
            OrdinaryObject props = ObjectCreate(cx, Intrinsics.ObjectPrototype);
            /* step 6 */
            // FIXME: spec unclear, need feedback
            realm.getGlobalThis().defineBuiltinProperties(cx, props);
            /* step 7 */
            return props;
        }

        /**
         * 26.2.3.6 Reflect.Realm.prototype [ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "Reflect.Realm";

        /**
         * 26.2.3.7 Realm Subclass Extension Properties
         * <p>
         * 26.2.3.7.1 Reflect.Realm.prototype.directEval ( source )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param source
         *            the source string
         * @return the evaluation result
         */
        @Function(name = "directEval", arity = 1)
        public static Object directEval(ExecutionContext cx, Object thisValue, Object source) {
            /* steps 1-4 */
            RealmObject realmObject = thisRealmObject(cx, thisValue);
            /* step 5 */
            return IndirectEval(realmObject.getRealm(), source);
        }

        /**
         * 26.2.3.7 Realm Subclass Extension Properties
         * <p>
         * 26.2.3.7.2 Reflect.Realm.prototype.indirectEval ( source )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param source
         *            the source string
         * @return the evaluation result
         */
        @Function(name = "indirectEval", arity = 1)
        public static Object indirectEval(ExecutionContext cx, Object thisValue, Object source) {
            /* steps 1-4 */
            RealmObject realmObject = thisRealmObject(cx, thisValue);
            /* step 5 */
            return IndirectEval(realmObject.getRealm(), source);
        }

        /**
         * 26.2.3.7 Realm Subclass Extension Properties
         * <p>
         * 26.2.3.7.3 Reflect.Realm.prototype.initGlobal ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the evaluation result
         */
        @Function(name = "initGlobal", arity = 0)
        public static Object initGlobal(ExecutionContext cx, Object thisValue) {
            /* steps 1-4 */
            @SuppressWarnings("unused")
            RealmObject realmObject = thisRealmObject(cx, thisValue);
            /* step 5 */
            return UNDEFINED;
        }
    }
}
