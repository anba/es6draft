/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.iteration.IterationAbstractOperations.CreateIterResultObject;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
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
 * <li>21.1.5 String Iterator Object Structure
 * </ul>
 * 
 * TODO: Not yet specified, current implementation based on ArrayIteratorPrototype
 */
public class StringIteratorPrototype extends OrdinaryObject implements Initialisable {
    public StringIteratorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
    }

    /**
     * 21.1.5.3 Properties of String Iterator Instances
     */
    private static class StringIterator extends OrdinaryObject {
        /** [[IteratedString]] */
        String iteratedString;

        /** [[StringIteratorNextIndex]] */
        int nextIndex;

        StringIterator(Realm realm) {
            super(realm);
        }
    }

    private static class StringIteratorAllocator implements ObjectAllocator<StringIterator> {
        static final ObjectAllocator<StringIterator> INSTANCE = new StringIteratorAllocator();

        @Override
        public StringIterator newInstance(Realm realm) {
            return new StringIterator(realm);
        }
    }

    /**
     * 21.1.5.1 CreateStringIterator Abstract Operation
     */
    public static OrdinaryObject CreateStringIterator(ExecutionContext cx, String string) {
        StringIterator iterator = ObjectCreate(cx, Intrinsics.StringIteratorPrototype,
                StringIteratorAllocator.INSTANCE);
        iterator.iteratedString = string;
        iterator.nextIndex = 0;
        return iterator;
    }

    /**
     * 21.1.5.2 The String Iterator Prototype
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 21.1.5.2.1 StringIterator.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Object constructor = UNDEFINED;

        /**
         * 21.1.5.2.2 StringIterator.prototype.next( )
         */
        @Function(name = "next", arity = 0)
        public static Object next(ExecutionContext cx, Object thisValue) {
            if (!Type.isObject(thisValue)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            if (!(thisValue instanceof StringIterator)) {
                throw throwTypeError(cx, Messages.Key.IncompatibleObject);
            }
            StringIterator iterator = (StringIterator) thisValue;
            String string = iterator.iteratedString;
            int index = iterator.nextIndex;
            int len = string.length();
            if (index >= len) {
                iterator.nextIndex = Integer.MAX_VALUE; // = +Infinity
                return CreateIterResultObject(cx, UNDEFINED, true);
            }
            int cp = string.codePointAt(index);
            iterator.nextIndex = index + Character.charCount(cp);
            String result = new String(Character.toChars(cp));
            return CreateIterResultObject(cx, result, false);
        }

        /**
         * 21.1.5.2.3 StringIterator.prototype.@@iterator ()
         */
        @Function(name = "@@iterator", symbol = BuiltinSymbol.iterator, arity = 0)
        public static Object iterator(ExecutionContext cx, Object thisValue) {
            return thisValue;
        }

        /**
         * 21.1.5.2.4 StringIterator.prototype.@@toStringTag
         */
        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = "String Iterator";
    }
}
