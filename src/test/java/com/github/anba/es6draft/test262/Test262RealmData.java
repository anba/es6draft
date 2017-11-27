/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.test262;

import static com.github.anba.es6draft.runtime.AbstractOperations.GetIterator;
import static com.github.anba.es6draft.runtime.AbstractOperations.GetV;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToInt32;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.stream.IntStream;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.RealmData;
import com.github.anba.es6draft.runtime.internal.Properties;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * Initializes the global state for test262 tests.
 */
public final class Test262RealmData extends RealmData {
    Test262RealmData(Realm realm) {
        super(realm);
    }

    @Override
    public void initializeExtensions() {
        getRealm().createGlobalProperties(new GlobalProperties(), GlobalProperties.class);
    }

    public final class GlobalProperties {
        /**
         * global-function: {@code print(message)}
         * 
         * @param cx
         *            the execution context
         * @param messages
         *            the string to print
         */
        @Function(name = "print", arity = 1)
        public void print(ExecutionContext cx, String... messages) {
            PrintWriter writer = cx.getRuntimeContext().getConsole().writer();
            writer.println(Strings.concatWith(' ', messages));
        }

        /**
         * global-value: {@code $262}
         * 
         * @param cx
         *            the execution context
         * @return the global {@code $262} object
         */
        @Value(name = "$262")
        public ScriptObject $262(ExecutionContext cx) {
            OrdinaryObject obj = ObjectCreate(cx, (ScriptObject) null);
            Properties.createProperties(cx, obj, new TestingFunctions(), TestingFunctions.class);
            return obj;
        }

        /**
         * global-function: {@code $buildRegExpUnicodeString(loneCodePoints, ranges)}
         * 
         * @param cx
         *            the execution context
         * @param loneCodePoints
         *            the array of lone code points
         * @param ranges
         *            the array of code point ranges
         * @return the result string
         */
        @Function(name = "$buildRegExpUnicodeString", arity = 2)
        public String buildRegExpUnicodeString(ExecutionContext cx, Object loneCodePoints, Object ranges) {
            StringBuilder sb = new StringBuilder();
            for (Iterator<?> iterator = GetIterator(cx, loneCodePoints); iterator.hasNext();) {
                int codePoint = ToInt32(cx, iterator.next());
                sb.appendCodePoint(codePoint);
            }
            for (Iterator<?> iterator = GetIterator(cx, ranges); iterator.hasNext();) {
                Object range = iterator.next();
                int start = ToInt32(cx, GetV(cx, range, 0));
                int end = ToInt32(cx, GetV(cx, range, 1));
                IntStream.rangeClosed(start, end).forEach(sb::appendCodePoint);
            }
            return sb.toString();
        }
    }
}
