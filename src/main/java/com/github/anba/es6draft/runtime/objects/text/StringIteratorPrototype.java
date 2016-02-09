/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.text;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateIterResultObject;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.builtins.NativeFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>21 Text Processing</h1><br>
 * <h2>21.1 String Objects</h2>
 * <ul>
 * <li>21.1.5 String Iterator Objects
 * </ul>
 */
public final class StringIteratorPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new String Iterator prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public StringIteratorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * 21.1.5.1 CreateStringIterator Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param string
     *            the string value
     * @return the new string iterator
     */
    public static OrdinaryObject CreateStringIterator(ExecutionContext cx, String string) {
        /* step 1 (not applicable) */
        /* steps 2-5 */
        return new StringIteratorObject(cx.getRealm(), string, cx.getIntrinsic(Intrinsics.StringIteratorPrototype));
    }

    /**
     * 21.1.5.1 CreateStringIterator Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param string
     *            the string value
     * @param index
     *            the start index
     * @return the new string iterator
     */
    public static OrdinaryObject CreateStringIterator(ExecutionContext cx, String string, int index) {
        assert 0 <= index && index <= string.length();
        /* step 1 (not applicable) */
        /* steps 2-5 */
        return new StringIteratorObject(cx.getRealm(), string, index,
                cx.getIntrinsic(Intrinsics.StringIteratorPrototype));
    }

    /**
     * Marker class for {@code %StringIteratorPrototype%.next}.
     */
    private static final class StringIteratorPrototypeNext {
    }

    /**
     * Returns {@code true} if <var>next</var> is the built-in {@code %StringIteratorPrototype%.next} function for the
     * requested realm.
     * 
     * @param realm
     *            the function realm
     * @param next
     *            the next function
     * @return {@code true} if <var>next</var> is the built-in {@code %StringIteratorPrototype%.next} function
     */
    public static boolean isBuiltinNext(Realm realm, Object next) {
        return NativeFunction.isNative(realm, next, StringIteratorPrototypeNext.class);
    }

    /**
     * 21.1.5.2 The %StringIteratorPrototype% Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.IteratorPrototype;

        /**
         * 21.1.5.2.1 %StringIteratorPrototype%.next( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the next iterator result object
         */
        @Function(name = "next", arity = 0, nativeId = StringIteratorPrototypeNext.class)
        public static Object next(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            if (!(thisValue instanceof StringIteratorObject)) {
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            StringIteratorObject iterator = (StringIteratorObject) thisValue;
            /* step 4 */
            String string = iterator.getIteratedString();
            /* step 5 */
            if (string == null) {
                return CreateIterResultObject(cx, UNDEFINED, true);
            }
            /* step 6 */
            int position = iterator.getNextIndex();
            /* step 7 */
            int len = string.length();
            /* step 8 */
            if (position >= len) {
                iterator.setIteratedString(null);
                return CreateIterResultObject(cx, UNDEFINED, true);
            }
            /* steps 9-11 */
            int cp = string.codePointAt(position);
            String resultString = Strings.fromCodePoint(cp);
            /* step 12 */
            int resultSize = Character.charCount(cp);
            /* step 13 */
            iterator.setNextIndex(position + resultSize);
            /* step 14 */
            return CreateIterResultObject(cx, resultString, false);
        }

        /**
         * 21.1.5.2.2 %StringIteratorPrototype% [@@toStringTag]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "String Iterator";
    }
}
