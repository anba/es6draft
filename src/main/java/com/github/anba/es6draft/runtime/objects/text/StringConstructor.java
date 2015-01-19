/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.text;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
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
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Creatable;
import com.github.anba.es6draft.runtime.types.CreateAction;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
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
public final class StringConstructor extends BuiltinConstructor implements Initializable,
        Creatable<StringObject> {
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
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
    }

    @Override
    public StringConstructor clone() {
        return new StringConstructor(getRealm());
    }

    /**
     * 21.1.1.1 String ( value )
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        /* step 1 (omitted) */
        /* steps 2-3 */
        boolean initializing = thisValue instanceof StringObject
                && ((StringObject) thisValue).getStringData() == null;
        /* steps 4-5 */
        CharSequence s;
        if (args.length == 0) {
            /* step 4 */
            s = "";
        } else {
            /* step 5 */
            Object value = args[0];
            if (!initializing && Type.isSymbol(value)) {
                return SymbolDescriptiveString(Type.symbolValue(value));
            }
            s = ToString(calleeContext, value);
        }
        /* step 6 (not applicable) */
        /* step 7 */
        if (initializing) {
            StringObject obj = (StringObject) thisValue;
            if (obj.getStringData() != null) {
                // TODO: This additional check can probably be removed after the new create
                // mechanism is fully specified.
                throw newTypeError(calleeContext, Messages.Key.InitializedObject);
            }
            if (!IsExtensible(calleeContext, obj)) {
                throw newTypeError(calleeContext, Messages.Key.NotExtensible);
            }
            int length = s.length();
            DefinePropertyOrThrow(calleeContext, obj, "length", new PropertyDescriptor(length,
                    false, false, false));
            obj.setStringData(s);
            return obj;
        }
        /* step 8 */
        return s;
    }

    /**
     * 21.1.1.2 new String ( ... argumentsList )
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return Construct(callerContext, this, args);
    }

    private static final class StringCreate implements CreateAction<StringObject> {
        static final CreateAction<StringObject> INSTANCE = new StringCreate();

        @Override
        public StringObject create(ExecutionContext cx, Constructor constructor, Object... args) {
            ScriptObject proto = GetPrototypeFromConstructor(cx, constructor,
                    Intrinsics.StringPrototype);
            return StringCreate(cx, proto);
        }
    }

    @Override
    public CreateAction<StringObject> createAction() {
        return StringCreate.INSTANCE;
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
        public static Object fromCharCode(ExecutionContext cx, Object thisValue,
                Object... codeUnits) {
            /* steps 1-2 */
            int length = codeUnits.length;
            // Optimize:
            if (length == 1) {
                return String.valueOf((char) ToUint16(cx, codeUnits[0]));
            }
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
        public static Object fromCodePoint(ExecutionContext cx, Object thisValue,
                Object... codePoints) {
            /* steps 1-2 */
            int length = codePoints.length;
            // Optimize:
            if (length == 1) {
                double nextCP = ToNumber(cx, codePoints[0]);
                int cp = (int) nextCP;
                if (cp < 0 || cp > 0x10FFFF || nextCP != (double) cp) {
                    throw newRangeError(cx, Messages.Key.InvalidCodePoint);
                }
                if (Character.isBmpCodePoint(cp)) {
                    return String.valueOf((char) cp);
                }
                return String.valueOf(Character.toChars(cp));
            }
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
        public static Object raw(ExecutionContext cx, Object thisValue, Object template,
                Object... substitutions) {
            /* step 1 (not applicable) */
            /* step 2 */
            long numberOfSubstitutions = substitutions.length;
            /* steps 3-4 */
            ScriptObject cooked = ToObject(cx, template);
            /* steps 5-7 */
            Object rawValue = Get(cx, cooked, "raw");
            ScriptObject raw = ToObject(cx, rawValue);
            /* step 8 */
            Object len = Get(cx, raw, "length");
            /* steps 9-10 */
            long literalSegments = ToLength(cx, len);
            /* step 11 */
            if (literalSegments <= 0) {
                return "";
            }
            /* step 12 */
            StringBuilder stringElements = new StringBuilder();
            /* steps 13-14 */
            for (long nextIndex = 0;; ++nextIndex) {
                long nextKey = nextIndex;
                Object next = Get(cx, raw, nextKey);
                CharSequence nextSeg = ToString(cx, next);
                stringElements.append(nextSeg);
                if (nextIndex + 1 == literalSegments) {
                    return stringElements.toString();
                }
                if (nextIndex < numberOfSubstitutions) {
                    next = substitutions[(int) nextIndex];
                } else {
                    next = "";
                }
                CharSequence nextSub = ToString(cx, next);
                stringElements.append(nextSub);
            }
        }
    }
}
