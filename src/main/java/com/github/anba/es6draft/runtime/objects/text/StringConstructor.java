/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.text;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticString.StringCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;

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
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.github.anba.es6draft.runtime.types.builtins.ExoticString;

/**
 * <h1>21 Text Processing</h1><br>
 * <h2>21.1 String Objects</h2>
 * <ul>
 * <li>21.1.1 The String Constructor
 * <li>21.1.2 Properties of the String Constructor
 * </ul>
 */
public final class StringConstructor extends BuiltinConstructor implements Initialisable {
    public StringConstructor(Realm realm) {
        super(realm, "String");
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    /**
     * 21.1.1.1 String ( value = "" )
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        /* step 1 (omitted) */
        /* steps 2-4 */
        CharSequence s = args.length > 0 ? ToString(calleeContext, args[0]) : "";
        /* step 5 */
        if (thisValue instanceof ExoticString) {
            ExoticString obj = (ExoticString) thisValue;
            if (obj.getStringData() == null) {
                int length = s.length();
                DefinePropertyOrThrow(calleeContext, obj, "length", new PropertyDescriptor(length,
                        false, false, false));
                obj.setStringData(s);
                return obj;
            }
        }
        /* step 6 */
        return s;
    }

    /**
     * 21.1.1.2 new String ( ... argumentsList )
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return Construct(callerContext, this, args);
    }

    /**
     * 21.1.2 Properties of the String Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = "String";

        /**
         * 21.1.2.3 String.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.StringPrototype;

        /**
         * 21.1.2.1 String.fromCharCode ( ...codeUnits)
         */
        @Function(name = "fromCharCode", arity = 1)
        public static Object fromCharCode(ExecutionContext cx, Object thisValue,
                Object... codeUnits) {
            /* steps 1-2 */
            int length = codeUnits.length;
            /* step 3 */
            char elements[] = new char[length];
            /* steps 4-5 */
            for (int nextIndex = 0; nextIndex < length; ++nextIndex) {
                Object next = codeUnits[nextIndex];
                char nextCU = ToUint16(cx, next);
                elements[nextIndex] = nextCU;
            }
            /* step 6 */
            return new String(elements);
        }

        /**
         * 21.1.2.2 String.fromCodePoint ( ...codePoints)
         */
        @Function(name = "fromCodePoint", arity = 0)
        public static Object fromCodePoint(ExecutionContext cx, Object thisValue,
                Object... codePoints) {
            /* steps 1-2 */
            int length = codePoints.length;
            /* step 3 */
            int elements[] = new int[length];
            /* steps 4-5 */
            for (int nextIndex = 0; nextIndex < length; ++nextIndex) {
                Object next = codePoints[nextIndex];
                double nextCP = ToNumber(cx, next);
                if (!SameValue(nextCP, ToInteger(nextCP))) {
                    throw newRangeError(cx, Messages.Key.InvalidCodePoint);
                }
                if (nextCP < 0 || nextCP > 0x10FFFF) {
                    throw newRangeError(cx, Messages.Key.InvalidCodePoint);
                }
                elements[nextIndex] = (int) nextCP;
            }
            /* step 6 */
            return new String(elements, 0, length);
        }

        /**
         * 21.1.2.4 String.raw ( callSite, ...substitutions)
         */
        @Function(name = "raw", arity = 1)
        public static Object raw(ExecutionContext cx, Object thisValue, Object callSite,
                Object... substitutions) {
            /* step 1 (not applicable) */
            /* steps 2-3 */
            ScriptObject cooked = ToObject(cx, callSite);
            /* steps 4-6 */
            Object rawValue = Get(cx, cooked, "raw");
            ScriptObject raw = ToObject(cx, rawValue);
            /* step 7 */
            Object len = Get(cx, raw, "length");
            /* steps 8-9 */
            long literalSegments = ToLength(cx, len);
            /* step 10 */
            if (literalSegments <= 0) {
                return "";
            }
            /* step 11 */
            StringBuilder stringElements = new StringBuilder();
            long substlength = substitutions.length;
            /* steps 12-13 */
            for (long nextIndex = 0;; ++nextIndex) {
                String nextKey = ToString(nextIndex);
                Object next = Get(cx, raw, nextKey);
                CharSequence nextSeg = ToString(cx, next);
                stringElements.append(nextSeg);
                if (nextIndex + 1 == literalSegments) {
                    return stringElements.toString();
                }
                if (nextIndex < substlength) {
                    next = substitutions[(int) nextIndex];
                } else {
                    // rest parameter objects are arrays, out of bounds access delegates to proto
                    next = Get(cx, cx.getIntrinsic(Intrinsics.ArrayPrototype), nextKey);
                }
                CharSequence nextSub = ToString(cx, next);
                stringElements.append(nextSub);
            }
        }

        /**
         * 21.1.2.5 String[ @@create ] ( )
         */
        @Function(name = "[Symbol.create]", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            ScriptObject proto = GetPrototypeFromConstructor(cx, thisValue,
                    Intrinsics.StringPrototype);
            return StringCreate(cx, proto);
        }
    }
}
