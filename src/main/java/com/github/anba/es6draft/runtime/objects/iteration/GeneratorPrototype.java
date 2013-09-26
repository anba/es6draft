/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.iteration;

import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.iteration.IterationAbstractOperations.GeneratorResume;

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
 * <h1>25 The "std:iteration" Module</h1><br>
 * <h2>25.4 Generator Objects</h2>
 * <ul>
 * <li>25.4.2 Properties of Generator Prototype
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
     * 25.4.1 Properties of Generator Prototype
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 25.4.1.1 Generator.prototype.constructor
         */
        @Value(name = "constructor", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final Intrinsics constructor = Intrinsics.Generator;

        /**
         * 25.4.1.2 Generator.prototype.next ( value )
         */
        @Function(name = "next", arity = 0)
        public static Object next(ExecutionContext cx, Object thisValue, Object value) {
            return GeneratorResume(cx, thisValue, value);
        }

        /**
         * 25.4.1.3 Generator.prototype.throw ( exception )
         */
        @Function(name = "throw", arity = 1)
        public static Object _throw(ExecutionContext cx, Object thisValue, Object exception) {
            /* steps 1-3 */
            if (!(thisValue instanceof GeneratorObject)) {
                throw throwTypeError(cx, Messages.Key.IncompatibleObject);
            }
            /* steps 4-16 */
            return ((GeneratorObject) thisValue)._throw(cx, exception);
        }

        /**
         * 25.4.1.4 Generator.prototype.@@iterator ()
         */
        @Function(name = "@@iterator", symbol = BuiltinSymbol.iterator, arity = 0)
        public static Object iterator(ExecutionContext cx, Object thisValue) {
            return thisValue;
        }

        /**
         * 25.4.1.5 Generator.prototype [ @@toStringTag ]
         */
        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "Generator";
    }
}
