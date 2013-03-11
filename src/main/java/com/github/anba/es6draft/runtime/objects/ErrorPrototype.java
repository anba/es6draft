/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.11 Error Objects</h2>
 * <ul>
 * <li>15.11.4 Properties of the Error Prototype Object
 * </ul>
 */
public class ErrorPrototype extends OrdinaryObject implements Scriptable, Initialisable {
    public ErrorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);
    }

    /**
     * 15.11.4 Properties of the Error Prototype Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.11.4.1 Error.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Error;

        /**
         * 15.11.4.2 Error.prototype.name
         */
        @Value(name = "name")
        public static final String name = "Error";

        /**
         * 15.11.4.3 Error.prototype.message
         */
        @Value(name = "message")
        public static final String message = "";

        /**
         * 15.11.4.4 Error.prototype.toString ( )
         */
        @Function(name = "toString", arity = 0)
        public static Object toString(Realm realm, Object thisValue) {
            if (!Type.isObject(thisValue)) {
                throw throwTypeError(realm, Messages.Key.NotObjectType);
            }
            Scriptable o = Type.objectValue(thisValue);
            Object name = Get(o, "name");
            CharSequence sname = (Type.isUndefined(name) ? "Error" : ToString(realm, name));
            Object msg = Get(o, "message");
            CharSequence smsg = (Type.isUndefined(msg) ? "" : ToString(realm, msg));
            if (sname.length() == 0) {
                return smsg;
            }
            if (smsg.length() == 0) {
                return sname;
            }
            return sname + ": " + smsg;
        }
    }
}
