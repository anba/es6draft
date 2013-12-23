/**
 * Copyright (c) 2012-2013 André Bargull
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
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.CompatibilityExtension;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>18 The Global Object</h1>
 * <ul>
 * <li>18.1 Value Properties of the Global Object
 * <li>18.2 Function Properties of the Global Object
 * <li>18.3 URI Handling Function Properties
 * <li>18.4 Constructor Properties of the Global Object
 * <li>18.5 Other Properties of the Global Object
 * </ul>
 */
public class GlobalObject extends OrdinaryObject {
    private final Realm realm;

    public GlobalObject(Realm realm) {
        super(realm);
        this.realm = realm;
    }

    /**
     * Initialise {@code object} with the default properties of the Global Object
     */
    public void defineBuiltinProperties(ExecutionContext cx, ScriptObject object) {
        createProperties(object, cx, ValueProperties.class);
        createProperties(object, cx, FunctionProperties.class);
        createProperties(object, cx, URIHandlingFunctions.class);
        createProperties(object, cx, ConstructorProperties.class);
        createProperties(object, cx, OtherProperties.class);
        createProperties(object, cx, AdditionalProperties.class);
    }

    /**
     * Returns the {@link Realm} of this global object
     */
    public final Realm getRealm() {
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
        @Value(name = "NaN", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Double NaN = Double.NaN;

        /**
         * 18.1.1 Infinity
         */
        @Value(name = "Infinity", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Double Infinity = Double.POSITIVE_INFINITY;

        /**
         * 18.1.3 undefined
         */
        @Value(name = "undefined", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Undefined undefined = UNDEFINED;
    }

    /**
     * 18.2 Function Properties of the Global Object
     */
    public enum FunctionProperties {
        ;

        /**
         * 18.2.1 eval (x)
         */
        @Function(name = "eval", arity = 1)
        public static Object eval(ExecutionContext cx, Object thisValue, Object... args) {
            return indirectEval(cx, args);
        }

        /**
         * 18.2.5 parseInt (string , radix)
         */
        @Value(name = "parseInt")
        public static Object parseInt(ExecutionContext cx) {
            return Get(cx, cx.getIntrinsic(Intrinsics.Number), "parseInt");
        }

        /**
         * 18.2.4 parseFloat (string)
         */
        @Value(name = "parseFloat")
        public static Object parseFloat(ExecutionContext cx) {
            return Get(cx, cx.getIntrinsic(Intrinsics.Number), "parseFloat");
        }

        /**
         * 18.2.3 isNaN (number)
         */
        @Function(name = "isNaN", arity = 1)
        public static Object isNaN(ExecutionContext cx, Object thisValue, Object number) {
            /* steps 1-2 */
            double num = ToNumber(cx, number);
            /* steps 3 */
            if (Double.isNaN(num)) {
                return true;
            }
            /* steps 4 */
            return false;
        }

        /**
         * 18.2.2 isFinite (number)
         */
        @Function(name = "isFinite", arity = 1)
        public static Object isFinite(ExecutionContext cx, Object thisValue, Object number) {
            /* steps 1-2 */
            double num = ToNumber(cx, number);
            /* step 3 */
            if (Double.isNaN(num) || Double.isInfinite(num)) {
                return false;
            }
            /* step 4 */
            return true;
        }
    }

    /**
     * 18.3 URI Handling Functions
     */
    public enum URIHandlingFunctions {
        ;

        /**
         * 18.3.1 decodeURI (encodedURI)
         */
        @Function(name = "decodeURI", arity = 1)
        public static Object decodeURI(ExecutionContext cx, Object thisValue, Object encodedURI) {
            /* steps 1-2 */
            String uriString = ToFlatString(cx, encodedURI);
            /* steps 3-4 */
            String decoded = URIFunctions.decodeURI(uriString);
            if (decoded == null) {
                throw newURIError(cx, Messages.Key.MalformedURI);
            }
            return decoded;
        }

        /**
         * 18.3.2 decodeURIComponent (encodedURIComponent)
         */
        @Function(name = "decodeURIComponent", arity = 1)
        public static Object decodeURIComponent(ExecutionContext cx, Object thisValue,
                Object encodedURIComponent) {
            /* steps 1-2 */
            String componentString = ToFlatString(cx, encodedURIComponent);
            /* steps 3-4 */
            String decoded = URIFunctions.decodeURIComponent(componentString);
            if (decoded == null) {
                throw newURIError(cx, Messages.Key.MalformedURI);
            }
            return decoded;
        }

        /**
         * 18.3.3 encodeURI (uri)
         */
        @Function(name = "encodeURI", arity = 1)
        public static Object encodeURI(ExecutionContext cx, Object thisValue, Object uri) {
            /* steps 1-2 */
            String uriString = ToFlatString(cx, uri);
            /* steps 3-4 */
            String encoded = URIFunctions.encodeURI(uriString);
            if (encoded == null) {
                throw newURIError(cx, Messages.Key.MalformedURI);
            }
            return encoded;
        }

        /**
         * 18.3.4 encodeURIComponent (uriComponent)
         */
        @Function(name = "encodeURIComponent", arity = 1)
        public static Object encodeURIComponent(ExecutionContext cx, Object thisValue,
                Object uriComponent) {
            /* steps 1-2 */
            String componentString = ToFlatString(cx, uriComponent);
            /* steps 3-4 */
            String encoded = URIFunctions.encodeURIComponent(componentString);
            if (encoded == null) {
                throw newURIError(cx, Messages.Key.MalformedURI);
            }
            return encoded;
        }
    }

    /**
     * 18.4 Constructor Properties of the Global Object
     */
    public enum ConstructorProperties {
        ;

        /**
         * 18.4.16 Object ( . . . )
         */
        @Value(name = "Object")
        public static final Intrinsics Object = Intrinsics.Object;

        /**
         * 18.4.10 Function ( . . . )
         */
        @Value(name = "Function")
        public static final Intrinsics Function = Intrinsics.Function;

        /**
         * 18.4.1 Array ( . . . )
         */
        @Value(name = "Array")
        public static final Intrinsics Array = Intrinsics.Array;

        /**
         * 18.4.21 String ( . . . )
         */
        @Value(name = "String")
        public static final Intrinsics String = Intrinsics.String;

        /**
         * 18.4.x Symbol ( . . . )
         */
        @Value(name = "Symbol")
        public static final Intrinsics Symbol = Intrinsics.Symbol;

        /**
         * 18.4.3 Boolean ( . . . )
         */
        @Value(name = "Boolean")
        public static final Intrinsics Boolean = Intrinsics.Boolean;

        /**
         * 18.4.15 Number ( . . . )
         */
        @Value(name = "Number")
        public static final Intrinsics Number = Intrinsics.Number;

        /**
         * 18.4.5 Date ( . . . )
         */
        @Value(name = "Date")
        public static final Intrinsics Date = Intrinsics.Date;

        /**
         * 18.4.19 RegExp ( . . . )
         */
        @Value(name = "RegExp")
        public static final Intrinsics RegExp = Intrinsics.RegExp;

        /**
         * 18.4.6 Error ( . . . )
         */
        @Value(name = "Error")
        public static final Intrinsics Error = Intrinsics.Error;

        /**
         * 18.4.7 EvalError ( . . . )
         */
        @Value(name = "EvalError")
        public static final Intrinsics EvalError = Intrinsics.EvalError;

        /**
         * 18.4.17 RangeError ( . . . )
         */
        @Value(name = "RangeError")
        public static final Intrinsics RangeError = Intrinsics.RangeError;

        /**
         * 18.4.18 ReferenceError ( . . . )
         */
        @Value(name = "ReferenceError")
        public static final Intrinsics ReferenceError = Intrinsics.ReferenceError;

        /**
         * 18.4.22 SyntaxError ( . . . )
         */
        @Value(name = "SyntaxError")
        public static final Intrinsics SyntaxError = Intrinsics.SyntaxError;

        /**
         * 18.4.23 TypeError ( . . . )
         */
        @Value(name = "TypeError")
        public static final Intrinsics TypeError = Intrinsics.TypeError;

        /**
         * 18.4.28 URIError ( . . . )
         */
        @Value(name = "URIError")
        public static final Intrinsics URIError = Intrinsics.URIError;

        // InternalError
        @Value(name = "InternalError")
        public static final Intrinsics InternalError = Intrinsics.InternalError;

        /**
         * 18.4.14 Map ( . . . )
         */
        @Value(name = "Map")
        public static final Intrinsics Map = Intrinsics.Map;

        /**
         * 18.4.29 WeakMap ( . . . )
         */
        @Value(name = "WeakMap")
        public static final Intrinsics WeakMap = Intrinsics.WeakMap;

        /**
         * 18.4.20 Set ( . . . )
         */
        @Value(name = "Set")
        public static final Intrinsics Set = Intrinsics.Set;

        /**
         * 18.4.30 WeakSet ( . . . )
         */
        @Value(name = "WeakSet")
        public static final Intrinsics WeakSet = Intrinsics.WeakSet;

        /**
         * 18.4.2 ArrayBuffer ( . . . )
         */
        @Value(name = "ArrayBuffer")
        public static final Intrinsics ArrayBuffer = Intrinsics.ArrayBuffer;

        /**
         * 18.4.11 Int8Array ( . . . )
         */
        @Value(name = "Int8Array")
        public static final Intrinsics Int8Array = Intrinsics.Int8Array;

        /**
         * 18.4.24 UInt8Array ( . . . )
         */
        @Value(name = "Uint8Array")
        public static final Intrinsics Uint8Array = Intrinsics.Uint8Array;

        /**
         * 18.4.25 UInt8ClampedArray ( . . . )
         */
        @Value(name = "Uint8ClampedArray")
        public static final Intrinsics Uint8ClampedArray = Intrinsics.Uint8ClampedArray;

        /**
         * 18.4.12 Int16Array ( . . . )
         */
        @Value(name = "Int16Array")
        public static final Intrinsics Int16Array = Intrinsics.Int16Array;

        /**
         * 18.4.26 UInt16Array ( . . . )
         */
        @Value(name = "Uint16Array")
        public static final Intrinsics Uint16Array = Intrinsics.Uint16Array;

        /**
         * 18.4.13 Int32Array ( . . . )
         */
        @Value(name = "Int32Array")
        public static final Intrinsics Int32Array = Intrinsics.Int32Array;

        /**
         * 18.4.27 UInt32Array ( . . . )
         */
        @Value(name = "Uint32Array")
        public static final Intrinsics Uint32Array = Intrinsics.Uint32Array;

        /**
         * 18.4.8 Float32Array ( . . . )
         */
        @Value(name = "Float32Array")
        public static final Intrinsics Float32Array = Intrinsics.Float32Array;

        /**
         * 18.4.9 Float64Array ( . . . )
         */
        @Value(name = "Float64Array")
        public static final Intrinsics Float64Array = Intrinsics.Float64Array;

        /**
         * 18.4.4 DataView ( . . . )
         */
        @Value(name = "DataView")
        public static final Intrinsics DataView = Intrinsics.DataView;

        /**
         * Promise ( . . . )
         */
        @Value(name = "Promise")
        public static final Intrinsics Promise = Intrinsics.Promise;

        /**
         * Loader ( . . . )
         */
        @Value(name = "Loader")
        public static final Intrinsics Loader = Intrinsics.Loader;

        /**
         * Realm ( . . . )
         */
        @Value(name = "Realm")
        public static final Intrinsics Realm = Intrinsics.Realm;
    }

    /**
     * 18.5 Other Properties of the Global Object
     */
    public enum OtherProperties {
        ;

        /**
         * 18.5.1 JSON
         */
        @Value(name = "JSON")
        public static final Intrinsics JSON = Intrinsics.JSON;

        /**
         * 18.5.2 Math
         */
        @Value(name = "Math")
        public static final Intrinsics Math = Intrinsics.Math;

        /**
         * 18.5.3 Proxy
         */
        @Value(name = "Proxy")
        public static final Intrinsics Proxy = Intrinsics.Proxy;

        /**
         * 18.5.4 Reflect
         */
        @Value(name = "Reflect")
        public static final Intrinsics Reflect = Intrinsics.Reflect;

        /**
         * Module Factory Function
         */
        @Value(name = "Module")
        public static final Intrinsics Module = Intrinsics.Module;

        // Internationalization API

        @Value(name = "Intl")
        public static final Intrinsics Intl = Intrinsics.Intl;
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
         */
        @Function(name = "escape", arity = 1)
        public static Object escape(ExecutionContext cx, Object thisValue, Object string) {
            /* steps 1-2 */
            String s = ToFlatString(cx, string);
            /* step 3 */
            int length = s.length();
            /* step 4 */
            StringBuilder r = new StringBuilder(length);
            /* steps 5-6 */
            for (int k = 0; k < length; ++k) {
                char c = s.charAt(k);
                if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                        || c == '@' || c == '*' || c == '_' || c == '+' || c == '-' || c == '.'
                        || c == '/') {
                    r.append(c);
                } else if (c < 256) {
                    r.append('%').append(toHexDigit(c, 4)).append(toHexDigit(c, 0));
                } else {
                    r.append("%u").append(toHexDigit(c, 12)).append(toHexDigit(c, 8))
                            .append(toHexDigit(c, 4)).append(toHexDigit(c, 0));
                }
            }
            /* step 7 */
            return r.toString();
        }

        /**
         * B.2.1.2 unescape (string)
         */
        @Function(name = "unescape", arity = 1)
        public static Object unescape(ExecutionContext cx, Object thisValue, Object string) {
            /* steps 1-2 */
            String s = ToFlatString(cx, string);
            /* step 3 */
            int length = s.length();
            /* step 4 */
            StringBuilder r = new StringBuilder(length);
            /* steps 5-6 */
            for (int k = 0; k < length; ++k) {
                char c = s.charAt(k);
                if (c == '%') {
                    if (k <= length - 6 && s.charAt(k + 1) == 'u') {
                        char c2 = s.charAt(k + 2);
                        char c3 = s.charAt(k + 3);
                        char c4 = s.charAt(k + 4);
                        char c5 = s.charAt(k + 5);
                        int h = fromHexDigit(c2) << 12 | fromHexDigit(c3) << 8
                                | fromHexDigit(c4) << 4 | fromHexDigit(c5);
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
            /* step 7 */
            return r.toString();
        }
    }

    private static final class URIFunctions {
        private URIFunctions() {
        }

        /**
         * 18.3.1 decodeURI (encodedURI)
         */
        public static String decodeURI(String encodedURI) {
            return decode(encodedURI, RESERVED_LO | HASH, RESERVED_HI);
        }

        /**
         * 18.3.2 decodeURIComponent (encodedURIComponent)
         */
        public static String decodeURIComponent(String encodedURIComponent) {
            return decode(encodedURIComponent, 0, 0);
        }

        /**
         * 18.3.3 encodeURI (uri)
         */
        public static String encodeURI(String uri) {
            return encode(uri, RESERVED_LO | UNESCAPED_LO | HASH, RESERVED_HI | UNESCAPED_HI);
        }

        /**
         * 18.3.4 encodeURIComponent (uriComponent)
         */
        public static String encodeURIComponent(String uriComponent) {
            return encode(uriComponent, UNESCAPED_LO, UNESCAPED_HI);
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

        private static StringBuilder writeByte(StringBuilder sb, int b) {
            int b0 = (b >> 4) & 0b1111;
            int b1 = b & 0b1111;
            b0 = b0 + (b0 < 0x0A ? '0' : ('A' - 10));
            b1 = b1 + (b1 < 0x0A ? '0' : ('A' - 10));
            return sb.append('%').append((char) b0).append((char) b1);
        }

        /**
         * Runtime Semantics: Encode Abstract Operation
         * <p>
         * Returns encoded string or {@code null} on error
         */
        private static String encode(String s, long low, long high) {
            int length = s.length();
            StringBuilder sb = null;
            for (int i = 0; i < length; ++i) {
                char c = s.charAt(i);
                if (masked(c, low, high)) {
                    if (sb != null) {
                        sb.append(c);
                    }
                } else {
                    if (sb == null) {
                        sb = new StringBuilder(length).append(s.substring(0, i));
                    }
                    if (c <= 0x7F) {
                        writeByte(sb, c);
                    } else if (c <= 0x7FF) {
                        int c0 = ((c >> 6) & 0b11111);
                        int c1 = (c & 0b111111);
                        writeByte(sb, 0b11000000 | c0);
                        writeByte(sb, 0b10000000 | c1);
                    } else if ((c >= 0x0800 && c <= 0xD7FF) || (c >= 0xE000 && c <= 0xFFFF)) {
                        int c0 = ((c >> 12) & 0b1111);
                        int c1 = ((c >> 6) & 0b111111);
                        int c2 = (c & 0b111111);
                        writeByte(sb, 0b11100000 | c0);
                        writeByte(sb, 0b10000000 | c1);
                        writeByte(sb, 0b10000000 | c2);
                    } else if (Character.isHighSurrogate(c)) {
                        int cp = s.codePointAt(i);
                        if (cp <= 0xFFFF || cp > 0x10FFFF) {
                            return null;
                        }
                        int c0 = ((cp >> 18) & 0b111);
                        int c1 = ((cp >> 12) & 0b111111);
                        int c2 = ((cp >> 6) & 0b111111);
                        int c3 = (cp & 0b111111);
                        writeByte(sb, 0b11110000 | c0);
                        writeByte(sb, 0b10000000 | c1);
                        writeByte(sb, 0b10000000 | c2);
                        writeByte(sb, 0b10000000 | c3);
                        // add one b/c of surrogate pair
                        i += 1;
                    } else {
                        return null;
                    }
                }
            }
            return (sb != null ? sb.toString() : s);
        }

        /**
         * Runtime Semantics: Decode Abstract Operation
         * <p>
         * Returns decoded string or {@code null} on error
         */
        private static String decode(String s, long low, long high) {
            int i = s.indexOf('%');
            if (i < 0) {
                return s;
            }
            int len = s.length();
            int j = 0;
            StringBuilder sb = new StringBuilder(len);
            while (i >= 0) {
                if (i + 2 >= len)
                    return null;
                if (j < i) {
                    // append substring before '%'
                    sb.append(s.substring(j, i));
                }
                int c0 = readByte(s, i);
                if (c0 < 0)
                    return null;
                if (c0 <= 0b01111111) {
                    // US-ASCII
                    if (masked(c0, low, high)) {
                        // leave as-is
                        sb.append(s.substring(i, i + 3));
                    } else {
                        sb.append((char) c0);
                    }
                    i += 3;
                } else if (c0 <= 0b10111111) {
                    // illegal (byte sequence part)
                    return null;
                } else if (c0 <= 0b11000001) {
                    // illegal (two byte encoding for US-ASCII)
                    return null;
                } else if (c0 <= 0b11011111) {
                    // two byte sequence
                    if (!((i + 2 + 3 < len)))
                        return null;
                    int c1 = readByte(s, i + 3);
                    if (c1 < 0x80 || c1 > 0xBF)
                        return null;
                    sb.append((char) ((c0 & 0b11111) << 6 | (c1 & 0b111111)));
                    i += 6;
                } else if (c0 <= 0b11101111) {
                    // three byte sequence
                    if (!((i + 2 + 6 < len)))
                        return null;
                    int c1 = readByte(s, i + 3);
                    int c2 = readByte(s, i + 6);
                    if (c0 == 0b11100000 ? (c1 < 0xA0 || c1 > 0xBF)
                            : c0 == 0b11101101 ? (c1 < 0x80 || c1 > 0x9F)
                                    : (c1 < 0x80 || c1 > 0xBF))
                        return null;
                    if (c2 < 0x80 || c2 > 0xBF)
                        return null;
                    sb.append((char) ((c0 & 0b1111) << 12 | (c1 & 0b111111) << 6 | (c2 & 0b111111)));
                    i += 9;
                } else if (c0 <= 0b11110100) {
                    // four byte sequence
                    if (!((i + 2 + 9 < len)))
                        return null;
                    int c1 = readByte(s, i + 3);
                    int c2 = readByte(s, i + 6);
                    int c3 = readByte(s, i + 9);
                    if (c0 == 0b11110000 ? (c1 < 0x90 || c1 > 0xBF)
                            : c0 == 0b11110100 ? (c1 < 0x80 || c1 > 0x8F)
                                    : (c1 < 0x80 || c1 > 0xBF))
                        return null;
                    if (c2 < 0x80 || c3 < 0x80 || c2 > 0xBF || c3 > 0xBF)
                        return null;
                    int cp = ((c0 & 0b111) << 18 | (c1 & 0b111111) << 12 | (c2 & 0b111111) << 6 | (c3 & 0b111111));
                    if (cp <= Character.MAX_CODE_POINT) {
                        // sb.appendCodePoint(cp);
                        sb.append(Character.highSurrogate(cp));
                        sb.append(Character.lowSurrogate(cp));
                    } else {
                        // illegal code point
                        return null;
                    }
                    i += 12;
                } else {
                    // illegal (cf. RFC-3629)
                    return null;
                }
                j = i;
                i = s.indexOf('%', i);
            }
            if (j < len) {
                // append remaining substring
                sb.append(s.substring(j, len));
            }
            return sb.toString();
        }

        /* embedded BitMaskUtil */

        private static boolean masked(char c, long low, long high) {
            return (c == 0) ? false : //
                    (c < 64) ? ((1L << c) & low) != 0 : //
                            (c < 128) ? ((1L << (c - 64)) & high) != 0 : false;
        }

        private static boolean masked(int c, long low, long high) {
            return (c == 0) ? false : //
                    (c < 64) ? ((1L << c) & low) != 0 : //
                            (c < 128) ? ((1L << (c - 64)) & high) != 0 : false;
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
