/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.iteration;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.builtins.NativeFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>25 Control Abstraction Objects</h1><br>
 * <h2>25.1 Iteration</h2>
 * <ul>
 * <li>25.1.2 The %IteratorPrototype % Object
 * </ul>
 */
public final class IteratorPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new Iterator prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public IteratorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * Marker class for {@code %IteratorPrototype% [ @@iterator ]}.
     */
    private static final class IteratorPrototypeIterator {
    }

    /**
     * Returns {@code true} if <var>iterator</var> is the built-in {@code %IteratorPrototype%[@@iterator]} function for
     * the requested realm.
     * 
     * @param realm
     *            the function realm
     * @param iterator
     *            the iterator function
     * @return {@code true} if <var>iterator</var> is the built-in {@code %IteratorPrototype%[@@iterator]} function
     */
    public static boolean isBuiltinIterator(Realm realm, Object iterator) {
        return NativeFunction.isNative(realm, iterator, IteratorPrototypeIterator.class);
    }

    /**
     * Properties of Iterator Prototype
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 25.1.2.1.1 %IteratorPrototype% [ @@iterator ] ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the this-value
         */
        @Function(name = "[Symbol.iterator]", symbol = BuiltinSymbol.iterator, arity = 0,
                nativeId = IteratorPrototypeIterator.class)
        public static Object iterator(ExecutionContext cx, Object thisValue) {
            return thisValue;
        }
    }
}
