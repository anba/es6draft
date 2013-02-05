/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.throwRangeError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinBrand;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.builtins.ExoticString;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.5 String Objects</h2>
 * <ul>
 * <li>15.5.1 The String Constructor Called as a Function
 * <li>15.5.2 The String Constructor
 * <li>15.5.3 Properties of the String Constructor
 * </ul>
 */
public class StringConstructor extends OrdinaryObject implements Scriptable, Callable, Constructor,
        Initialisable {
    public StringConstructor(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);
        AddRestrictedFunctionProperties(realm, this);
    }

    /**
     * [[BuiltinBrand]]
     */
    @Override
    public BuiltinBrand getBuiltinBrand() {
        return BuiltinBrand.BuiltinFunction;
    }

    @Override
    public String toSource() {
        return "function String() { /* native code */ }";
    }

    /**
     * 15.5.1.1 String ( [ value ] )
     */
    @Override
    public Object call(Object thisValue, Object... args) {
        if (args.length == 0) {
            return "";
        }
        return ToString(realm(), args[0]);
    }

    /**
     * 15.5.2.1 new String ( [ value ] )
     */
    @Override
    public Object construct(Object... args) {
        CharSequence stringData = (args.length > 0 ? ToString(realm(), args[0]) : "");
        ExoticString obj = new ExoticString(realm(), stringData);
        obj.setPrototype(realm().getIntrinsic(Intrinsics.StringPrototype));
        return obj;
    }

    /**
     * 15.5.3 Properties of the String Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final int length = 1;

        /**
         * 15.5.3.1 String.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.StringPrototype;

        /**
         * 15.5.3.2 String.fromCharCode ( ...codeUnits)
         */
        @Function(name = "fromCharCode", arity = 1)
        public static Object fromCharCode(Realm realm, Object thisValue, Object... codeUnits) {
            int length = codeUnits.length;
            char elements[] = new char[length];
            for (int nextIndex = 0; nextIndex < length; ++nextIndex) {
                Object next = codeUnits[nextIndex];
                char nextCU = ToUint16(realm, next);
                elements[nextIndex] = nextCU;
            }
            return new String(elements);
        }

        /**
         * 15.5.3.3 String.fromCodePoint ( ...codePoints)
         */
        @Function(name = "fromCodePoint", arity = 0)
        public static Object fromCodePoint(Realm realm, Object thisValue, Object... codePoints) {
            int length = codePoints.length;
            int elements[] = new int[length];
            for (int nextIndex = 0; nextIndex < length; ++nextIndex) {
                Object next = codePoints[nextIndex];
                double nextCP = ToNumber(realm, next);
                if (!SameValue(nextCP, ToInteger(realm, nextCP))) {
                    throw throwRangeError(realm, "");
                }
                if (nextCP < 0 || nextCP > 0x10FFFF) {
                    throw throwRangeError(realm, "");
                }
                elements[nextIndex] = (int) nextCP;
            }
            return new String(elements, 0, length);
        }

        /**
         * 15.5.3.4 String.raw ( callSite, ...substitutions)
         */
        @Function(name = "raw", arity = 1)
        public static Object raw(Realm realm, Object thisValue, Object callSite,
                Object... substitutions) {
            Scriptable cooked = ToObject(realm, callSite);
            Object rawValue = Get(cooked, "raw");
            Scriptable raw = ToObject(realm, rawValue);
            Object len = Get(raw, "length");
            long literalSegments = ToUint32(realm, len); // FIXME: spec bug (bug 492)
            if (literalSegments == 0) {
                return "";
            }
            long substlength = substitutions.length;
            StringBuilder stringElements = new StringBuilder();
            for (long nextIndex = 0; nextIndex < literalSegments; ++nextIndex) {
                String nextKey = ToString(nextIndex);
                Object next = Get(raw, nextKey);
                CharSequence nextSeg = ToString(realm, next);
                stringElements.append(nextSeg);
                if (nextIndex + 1 == literalSegments) {
                    return stringElements.toString();
                }
                next = (nextIndex < substlength ? substitutions[(int) nextIndex] : UNDEFINED);
                CharSequence nextSub = ToString(realm, next);
                stringElements.append(nextSub);
            }
            // FIXME: spec bug (condition nextIndex < literalSegments is invalid) (Bug 1152)
            assert false : "spec bug";
            return UNDEFINED;
        }
    }
}
