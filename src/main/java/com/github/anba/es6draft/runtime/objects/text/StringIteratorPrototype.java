/**
 * Copyright (c) 2012-2014 André Bargull
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
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;
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
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
    }

    /**
     * 21.1.5.3 Properties of String Iterator Instances
     */
    private static final class StringIterator extends OrdinaryObject {
        /** [[IteratedString]] */
        String iteratedString;

        /** [[StringIteratorNextIndex]] */
        int nextIndex;

        StringIterator(Realm realm) {
            super(realm);
        }
    }

    private static final class StringIteratorAllocator implements ObjectAllocator<StringIterator> {
        static final ObjectAllocator<StringIterator> INSTANCE = new StringIteratorAllocator();

        @Override
        public StringIterator newInstance(Realm realm) {
            return new StringIterator(realm);
        }
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
        /* steps 2-4 */
        StringIterator iterator = ObjectCreate(cx, Intrinsics.StringIteratorPrototype,
                StringIteratorAllocator.INSTANCE);
        iterator.iteratedString = string;
        iterator.nextIndex = 0;
        /* step 5 */
        return iterator;
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
        @Function(name = "next", arity = 0)
        public static Object next(ExecutionContext cx, Object thisValue) {
            /* step 1 (omitted) */
            /* step 2 */
            if (!Type.isObject(thisValue)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 3 */
            if (!(thisValue instanceof StringIterator)) {
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            StringIterator iterator = (StringIterator) thisValue;
            /* step 4 */
            String string = iterator.iteratedString;
            /* step 5 */
            if (string == null) {
                return CreateIterResultObject(cx, UNDEFINED, true);
            }
            /* step 6 */
            int position = iterator.nextIndex;
            /* step 7 */
            int len = string.length();
            /* step 8 */
            if (position >= len) {
                iterator.iteratedString = null;
                return CreateIterResultObject(cx, UNDEFINED, true);
            }
            /* steps 9-11 */
            int cp = string.codePointAt(position);
            String resultString = new String(Character.toChars(cp));
            /* step 12 */
            int resultSize = Character.charCount(cp);
            /* step 13 */
            iterator.nextIndex = position + resultSize;
            /* step 14 */
            return CreateIterResultObject(cx, resultString, false);
        }

        /**
         * 21.1.5.2.2 %StringIteratorPrototype% [@@iterator] ()
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the this-value
         */
        @Function(name = "[Symbol.iterator]", symbol = BuiltinSymbol.iterator, arity = 0)
        public static Object iterator(ExecutionContext cx, Object thisValue) {
            return thisValue;
        }

        /**
         * 21.1.5.2.3 %StringIteratorPrototype% [@@toStringTag]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = "String Iterator";
    }
}
