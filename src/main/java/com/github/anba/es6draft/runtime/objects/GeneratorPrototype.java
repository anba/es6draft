/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.builtins.GeneratorObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 *
 */
public class GeneratorPrototype extends OrdinaryObject implements Initialisable {
    public GeneratorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);
    }

    public enum Properties {
        ;

        private static GeneratorObject generatorObject(Realm realm, Object object) {
            if (object instanceof GeneratorObject) {
                return (GeneratorObject) object;
            }
            throw throwTypeError(realm, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Generator;

        @Function(name = "next", arity = 0)
        public static Object next(Realm realm, Object thisValue) {
            return generatorObject(realm, thisValue).send(realm, UNDEFINED);
        }

        @Function(name = "send", arity = 1)
        public static Object send(Realm realm, Object thisValue, Object value) {
            return generatorObject(realm, thisValue).send(realm, value);
        }

        @Function(name = "throw", arity = 1)
        public static Object _throw(Realm realm, Object thisValue, Object value) {
            return generatorObject(realm, thisValue)._throw(realm, value);
        }

        @Function(name = "close", arity = 0)
        public static Object close(Realm realm, Object thisValue) {
            return generatorObject(realm, thisValue).close(realm);
        }

        @Function(name = "@@iterator", symbol = BuiltinSymbol.iterator, arity = 0)
        public static Object iterator(Realm realm, Object thisValue) {
            return thisValue;
        }
    }
}
