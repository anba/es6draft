/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.modules;

import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.modules.RealmConstructor.IndirectEval;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>1 Modules: Semantics</h1><br>
 * <h2>1.5 Realm Objects</h2>
 * <ul>
 * <li>1.5.2 Properties of the Realm Prototype Object
 * </ul>
 */
public class RealmPrototype extends OrdinaryObject implements Initialisable {
    public RealmPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
    }

    /**
     * 1.5.2 Properties of the Realm Prototype Object
     */
    public enum Properties {
        ;

        /**
         * Abstract Operation: thisRealmObject(value)
         */
        private static RealmObject thisRealmObject(ExecutionContext cx, Object value) {
            if (value instanceof RealmObject) {
                RealmObject realmObject = (RealmObject) value;
                if (realmObject.getRealm() == null) {
                    throw newTypeError(cx, Messages.Key.UninitialisedObject);
                }
                return realmObject;
            }
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * Realm.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Realm;

        /**
         * 1.5.2.1 get Realm.prototype.global
         */
        @Accessor(name = "global", type = Accessor.Type.Getter)
        public static Object global(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            RealmObject realmObject = thisRealmObject(cx, thisValue);
            /* step 3 */
            return realmObject.getRealm().getGlobalThis();
        }

        /**
         * 1.5.2.2 Realm.prototype.eval ( source )
         */
        @Function(name = "eval", arity = 1)
        public static Object eval(ExecutionContext cx, Object thisValue, Object source) {
            /* steps 1-2 */
            RealmObject realmObject = thisRealmObject(cx, thisValue);
            /* step 3 */
            return IndirectEval(realmObject.getRealm(), source);
        }
    }
}
