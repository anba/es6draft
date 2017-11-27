/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToNumber;
import static com.github.anba.es6draft.runtime.internal.Errors.newURIError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.Eval.indirectEval;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.CompatibilityExtension;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.StrBuilder;
import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>18 The Global Object</h1>
 * <ul>
 * <li>18.1 Value Properties of the Global Object
 * <li>18.2 Function Properties of the Global Object
 * <li>18.3 Constructor Properties of the Global Object
 * <li>18.4 Other Properties of the Global Object
 * </ul>
 */
public final class GlobalObject extends OrdinaryObject implements Initializable {
    private final Realm realm;

    /**
     * Constructs a new Global object.
     * 
     * @param realm
     *            the realm object
     */
    public GlobalObject(Realm realm) {
        super(realm);
        this.realm = realm;
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, ValueProperties.class);
        createProperties(realm, this, FunctionProperties.class);
        createProperties(realm, this, ConstructorProperties.class);
        createProperties(realm, this, OtherProperties.class);
        if (realm.getRuntimeContext().isEnabled(CompatibilityOption.System)
                || realm.getRuntimeContext().isEnabled(CompatibilityOption.SystemGlobal)) {
            createProperties(realm, this, SystemProperty.class);
        }
        createProperties(realm, this, SIMDProperty.class);
        createProperties(realm, this, ObservableProperty.class);
        createProperties(realm, this, ZoneProperty.class);
        createProperties(realm, this, GlobalProperty.class);
        createProperties(realm, this, BigIntProperties.class);
        createProperties(realm, this, AdditionalProperties.class);
    }

    /**
     * Returns the {@link Realm} of this global object.
     * 
     * @return the realm instance
     */
    public Realm getRealm() {
        return realm;
    }

    /**
     * 18.1 Value Properties of the Global Object
     */
    public enum ValueProperties {
        ;

        /**
         * 18.1.2 NaN
         */
        @Value(name = "NaN", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Double NaN = Double.NaN;

        /**
         * 18.1.1 Infinity
         */
        @Value(name = "Infinity", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Double Infinity = Double.POSITIVE_INFINITY;

        /**
         * 18.1.3 undefined
         */
        @Value(name = "undefined", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Undefined undefined = UNDEFINED;
    }

    /**
     * 18.2 Function Properties of the Global Object
     */
    public enum FunctionProperties {
        ;

        /**
         * 18.2.1 eval (x)
         * 
         * @param cx
         *            the execution context
         * @param caller
         *            the caller context
         * @param thisValue
         *            the function this-value
         * @param args
         *            the arguments
         * @return the evaluation result
         */
        @Function(name = "eval", arity = 1)
        public static Object eval(ExecutionContext cx, ExecutionContext caller, Object thisValue, Object... args) {
            /* steps 1-3 (not applicable) */
            /* step 4 */
            return indirectEval(cx, caller, args);
        }

        /**
         * 18.2.2 isFinite (number)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param number
         *            the number value
         * @return {@code true} if the argument is a finite number
         */
        @Function(name = "isFinite", arity = 1)
        public static Object isFinite(ExecutionContext cx, Object thisValue, Object number) {
            /* step 1 */
            double num = ToNumber(cx, number);
            /* step 2 */
            if (Double.isNaN(num) || Double.isInfinite(num)) {
                return false;
            }
            /* step 3 */
            return true;
        }

        /**
         * 18.2.3 isNaN (number)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param number
         *            the number value
         * @return {@code true} if the number is the NaN value
         */
        @Function(name = "isNaN", arity = 1)
        public static Object isNaN(ExecutionContext cx, Object thisValue, Object number) {
            /* step 1 */
            double num = ToNumber(cx, number);
            /* step 2 */
            if (Double.isNaN(num)) {
                return true;
            }
            /* step 3 */
            return false;
        }

        /**
         * 18.2.4 parseFloat (string)
         * 
         * @param cx
         *            the execution context
         * @return the parsed number
         */
        @Value(name = "parseFloat")
        public static Object parseFloat(ExecutionContext cx) {
            return Get(cx, cx.getIntrinsic(Intrinsics.Number), "parseFloat");
        }

        /**
         * 18.2.5 parseInt (string , radix)
         * 
         * @param cx
         *            the execution context
         * @return the parsed integer
         */
        @Value(name = "parseInt")
        public static Object parseInt(ExecutionContext cx) {
            return Get(cx, cx.getIntrinsic(Intrinsics.Number), "parseInt");
        }

        /**
         * 18.2.6 URI Handling Functions<br>
         * 18.2.6.2 decodeURI (encodedURI)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param encodedURI
         *            the encoded URI
         * @return the decoded URI
         */
        @Function(name = "decodeURI", arity = 1)
        public static Object decodeURI(ExecutionContext cx, Object thisValue, Object encodedURI) {
            /* step 1 */
            String uriString = ToFlatString(cx, encodedURI);
            /* steps 2-3 */
            String decoded = URIFunctions.decodeURI(cx, uriString);
            if (decoded == null) {
                throw newURIError(cx, Messages.Key.MalformedURI);
            }
            return decoded;
        }

        /**
         * 18.2.6 URI Handling Functions<br>
         * 18.2.6.3 decodeURIComponent (encodedURIComponent)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param encodedURIComponent
         *            the encoded URI component
         * @return the decoded URI component
         */
        @Function(name = "decodeURIComponent", arity = 1)
        public static Object decodeURIComponent(ExecutionContext cx, Object thisValue, Object encodedURIComponent) {
            /* step 1 */
            String componentString = ToFlatString(cx, encodedURIComponent);
            /* steps 2-3 */
            String decoded = URIFunctions.decodeURIComponent(cx, componentString);
            if (decoded == null) {
                throw newURIError(cx, Messages.Key.MalformedURI);
            }
            return decoded;
        }

        /**
         * 18.2.6 URI Handling Functions<br>
         * 18.2.6.4 encodeURI (uri)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param uri
         *            the URI
         * @return the encoded URI
         */
        @Function(name = "encodeURI", arity = 1)
        public static Object encodeURI(ExecutionContext cx, Object thisValue, Object uri) {
            /* step 1 */
            String uriString = ToFlatString(cx, uri);
            /* steps 2-3 */
            String encoded = URIFunctions.encodeURI(cx, uriString);
            if (encoded == null) {
                throw newURIError(cx, Messages.Key.MalformedURI);
            }
            return encoded;
        }

        /**
         * 18.2.6 URI Handling Functions<br>
         * 18.2.6.5 encodeURIComponent (uriComponent)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param uriComponent
         *            the URI component
         * @return the encoded URI component
         */
        @Function(name = "encodeURIComponent", arity = 1)
        public static Object encodeURIComponent(ExecutionContext cx, Object thisValue, Object uriComponent) {
            /* step 1 */
            String componentString = ToFlatString(cx, uriComponent);
            /* steps 2-3 */
            String encoded = URIFunctions.encodeURIComponent(cx, componentString);
            if (encoded == null) {
                throw newURIError(cx, Messages.Key.MalformedURI);
            }
            return encoded;
        }
    }

    /**
     * 18.3 Constructor Properties of the Global Object
     */
    public enum ConstructorProperties {
        ;

        /**
         * 18.3.1 Array ( . . . )
         */
        @Value(name = "Array")
        public static final Intrinsics Array = Intrinsics.Array;

        /**
         * 18.3.2 ArrayBuffer ( . . . )
         */
        @Value(name = "ArrayBuffer")
        public static final Intrinsics ArrayBuffer = Intrinsics.ArrayBuffer;

        /**
         * 18.3.3 Boolean ( . . . )
         */
        @Value(name = "Boolean")
        public static final Intrinsics Boolean = Intrinsics.Boolean;

        /**
         * 18.3.4 DataView ( . . . )
         */
        @Value(name = "DataView")
        public static final Intrinsics DataView = Intrinsics.DataView;

        /**
         * 18.3.5 Date ( . . . )
         */
        @Value(name = "Date")
        public static final Intrinsics Date = Intrinsics.Date;

        /**
         * 18.3.6 Error ( . . . )
         */
        @Value(name = "Error")
        public static final Intrinsics Error = Intrinsics.Error;

        /**
         * 18.3.7 EvalError ( . . . )
         */
        @Value(name = "EvalError")
        public static final Intrinsics EvalError = Intrinsics.EvalError;

        /**
         * 18.3.8 Float32Array ( . . . )
         */
        @Value(name = "Float32Array")
        public static final Intrinsics Float32Array = Intrinsics.Float32Array;

        /**
         * 18.3.9 Float64Array ( . . . )
         */
        @Value(name = "Float64Array")
        public static final Intrinsics Float64Array = Intrinsics.Float64Array;

        /**
         * 18.3.10 Function ( . . . )
         */
        @Value(name = "Function")
        public static final Intrinsics Function = Intrinsics.Function;

        /**
         * 18.3.11 Int8Array ( . . . )
         */
        @Value(name = "Int8Array")
        public static final Intrinsics Int8Array = Intrinsics.Int8Array;

        /**
         * 18.3.12 Int16Array ( . . . )
         */
        @Value(name = "Int16Array")
        public static final Intrinsics Int16Array = Intrinsics.Int16Array;

        /**
         * 18.3.13 Int32Array ( . . . )
         */
        @Value(name = "Int32Array")
        public static final Intrinsics Int32Array = Intrinsics.Int32Array;

        /**
         * 18.3.14 Map ( . . . )
         */
        @Value(name = "Map")
        public static final Intrinsics Map = Intrinsics.Map;

        /**
         * 18.3.15 Number ( . . . )
         */
        @Value(name = "Number")
        public static final Intrinsics Number = Intrinsics.Number;

        /**
         * 18.3.16 Object ( . . . )
         */
        @Value(name = "Object")
        public static final Intrinsics Object = Intrinsics.Object;

        /**
         * 18.3.17 Proxy ( . . . )
         */
        @Value(name = "Proxy")
        public static final Intrinsics Proxy = Intrinsics.Proxy;

        /**
         * 18.3.18 Promise ( . . . )
         */
        @Value(name = "Promise")
        public static final Intrinsics Promise = Intrinsics.Promise;

        /**
         * 18.3.19 RangeError ( . . . )
         */
        @Value(name = "RangeError")
        public static final Intrinsics RangeError = Intrinsics.RangeError;

        /**
         * 18.3.20 ReferenceError ( . . . )
         */
        @Value(name = "ReferenceError")
        public static final Intrinsics ReferenceError = Intrinsics.ReferenceError;

        /**
         * 18.3.21 RegExp ( . . . )
         */
        @Value(name = "RegExp")
        public static final Intrinsics RegExp = Intrinsics.RegExp;

        /**
         * 18.3.22 Set ( . . . )
         */
        @Value(name = "Set")
        public static final Intrinsics Set = Intrinsics.Set;

        @Value(name = "SharedArrayBuffer")
        public static final Intrinsics SharedArrayBuffer = Intrinsics.SharedArrayBuffer;

        /**
         * 18.3.23 String ( . . . )
         */
        @Value(name = "String")
        public static final Intrinsics String = Intrinsics.String;

        /**
         * 18.3.24 Symbol ( . . . )
         */
        @Value(name = "Symbol")
        public static final Intrinsics Symbol = Intrinsics.Symbol;

        /**
         * 18.3.25 SyntaxError ( . . . )
         */
        @Value(name = "SyntaxError")
        public static final Intrinsics SyntaxError = Intrinsics.SyntaxError;

        /**
         * 18.3.26 TypeError ( . . . )
         */
        @Value(name = "TypeError")
        public static final Intrinsics TypeError = Intrinsics.TypeError;

        /**
         * 18.3.27 UInt8Array ( . . . )
         */
        @Value(name = "Uint8Array")
        public static final Intrinsics Uint8Array = Intrinsics.Uint8Array;

        /**
         * 18.3.28 UInt8ClampedArray ( . . . )
         */
        @Value(name = "Uint8ClampedArray")
        public static final Intrinsics Uint8ClampedArray = Intrinsics.Uint8ClampedArray;

        /**
         * 18.3.29 UInt16Array ( . . . )
         */
        @Value(name = "Uint16Array")
        public static final Intrinsics Uint16Array = Intrinsics.Uint16Array;

        /**
         * 18.3.30 UInt32Array ( . . . )
         */
        @Value(name = "Uint32Array")
        public static final Intrinsics Uint32Array = Intrinsics.Uint32Array;

        /**
         * 18.3.31 URIError ( . . . )
         */
        @Value(name = "URIError")
        public static final Intrinsics URIError = Intrinsics.URIError;

        /**
         * 18.3.32 WeakMap ( . . . )
         */
        @Value(name = "WeakMap")
        public static final Intrinsics WeakMap = Intrinsics.WeakMap;

        /**
         * 18.3.33 WeakSet ( . . . )
         */
        @Value(name = "WeakSet")
        public static final Intrinsics WeakSet = Intrinsics.WeakSet;

        // InternalError
        @Value(name = "InternalError")
        public static final Intrinsics InternalError = Intrinsics.InternalError;
    }

    /**
     * 18.4 Other Properties of the Global Object
     */
    public enum OtherProperties {
        ;

        @Value(name = "Atomics")
        public static final Intrinsics Atomics = Intrinsics.Atomics;

        /**
         * 18.4.1 JSON
         */
        @Value(name = "JSON")
        public static final Intrinsics JSON = Intrinsics.JSON;

        /**
         * 18.4.2 Math
         */
        @Value(name = "Math")
        public static final Intrinsics Math = Intrinsics.Math;

        /**
         * 18.4.3 Reflect
         */
        @Value(name = "Reflect")
        public static final Intrinsics Reflect = Intrinsics.Reflect;

        // Internationalization API

        @Value(name = "Intl")
        public static final Intrinsics Intl = Intrinsics.Intl;
    }

    public enum SystemProperty {
        ;

        @Value(name = "System")
        public static final Intrinsics System = Intrinsics.System;
    }

    @CompatibilityExtension(CompatibilityOption.SIMD)
    public enum SIMDProperty {
        ;

        @Value(name = "SIMD")
        public static final Intrinsics SIMD = Intrinsics.SIMD;
    }

    @CompatibilityExtension(CompatibilityOption.Observable)
    public enum ObservableProperty {
        ;

        @Value(name = "Observable")
        public static final Intrinsics Observable = Intrinsics.Observable;
    }

    @CompatibilityExtension(CompatibilityOption.Zones)
    public enum ZoneProperty {
        ;

        @Value(name = "Zone")
        public static final Intrinsics Zone = Intrinsics.Zone;
    }

    @CompatibilityExtension(CompatibilityOption.GlobalProperty)
    public enum GlobalProperty {
        ;

        /**
         * global
         * 
         * @param cx
         *            the execution context
         * @return the global object
         */
        @Value(name = "global", attributes = @Attributes(writable = true, enumerable = false, configurable = true))
        public static Object global(ExecutionContext cx) {
            return cx.getRealm().getGlobalThis();
        }
    }

    @CompatibilityExtension(CompatibilityOption.BigInt)
    public enum BigIntProperties {
        ;

        @Value(name = "BigInt")
        public static final Intrinsics BigInt = Intrinsics.BigInt;

        /**
         * BigInt64Array ( . . . )
         */
        @Value(name = "BigInt64Array")
        public static final Intrinsics BigInt64Array = Intrinsics.BigInt64Array;

        /**
         * BigUInt64Array ( . . . )
         */
        @Value(name = "BigUint64Array")
        public static final Intrinsics BigUint64Array = Intrinsics.BigUint64Array;
    }

    /**
     * B.2.1 Additional Properties of the Global Object
     */
    @CompatibilityExtension(CompatibilityOption.GlobalObject)
    public enum AdditionalProperties {
        ;

        private static int fromHexDigit(char c) {
            if (c >= '0' && c <= '9')
                return (c - '0');
            if (c >= 'A' && c <= 'F')
                return (c - 'A') + 10;
            if (c >= 'a' && c <= 'f')
                return (c - 'a') + 10;
            return -1;
        }

        private static char toHexDigit(int i, int shift) {
            i = (i >> shift) & 0b1111;
            return (char) (i + (i < 0x0A ? '0' : ('A' - 10)));
        }

        /**
         * B.2.1.1 escape (string)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param string
         *            the string
         * @return the escaped string
         */
        @Function(name = "escape", arity = 1)
        public static Object escape(ExecutionContext cx, Object thisValue, Object string) {
            /* step 1 */
            String s = ToFlatString(cx, string);
            /* step 2 */
            int length = s.length();
            /* step 3 */
            StrBuilder r = new StrBuilder(cx, length);
            /* steps 4-5 */
            for (int k = 0; k < length; ++k) {
                char c = s.charAt(k);
                if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '@' || c == '*'
                        || c == '_' || c == '+' || c == '-' || c == '.' || c == '/') {
                    r.append(c);
                } else if (c < 256) {
                    r.append('%').append(toHexDigit(c, 4)).append(toHexDigit(c, 0));
                } else {
                    r.append("%u").append(toHexDigit(c, 12)).append(toHexDigit(c, 8)).append(toHexDigit(c, 4))
                            .append(toHexDigit(c, 0));
                }
            }
            /* step 6 */
            return r.toString();
        }

        /**
         * B.2.1.2 unescape (string)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param string
         *            the string
         * @return the unescaped string
         */
        @Function(name = "unescape", arity = 1)
        public static Object unescape(ExecutionContext cx, Object thisValue, Object string) {
            /* step 1 */
            String s = ToFlatString(cx, string);
            /* step 2 */
            int length = s.length();
            /* step 3 */
            StrBuilder r = new StrBuilder(cx, length);
            /* steps 4-5 */
            for (int k = 0; k < length; ++k) {
                char c = s.charAt(k);
                if (c == '%') {
                    if (k <= length - 6 && s.charAt(k + 1) == 'u') {
                        char c2 = s.charAt(k + 2);
                        char c3 = s.charAt(k + 3);
                        char c4 = s.charAt(k + 4);
                        char c5 = s.charAt(k + 5);
                        int h = fromHexDigit(c2) << 12 | fromHexDigit(c3) << 8 | fromHexDigit(c4) << 4
                                | fromHexDigit(c5);
                        if (h >= 0) {
                            k += 5;
                            c = (char) h;
                        }
                    } else if (k <= length - 3) {
                        char c1 = s.charAt(k + 1);
                        char c2 = s.charAt(k + 2);
                        int h = fromHexDigit(c1) << 4 | fromHexDigit(c2);
                        if (h >= 0) {
                            k += 2;
                            c = (char) h;
                        }
                    }
                }
                r.append(c);
            }
            /* step 6 */
            return r.toString();
        }
    }

    /**
     * 18.2.6 URI Handling Functions
     */
    private static final class URIFunctions {
        private URIFunctions() {
        }

        /**
         * 18.2.6.2 decodeURI (encodedURI)
         * 
         * @param cx
         *            the execution context
         * @param encodedURI
         *            the encoded URI
         * @return the decoded URI or {@code null} if invalid
         */
        public static String decodeURI(ExecutionContext cx, String encodedURI) {
            return decode(cx, encodedURI, RESERVED_LO | HASH, RESERVED_HI);
        }

        /**
         * 18.2.6.3 decodeURIComponent (encodedURIComponent)
         * 
         * @param cx
         *            the execution context
         * @param encodedURIComponent
         *            the encoded URI component
         * @return the decoded URI component or {@code null} if invalid
         */
        public static String decodeURIComponent(ExecutionContext cx, String encodedURIComponent) {
            return decode(cx, encodedURIComponent, 0, 0);
        }

        /**
         * 18.2.6.4 encodeURI (uri)
         * 
         * @param cx
         *            the execution context
         * @param uri
         *            the URI
         * @return the encoded URI or {@code null} if invalid
         */
        public static String encodeURI(ExecutionContext cx, String uri) {
            return encode(cx, uri, RESERVED_LO | UNESCAPED_LO | HASH, RESERVED_HI | UNESCAPED_HI);
        }

        /**
         * 18.2.6.5 encodeURIComponent (uriComponent)
         * 
         * @param cx
         *            the execution context
         * @param uriComponent
         *            the URI component
         * @return the encoded URI component or {@code null} if invalid
         */
        public static String encodeURIComponent(ExecutionContext cx, String uriComponent) {
            return encode(cx, uriComponent, UNESCAPED_LO, UNESCAPED_HI);
        }

        // reserved = ; / ? : @ & = + $ ,
        private static final long RESERVED_LO = 0b10101100_00000000_10011000_01010000_00000000_00000000_00000000_00000000L;
        private static final long RESERVED_HI = 0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00000001L;

        // alpha = a-z A-Z
        private static final long ALPHA_LO = 0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L;
        private static final long ALPHA_HI = 0b00000111_11111111_11111111_11111110_00000111_11111111_11111111_11111110L;

        // digit = 0-9
        private static final long DIGIT_LO = 0b00000011_11111111_00000000_00000000_00000000_00000000_00000000_00000000L;
        private static final long DIGIT_HI = 0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L;

        // mark = - _ . ! ~ * ' ( )
        private static final long MARK_LO = 0b00000000_00000000_01100111_10000010_00000000_00000000_00000000_00000000L;
        private static final long MARK_HI = 0b01000000_00000000_00000000_00000000_10000000_00000000_00000000_00000000L;

        // unescaped = alpha | digit | mark
        private static final long UNESCAPED_LO = ALPHA_LO | DIGIT_LO | MARK_LO;
        private static final long UNESCAPED_HI = ALPHA_HI | DIGIT_HI | MARK_HI;

        private static final long HASH = 0b00000000_00000000_00000000_00001000_00000000_00000000_00000000_00000000L;

        static {
            assert HASH == low('#');
            assert RESERVED_LO == low(";/?:&=+$,");
            assert RESERVED_HI == high("@");

            assert ALPHA_LO == low("");
            assert ALPHA_HI == high("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");

            assert DIGIT_LO == low("0123456789");
            assert DIGIT_HI == high("");

            assert MARK_LO == low("-.!*'()");
            assert MARK_HI == high("_~");
        }

        private static int readNibble(char c) {
            if (c >= 'a') {
                return (c <= 'f') ? (c - ('a' - 10)) : -1;
            }
            if (c >= 'A') {
                return (c <= 'F') ? (c - ('A' - 10)) : -1;
            }
            return (c >= '0' && c <= '9') ? (c - '0') : -1;
        }

        private static int readByte(String s, int i) {
            if (s.charAt(i) != '%')
                return -1;
            return (readNibble(s.charAt(i + 1)) << 4) | readNibble(s.charAt(i + 2));
        }

        private static void writeByte(StrBuilder sb, int b) {
            int b0 = (b >> 4) & 0b1111;
            int b1 = b & 0b1111;
            b0 = b0 + (b0 < 0x0A ? '0' : ('A' - 10));
            b1 = b1 + (b1 < 0x0A ? '0' : ('A' - 10));
            sb.append('%').append((char) b0).append((char) b1);
        }

        /**
         * 18.2.6.1.1 Runtime Semantics: Encode ( string, unescapedSet )
         * <p>
         * Returns the encoded string or {@code null} on error.
         * 
         * @param cx
         *            the execution context
         * @param s
         *            the string
         * @param low
         *            the low bit set
         * @param high
         *            the high bit set
         * @return the encoded string
         */
        private static String encode(ExecutionContext cx, String s, long low, long high) {
            final int length = s.length();
            int j = 0;
            StrBuilder sb = null;
            for (int i = 0; i < length; ++i) {
                char c = s.charAt(i);
                if (masked(c, low, high)) {
                    continue;
                }
                if (sb == null) {
                    // 10 = 5 * encoded ASCII or 1 * encoded supplementary character
                    sb = new StrBuilder(cx, length + 10);
                }
                if (j < i) {
                    sb.append(s, j, i);
                }
                if (c <= 0x7F) {
                    writeByte(sb, c);
                } else if (c <= 0x7FF) {
                    writeByte(sb, 0b11000000 | ((c >> 6) & 0b11111));
                    writeByte(sb, 0b10000000 | (c & 0b111111));
                } else if (c <= 0xD7FF || c >= 0xE000) {
                    writeByte(sb, 0b11100000 | ((c >> 12) & 0b1111));
                    writeByte(sb, 0b10000000 | ((c >> 6) & 0b111111));
                    writeByte(sb, 0b10000000 | (c & 0b111111));
                } else if (Character.isHighSurrogate(c) && i + 1 < length) {
                    char d = s.charAt(i + 1);
                    if (!Character.isLowSurrogate(d)) {
                        // lone high surrogate
                        return null;
                    }
                    int cp = Character.toCodePoint(c, d);
                    writeByte(sb, 0b11110000 | ((cp >> 18) & 0b111));
                    writeByte(sb, 0b10000000 | ((cp >> 12) & 0b111111));
                    writeByte(sb, 0b10000000 | ((cp >> 6) & 0b111111));
                    writeByte(sb, 0b10000000 | (cp & 0b111111));
                    // Read two chars, increment i accordingly.
                    i += 1;
                } else {
                    // lone surrogate
                    return null;
                }
                j = i + 1;
            }
            if (sb == null) {
                return s;
            }
            if (j < length) {
                sb.append(s, j, length);
            }
            return sb.toString();
        }

        /**
         * 18.2.6.1.2 Runtime Semantics: Decode ( string, reservedSet )
         * <p>
         * Returns the decoded string or {@code null} on error.
         * 
         * @param cx
         *            the execution context
         * @param s
         *            the string
         * @param low
         *            the low bit set
         * @param high
         *            the high bit set
         * @return the decoded string
         */
        private static String decode(ExecutionContext cx, String s, long low, long high) {
            int i = s.indexOf('%');
            if (i < 0) {
                return s;
            }
            final int length = s.length();
            int j = 0;
            StrBuilder sb = null;
            while (i >= 0) {
                if (i + 2 >= length)
                    return null;
                int c0 = readByte(s, i);
                if (c0 < 0)
                    return null;
                int cp;
                if (c0 <= 0b01111111) {
                    // US-ASCII
                    if (masked(c0, low, high)) {
                        // leave as-is
                        i = s.indexOf('%', i + 3);
                        continue;
                    }
                    cp = (char) c0;
                } else {
                    cp = decodeNonASCII(c0, s, i, length);
                    if (cp < 0)
                        return null;
                }
                int k;
                if (c0 <= 0b10111111) {
                    // US-ASCII
                    // or: illegal (byte sequence part)
                    k = 3;
                } else if (c0 <= 0b11011111) {
                    // two byte sequence
                    // or: illegal (two byte encoding for US-ASCII)
                    k = 6;
                } else if (c0 <= 0b11101111) {
                    // three byte sequence
                    k = 9;
                } else {
                    // four byte sequence
                    // or: illegal (cf. RFC-3629)
                    k = 12;
                }
                if (sb == null) {
                    if (i == 0 && k == length) {
                        // Single character escape
                        return Strings.fromCodePoint(cp);
                    }
                    sb = new StrBuilder(cx, length);
                }
                if (j < i) {
                    // append substring before '%'
                    sb.append(s, j, i);
                }
                sb.appendCodePoint(cp);
                j = i + k;
                i = s.indexOf('%', j);
            }
            if (sb == null) {
                return s;
            }
            if (j < length) {
                // append remaining substring
                sb.append(s, j, length);
            }
            return sb.toString();
        }

        private static int decodeNonASCII(int c0, String s, int i, int len) {
            assert c0 > 0b01111111 : "Caller should handle US-ASCII";
            if (c0 <= 0b10111111) {
                // illegal (byte sequence part)
                return -1;
            } else if (c0 <= 0b11000001) {
                // illegal (two byte encoding for US-ASCII)
                return -1;
            } else if (c0 <= 0b11011111) {
                // two byte sequence
                if (!(i + 2 + 3 < len))
                    return -1;
                int c1 = readByte(s, i + 3);
                if (c1 < 0x80 || c1 > 0xBF)
                    return -1;
                return (char) ((c0 & 0b11111) << 6 | (c1 & 0b111111));
            } else if (c0 <= 0b11101111) {
                // three byte sequence
                if (!(i + 2 + 6 < len))
                    return -1;
                int c1 = readByte(s, i + 3);
                int c2 = readByte(s, i + 6);
                if (c0 == 0b11100000 ? (c1 < 0xA0 || c1 > 0xBF)
                        : c0 == 0b11101101 ? (c1 < 0x80 || c1 > 0x9F) : (c1 < 0x80 || c1 > 0xBF))
                    return -1;
                if (c2 < 0x80 || c2 > 0xBF)
                    return -1;
                return (char) ((c0 & 0b1111) << 12 | (c1 & 0b111111) << 6 | (c2 & 0b111111));
            } else if (c0 <= 0b11110100) {
                // four byte sequence
                if (!(i + 2 + 9 < len))
                    return -1;
                int c1 = readByte(s, i + 3);
                int c2 = readByte(s, i + 6);
                int c3 = readByte(s, i + 9);
                if (c0 == 0b11110000 ? (c1 < 0x90 || c1 > 0xBF)
                        : c0 == 0b11110100 ? (c1 < 0x80 || c1 > 0x8F) : (c1 < 0x80 || c1 > 0xBF))
                    return -1;
                if (c2 < 0x80 || c3 < 0x80 || c2 > 0xBF || c3 > 0xBF)
                    return -1;
                int cp = ((c0 & 0b111) << 18 | (c1 & 0b111111) << 12 | (c2 & 0b111111) << 6 | (c3 & 0b111111));
                assert cp <= Character.MAX_CODE_POINT;
                return cp;
            } else {
                // illegal (cf. RFC-3629)
                return -1;
            }
        }

        /* embedded BitMaskUtil */

        private static boolean masked(char c, long low, long high) {
            return (c != 0) && ((c < 64) ? ((1L << c) & low) != 0 : (c < 128) && ((1L << (c - 64)) & high) != 0);
        }

        private static boolean masked(int c, long low, long high) {
            return (c != 0) && ((c < 64) ? ((1L << c) & low) != 0 : (c < 128) && ((1L << (c - 64)) & high) != 0);
        }

        private static long low(String s) {
            return low(s.toCharArray());
        }

        private static long high(String s) {
            return high(s.toCharArray());
        }

        private static long low(char[] cs) {
            long lo = 0;
            for (char c : cs) {
                assert c < 64;
                lo |= (1L << c);
            }
            return lo;
        }

        private static long high(char[] cs) {
            long hi = 0;
            for (char c : cs) {
                assert c >= 64 && c < 128;
                hi |= (1L << (c - 64));
            }
            return hi;
        }

        private static long low(char c) {
            assert c < 64;
            return (1L << c);
        }

        @SuppressWarnings("unused")
        private static long high(char c) {
            assert c >= 64 && c < 128;
            return (1L << (c - 64));
        }
    }
}
