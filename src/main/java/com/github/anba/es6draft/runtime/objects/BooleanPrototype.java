/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.throwTypeError;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.6 Boolean Objects</h2>
 * <ul>
 * <li>15.6.4 Properties of the Boolean Prototype Object
 * </ul>
 */
public class BooleanPrototype extends BooleanObject implements Scriptable, Initialisable {
    public BooleanPrototype(Realm realm) {
        super(realm, false);
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);
    }

    /**
     * 15.6.4 Properties of the Boolean Prototype Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.6.4.1 Boolean.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Boolean;

        /**
         * 15.6.4.2 Boolean.prototype.toString ( )
         */
        @Function(name = "toString", arity = 0)
        public static Object toString(Realm realm, Object thisValue) {
            /* step 1 (omitted) */
            // Object B = thisValue;
            boolean b;
            if (Type.isBoolean(thisValue)) {
                /* step 2 */
                b = Type.booleanValue(thisValue);
            } else if (Type.isObject(thisValue) && thisValue instanceof BooleanObject) {
                /* step 3 */
                b = ((BooleanObject) thisValue).getBooleanData();
            } else {
                /* step 4 */
                throw throwTypeError(realm, "incompatible object");
            }
            /* step 5 */
            return (b ? "true" : "false");
        }

        /**
         * 15.6.4.3 Boolean.prototype.valueOf ( )
         */
        @Function(name = "valueOf", arity = 0)
        public static Object valueOf(Realm realm, Object thisValue) {
            /* step 1 (omitted) */
            // Object B = thisValue;
            boolean b;
            if (Type.isBoolean(thisValue)) {
                /* step 2 */
                b = Type.booleanValue(thisValue);
            } else if (Type.isObject(thisValue) && thisValue instanceof BooleanObject) {
                /* step 3 */
                b = ((BooleanObject) thisValue).getBooleanData();
            } else {
                /* step 4 */
                throw throwTypeError(realm, "incompatible object");
            }
            /* step 5 */
            return b;
        }
    }
}
