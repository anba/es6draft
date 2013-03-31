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

import com.github.anba.es6draft.runtime.ExecutionContext;
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
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
    }

    public enum Properties {
        ;

        private static GeneratorObject generatorObject(ExecutionContext cx, Object object) {
            if (object instanceof GeneratorObject) {
                return (GeneratorObject) object;
            }
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Generator;

        @Function(name = "next", arity = 0)
        public static Object next(ExecutionContext cx, Object thisValue) {
            return generatorObject(cx, thisValue).send(cx, UNDEFINED);
        }

        @Function(name = "send", arity = 1)
        public static Object send(ExecutionContext cx, Object thisValue, Object value) {
            return generatorObject(cx, thisValue).send(cx, value);
        }

        @Function(name = "throw", arity = 1)
        public static Object _throw(ExecutionContext cx, Object thisValue, Object value) {
            return generatorObject(cx, thisValue)._throw(cx, value);
        }

        @Function(name = "close", arity = 0)
        public static Object close(ExecutionContext cx, Object thisValue) {
            return generatorObject(cx, thisValue).close(cx);
        }

        @Function(name = "@@iterator", symbol = BuiltinSymbol.iterator, arity = 0)
        public static Object iterator(ExecutionContext cx, Object thisValue) {
            return thisValue;
        }
    }
}
