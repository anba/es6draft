/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.async.iteration;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.async.iteration.AsyncGeneratorAbstractOperations.AsyncGeneratorEnqueue;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.ReturnValue;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>Async Generator Functions</h1><br>
 * <h2>AsyncGenerator Objects</h2>
 * <ul>
 * <li>Properties of AsyncGenerator Prototype
 * </ul>
 */
public final class AsyncGeneratorPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new AsyncGenerator prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public AsyncGeneratorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * Properties of AsyncGenerator Prototype
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.AsyncIteratorPrototype;

        /**
         * AsyncGenerator.prototype.constructor
         */
        @Value(name = "constructor",
                attributes = @Attributes(writable = false, enumerable = false, configurable = true) )
        public static final Intrinsics constructor = Intrinsics.AsyncGenerator;

        /**
         * AsyncGenerator.prototype.next ( value )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the value object
         * @return the promise object
         */
        @Function(name = "next", arity = 1)
        public static Object next(ExecutionContext cx, Object thisValue, Object value) {
            /* steps 1-3 */
            return AsyncGeneratorEnqueue(cx, thisValue, value);
        }

        /**
         * AsyncGenerator.prototype.return ( value )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the return value
         * @return the promise object
         */
        @Function(name = "return", arity = 1)
        public static Object _return(ExecutionContext cx, Object thisValue, Object value) {
            return AsyncGeneratorEnqueue(cx, thisValue, new ReturnValue(value));
        }

        /**
         * AsyncGenerator.prototype.throw ( exception )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param exception
         *            the exception object
         * @return the promise object
         */
        @Function(name = "throw", arity = 1)
        public static Object _throw(ExecutionContext cx, Object thisValue, Object exception) {
            return AsyncGeneratorEnqueue(cx, thisValue, ScriptException.create(exception));
        }

        /**
         * AsyncGenerator.prototype [ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true) )
        public static final String toStringTag = "AsyncGenerator";
    }
}
