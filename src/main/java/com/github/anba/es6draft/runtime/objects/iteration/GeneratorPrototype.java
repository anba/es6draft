/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.iteration;

import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.19 The "std:iteration" Module</h2><br>
 * <h3>15.19.4 Generator Objects</h3>
 * <ul>
 * <li>15.19.4.2 Properties of Generator Prototype
 * </ul>
 */
public class GeneratorPrototype extends OrdinaryObject implements Initialisable {
    public GeneratorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
    }

    /**
     * 15.19.4.2 Properties of Generator Prototype
     */
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

        /**
         * 15.19.4.2.1 Generator.prototype.constructor
         */
        @Value(name = "constructor", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final Intrinsics constructor = Intrinsics.Generator;

        /**
         * 15.19.4.2.3 Generator.prototype.next ( value )
         */
        @Function(name = "next", arity = 0)
        public static Object next(ExecutionContext cx, Object thisValue, Object value) {
            return generatorObject(cx, thisValue).send(cx, UNDEFINED);
        }

        /**
         * 15.19.4.2.4 Generator.prototype.throw ( exception )
         */
        @Function(name = "throw", arity = 1)
        public static Object _throw(ExecutionContext cx, Object thisValue, Object exception) {
            return generatorObject(cx, thisValue)._throw(cx, exception);
        }

        /**
         * 15.19.4.2.5 Generator.prototype [ @@toStringTag ]
         */
        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = "Generator";
    }
}
