/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.text;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.SymbolPrototype.SymbolDescriptiveString;
import static com.github.anba.es6draft.runtime.types.builtins.StringObject.StringCreate;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.StrBuilder;
import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.github.anba.es6draft.runtime.types.builtins.StringObject;

/**
 * <h1>21 Text Processing</h1><br>
 * <h2>21.1 String Objects</h2>
 * <ul>
 * <li>21.1.1 The String Constructor
 * <li>21.1.2 Properties of the String Constructor
 * </ul>
 */
public final class StringConstructor extends BuiltinConstructor implements Initializable {
    /**
     * Constructs a new String constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public StringConstructor(Realm realm) {
        super(realm, "String", 1);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * 21.1.1.1 String ( value )
     */
    @Override
    public CharSequence call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        /* step 1 */
        if (args.length == 0) {
            return "";
        }
        Object value = args[0];
        /* step 2.a */
        if (Type.isSymbol(value)) {
            return SymbolDescriptiveString(calleeContext, Type.symbolValue(value));
        }
        /* steps 2.b, 3 */
        return ToString(calleeContext, value);
    }

    /**
     * 21.1.1.1 String ( value )
     */
    @Override
    public StringObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        /* steps 1-2 */
        CharSequence s = args.length == 0 ? "" : ToString(calleeContext, args[0]);
        /* step 3 (not applicable) */
        /* step 4 */
        return StringCreate(calleeContext, s,
                GetPrototypeFromConstructor(calleeContext, newTarget, Intrinsics.StringPrototype));
    }

    /**
     * 21.1.2 Properties of the String Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = "String";

        /**
         * 21.1.2.3 String.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.StringPrototype;

        /**
         * 21.1.2.1 String.fromCharCode ( ...codeUnits)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param codeUnits
         *            the unicode character units
         * @return the result string
         */
        @Function(name = "fromCharCode", arity = 1)
        public static Object fromCharCode(ExecutionContext cx, Object thisValue, Object... codeUnits) {
            /* steps 1-2 */
            int length = codeUnits.length;
            // Optimize:
            if (length == 1) {
                return String.valueOf(ToUint16(cx, codeUnits[0]));
            }
            /* step 3 */
            char elements[] = new char[length];
            /* steps 4-5 */
            for (int nextIndex = 0; nextIndex < length; ++nextIndex) {
                /* steps 5.a-b */
                char nextCU = ToUint16(cx, codeUnits[nextIndex]);
                /* step 5.c */
                elements[nextIndex] = nextCU;
            }
            /* step 6 */
            return new String(elements);
        }

        /**
         * 21.1.2.2 String.fromCodePoint ( ...codePoints)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param codePoints
         *            the unicode code points
         * @return the result string
         */
        @Function(name = "fromCodePoint", arity = 1)
        public static Object fromCodePoint(ExecutionContext cx, Object thisValue, Object... codePoints) {
            /* steps 1-2 */
            int length = codePoints.length;
            // Optimize:
            if (length == 1) {
                /* steps 5.a-b */
                double nextCP = ToNumber(cx, codePoints[0]);
                int cp = (int) nextCP;
                /* steps 5.c-d */
                if (cp < 0 || cp > 0x10FFFF || nextCP != (double) cp) {
                    throw newRangeError(cx, Messages.Key.InvalidCodePoint);
                }
                /* steps 5.e, 6 */
                return Strings.fromCodePoint(cp);
            }
            /* step 3 */
            int elements[] = new int[length];
            /* steps 4-5 */
            for (int nextIndex = 0; nextIndex < length; ++nextIndex) {
                /* steps 5.a-b */
                double nextCP = ToNumber(cx, codePoints[nextIndex]);
                int cp = (int) nextCP;
                /* steps 5.c-d */
                if (cp < 0 || cp > 0x10FFFF || nextCP != (double) cp) {
                    throw newRangeError(cx, Messages.Key.InvalidCodePoint);
                }
                /* step 5.e */
                elements[nextIndex] = cp;
            }
            /* step 6 */
            return new String(elements, 0, length);
        }

        /**
         * 21.1.2.4 String.raw ( template , ...substitutions )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param template
         *            the call site object
         * @param substitutions
         *            the string substitutions
         * @return the interpolated string
         */
        @Function(name = "raw", arity = 1)
        public static Object raw(ExecutionContext cx, Object thisValue, Object template, Object... substitutions) {
            /* step 1 (not applicable) */
            /* step 2 */
            long numberOfSubstitutions = substitutions.length;
            /* step 3 */
            ScriptObject cooked = ToObject(cx, template);
            /* step 4 */
            ScriptObject raw = ToObject(cx, Get(cx, cooked, "raw"));
            /* step 5 */
            long literalSegments = ToLength(cx, Get(cx, raw, "length"));
            /* step 6 */
            if (literalSegments <= 0) {
                return "";
            }
            /* step 7 */
            StrBuilder stringElements = new StrBuilder(cx);
            /* steps 8-9 */
            for (long nextIndex = 0;; ++nextIndex) {
                /* steps 9.a-b */
                CharSequence nextSeg = ToString(cx, Get(cx, raw, nextIndex));
                /* step 9.c */
                stringElements.append(nextSeg);
                /* step 9.d */
                if (nextIndex + 1 == literalSegments) {
                    return stringElements.toString();
                }
                /* steps 9.e-h */
                if (nextIndex < numberOfSubstitutions) {
                    CharSequence nextSub = ToString(cx, substitutions[(int) nextIndex]);
                    stringElements.append(nextSub);
                }
            }
        }
    }
}
