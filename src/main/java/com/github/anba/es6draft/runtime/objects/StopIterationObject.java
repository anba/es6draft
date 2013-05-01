/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.IntegrityLevel;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * TODO: spec incomplete<br>
 * TODO: change to constructor for return-in-generator support
 */
public class StopIterationObject extends OrdinaryObject implements Initialisable {
    public StopIterationObject(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        setIntegrity(cx, IntegrityLevel.NonExtensible);
    }

    /**
     * FIXME: missing in spec
     */
    public static boolean IteratorComplete(Realm realm, ScriptException e) {
        // return (realm.getIntrinsic(Intrinsics.StopIteration) == e.getValue());
        // TODO: IteratorComplete() works cross-realm?
        return (e.getValue() instanceof StopIterationObject);
    }

    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 
         */
        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = "StopIteration";

        /**
         * 
         */
        @Function(name = "@@hasInstance", arity = 1, symbol = BuiltinSymbol.hasInstance)
        public static Object hasInstance(ExecutionContext cx, Object thisValue, Object v) {
            return cx.getIntrinsic(Intrinsics.StopIteration) == v;
        }
    }
}
