/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.iteration;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.iteration.GeneratorAbstractOperations.GeneratorResume;
import static com.github.anba.es6draft.runtime.objects.iteration.GeneratorAbstractOperations.GeneratorReturn;
import static com.github.anba.es6draft.runtime.objects.iteration.GeneratorAbstractOperations.GeneratorThrow;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.builtins.NativeFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>25 Control Abstraction Objects</h1><br>
 * <h2>25.3 Generator Objects</h2>
 * <ul>
 * <li>25.3.1 Properties of Generator Prototype
 * </ul>
 */
public final class GeneratorPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new Generator prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public GeneratorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * Marker class for {@code %GeneratorPrototype%.next}.
     */
    private static final class GeneratorPrototypeNext {
    }

    /**
     * Returns {@code true} if <var>next</var> is the built-in {@code %GeneratorPrototype%.next} function for the
     * requested realm.
     * 
     * @param realm
     *            the function realm
     * @param next
     *            the next function
     * @return {@code true} if <var>next</var> is the built-in {@code %GeneratorPrototype%.next} function
     */
    public static boolean isBuiltinNext(Realm realm, Object next) {
        return NativeFunction.isNative(realm, next, GeneratorPrototypeNext.class);
    }

    /**
     * 25.3.1 Properties of Generator Prototype
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.IteratorPrototype;

        /**
         * 25.3.1.1 Generator.prototype.constructor
         */
        @Value(name = "constructor", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final Intrinsics constructor = Intrinsics.Generator;

        /**
         * 25.3.1.2 Generator.prototype.next ( value )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the value object
         * @return the iterator result object
         */
        @Function(name = "next", arity = 1, nativeId = GeneratorPrototypeNext.class)
        public static Object next(ExecutionContext cx, Object thisValue, Object value) {
            return GeneratorResume(cx, thisValue, value);
        }

        /**
         * 25.3.1.3 Generator.prototype.return ( value )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the return value
         * @return the iterator result object
         */
        @Function(name = "return", arity = 1)
        public static Object _return(ExecutionContext cx, Object thisValue, Object value) {
            return GeneratorReturn(cx, thisValue, value);
        }

        /**
         * 25.3.1.4 Generator.prototype.throw ( exception )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param exception
         *            the exception object
         * @return the iterator result object
         */
        @Function(name = "throw", arity = 1)
        public static Object _throw(ExecutionContext cx, Object thisValue, Object exception) {
            return GeneratorThrow(cx, thisValue, exception);
        }

        /**
         * 25.3.1.5 Generator.prototype [ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "Generator";
    }
}
