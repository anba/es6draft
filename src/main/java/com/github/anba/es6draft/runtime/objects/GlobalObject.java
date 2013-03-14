/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToInt32;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToNumber;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
import static com.github.anba.es6draft.runtime.internal.Errors.throwURIError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.Eval.indirectEval;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import org.mozilla.javascript.StringToNumber;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.1 The Global Object</h2>
 * <ul>
 * <li>15.1.1 Value Properties of the Global Object
 * <li>15.1.2 Function Properties of the Global Object
 * <li>15.1.3 URI Handling Function Properties
 * <li>15.1.4 Constructor Properties of the Global Object
 * <li>15.1.5 Other Properties of the Global Object
 * </ul>
 */
public class GlobalObject extends OrdinaryObject implements Scriptable, Initialisable {
    public GlobalObject(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(Realm realm) {
        // implementation defined behaviour
        setPrototype(realm.getIntrinsic(Intrinsics.ObjectPrototype));

        createProperties(this, realm, ValueProperties.class);
        createProperties(this, realm, FunctionProperties.class);
        createProperties(this, realm, URIFunctionProperties.class);
        createProperties(this, realm, ConstructorProperties.class);
        createProperties(this, realm, OtherProperties.class);
    }

    /**
     * 15.1.1 Value Properties of the Global Object
     */
    public enum ValueProperties {
        ;

        /**
         * 15.1.1.1 NaN
         */
        @Value(name = "NaN", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Double NaN = Double.NaN;

        /**
         * 15.1.1.2 Infinity
         */
        @Value(name = "Infinity", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Double Infinity = Double.POSITIVE_INFINITY;

        /**
         * 15.1.1.3 undefined
         */
        @Value(name = "undefined", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Undefined undefined = UNDEFINED;
    }

    /**
     * 15.1.2 Function Properties of the Global Object
     */
    public enum FunctionProperties {
        ;

        /**
         * 15.1.2.1 eval (x)
         */
        @Function(name = "eval", arity = 1)
        public static Object eval(Realm realm, Object thisValue, Object x) {
            return indirectEval(realm, x);
        }

        /**
         * 15.1.2.2 parseInt (string , radix)
         */
        @Function(name = "parseInt", arity = 2)
        public static Object parseInt(Realm realm, Object thisValue, Object string, Object radix) {
            CharSequence inputString = ToString(realm, string);
            CharSequence s = Strings.trimLeft(inputString);
            int len = s.length();
            int index = 0;
            /* step 5-6 */
            boolean isPos = true;
            if (index < len && (s.charAt(index) == '+' || s.charAt(index) == '-')) {
                isPos = (s.charAt(index) == '+');
                index += 1;
            }
            /* step 7-8 */
            int r = ToInt32(realm, radix);
            /* step 9 */
            boolean stripPrefix = true;
            if (r != 0) {
                /* step 10 */
                if (r < 2 || r > 36) {
                    return Double.NaN;
                }
                stripPrefix = (r == 16);
            } else {
                /* step 11 */
                r = 10;
            }
            /* step 12 */
            if (stripPrefix && index + 1 < len && s.charAt(index) == '0'
                    && (s.charAt(index + 1) == 'x' || s.charAt(index + 1) == 'X')) {
                r = 16;
                index += 2;
            }
            /* step 13-16 */
            double number = StringToNumber.stringToNumber(s.toString(), index, r);
            /* step 17 */
            return (isPos ? number : -number);
        }

        /**
         * 15.1.2.3 parseFloat (string)
         */
        @Function(name = "parseFloat", arity = 1)
        public static Object parseFloat(Realm realm, Object thisValue, Object string) {
            CharSequence inputString = ToString(realm, string);
            String trimmedString = Strings.trimLeft(inputString).toString();
            if (trimmedString.isEmpty()) {
                return Double.NaN;
            }
            return readDecimalLiteralPrefix(trimmedString, 0, trimmedString.length());
        }

        /**
         * 15.1.2.4 isNaN (number)
         */
        @Function(name = "isNaN", arity = 1)
        public static Object isNaN(Realm realm, Object thisValue, Object number) {
            double num = ToNumber(realm, number);
            if (Double.isNaN(num)) {
                return true;
            }
            return false;
        }

        /**
         * 15.1.2.5 isFinite (number)
         */
        @Function(name = "isFinite", arity = 1)
        public static Object isFinite(Realm realm, Object thisValue, Object number) {
            double num = ToNumber(realm, number);
            if (Double.isNaN(num) || Double.isInfinite(num)) {
                return false;
            }
            return true;
        }

        private static int fromHexDigit(char c) {
            if (c >= '0' && c <= '9')
                return (c - '0');
            if (c >= 'A' && c <= 'F')
                return (c - 'A');
            if (c >= 'a' && c <= 'f')
                return (c - 'a');
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
        public static Object escape(Realm realm, Object thisValue, Object string) {
            // FIXME: spec bug (spec language still in ES3 style)
            String s = ToFlatString(realm, string);
            int length = s.length();
            StringBuilder r = new StringBuilder(length);
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
            return r.toString();
        }

        /**
         * B.2.1.2 unescape (string)
         */
        @Function(name = "unescape", arity = 1)
        public static Object unescape(Realm realm, Object thisValue, Object string) {
            // FIXME: spec bug (spec language still in ES3 style)
            String s = ToFlatString(realm, string);
            int length = s.length();
            StringBuilder r = new StringBuilder(length);
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
            return r.toString();
        }
    }

    /**
     * 15.1.3 URI Handling Function Properties
     */
    public enum URIFunctionProperties {
        ;

        /**
         * 15.1.3.1 decodeURI (encodedURI)
         */
        @Function(name = "decodeURI", arity = 1)
        public static Object decodeURI(Realm realm, Object thisValue, Object encodedURI) {
            String uriString = ToFlatString(realm, encodedURI);
            String decoded = URIFunctions.decodeURI(uriString);
            if (decoded == null) {
                throw throwURIError(realm, Messages.Key.MalformedURI);
            }
            return decoded;
        }

        /**
         * 15.1.3.2 decodeURIComponent (encodedURIComponent)
         */
        @Function(name = "decodeURIComponent", arity = 1)
        public static Object decodeURIComponent(Realm realm, Object thisValue,
                Object encodedURIComponent) {
            String componentString = ToFlatString(realm, encodedURIComponent);
            String decoded = URIFunctions.decodeURIComponent(componentString);
            if (decoded == null) {
                throw throwURIError(realm, Messages.Key.MalformedURI);
            }
            return decoded;
        }

        /**
         * 15.1.3.3 encodeURI (uri)
         */
        @Function(name = "encodeURI", arity = 1)
        public static Object encodeURI(Realm realm, Object thisValue, Object uri) {
            String uriString = ToFlatString(realm, uri);
            String encoded = URIFunctions.encodeURI(uriString);
            if (encoded == null) {
                throw throwURIError(realm, Messages.Key.MalformedURI);
            }
            return encoded;
        }

        /**
         * 15.1.3.4 encodeURIComponent (uriComponent)
         */
        @Function(name = "encodeURIComponent", arity = 1)
        public static Object encodeURIComponent(Realm realm, Object thisValue, Object uriComponent) {
            String componentString = ToFlatString(realm, uriComponent);
            String encoded = URIFunctions.encodeURIComponent(componentString);
            if (encoded == null) {
                throw throwURIError(realm, Messages.Key.MalformedURI);
            }
            return encoded;
        }
    }

    /**
     * 15.1.4 Constructor Properties of the Global Object
     */
    public enum ConstructorProperties {
        ;

        /**
         * 15.1.4.1 Object ( . . . )
         */
        @Value(name = "Object")
        public static final Intrinsics Object = Intrinsics.Object;

        /**
         * 15.1.4.2 Function ( . . . )
         */
        @Value(name = "Function")
        public static final Intrinsics Function = Intrinsics.Function;

        /**
         * 15.1.4.3 Array ( . . . )
         */
        @Value(name = "Array")
        public static final Intrinsics Array = Intrinsics.Array;

        /**
         * 15.1.4.4 String ( . . . )
         */
        @Value(name = "String")
        public static final Intrinsics String = Intrinsics.String;

        /**
         * 15.1.4.5 Boolean ( . . . )
         */
        @Value(name = "Boolean")
        public static final Intrinsics Boolean = Intrinsics.Boolean;

        /**
         * 15.1.4.6 Number ( . . . )
         */
        @Value(name = "Number")
        public static final Intrinsics Number = Intrinsics.Number;

        /**
         * 15.1.4.7 Date ( . . . )
         */
        @Value(name = "Date")
        public static final Intrinsics Date = Intrinsics.Date;

        /**
         * 15.1.4.8 RegExp ( . . . )
         */
        @Value(name = "RegExp")
        public static final Intrinsics RegExp = Intrinsics.RegExp;

        /**
         * 15.1.4.9 Error ( . . . )
         */
        @Value(name = "Error")
        public static final Intrinsics Error = Intrinsics.Error;

        /**
         * 15.1.4.10 EvalError ( . . . )
         */
        @Value(name = "EvalError")
        public static final Intrinsics EvalError = Intrinsics.EvalError;

        /**
         * 15.1.4.11 RangeError ( . . . )
         */
        @Value(name = "RangeError")
        public static final Intrinsics RangeError = Intrinsics.RangeError;

        /**
         * 15.1.4.12 ReferenceError ( . . . )
         */
        @Value(name = "ReferenceError")
        public static final Intrinsics ReferenceError = Intrinsics.ReferenceError;

        /**
         * 15.1.4.13 SyntaxError ( . . . )
         */
        @Value(name = "SyntaxError")
        public static final Intrinsics SyntaxError = Intrinsics.SyntaxError;

        /**
         * 15.1.4.14 TypeError ( . . . )
         */
        @Value(name = "TypeError")
        public static final Intrinsics TypeError = Intrinsics.TypeError;

        /**
         * 15.1.4.15 URIError ( . . . )
         */
        @Value(name = "URIError")
        public static final Intrinsics URIError = Intrinsics.URIError;

        /**
         * 15.1.4.16 Map ( . . . )
         */
        @Value(name = "Map")
        public static final Intrinsics Map = Intrinsics.Map;

        /**
         * 15.1.4.17 WeakMap ( . . . )
         */
        @Value(name = "WeakMap")
        public static final Intrinsics WeakMap = Intrinsics.WeakMap;

        /**
         * 15.1.4.18 Set ( . . . )
         */
        @Value(name = "Set")
        public static final Intrinsics Set = Intrinsics.Set;

        // TODO: StopIteration object

        @Value(name = "StopIteration")
        public static final Intrinsics StopIteration = Intrinsics.StopIteration;

        // TODO: binary module intrinsics

        @Value(name = "ArrayBuffer")
        public static final Intrinsics ArrayBuffer = Intrinsics.ArrayBuffer;

        @Value(name = "Int8Array")
        public static final Intrinsics Int8Array = Intrinsics.Int8Array;

        @Value(name = "Uint8Array")
        public static final Intrinsics Uint8Array = Intrinsics.Uint8Array;

        @Value(name = "Uint8ClampedArray")
        public static final Intrinsics Uint8ClampedArray = Intrinsics.Uint8ClampedArray;

        @Value(name = "Int16Array")
        public static final Intrinsics Int16Array = Intrinsics.Int16Array;

        @Value(name = "Uint16Array")
        public static final Intrinsics Uint16Array = Intrinsics.Uint16Array;

        @Value(name = "Int32Array")
        public static final Intrinsics Int32Array = Intrinsics.Int32Array;

        @Value(name = "Uint32Array")
        public static final Intrinsics Uint32Array = Intrinsics.Uint32Array;

        @Value(name = "Float32Array")
        public static final Intrinsics Float32Array = Intrinsics.Float32Array;

        @Value(name = "Float64Array")
        public static final Intrinsics Float64Array = Intrinsics.Float64Array;

        @Value(name = "DataView")
        public static final Intrinsics DataView = Intrinsics.DataView;

        // Internationalization API

        @Value(name = "Intl")
        public static final Intrinsics Intl = Intrinsics.Intl;
    }

    /**
     * 15.1.5 Other Properties of the Global Object
     */
    public enum OtherProperties {
        ;

        /**
         * 15.1.5.1 Math
         */
        @Value(name = "Math")
        public static final Intrinsics Math = Intrinsics.Math;

        /**
         * 15.1.5.2 JSON
         */
        @Value(name = "JSON")
        public static final Intrinsics JSON = Intrinsics.JSON;
    }

    private static double readDecimalLiteralPrefix(String s, int start, int end) {
        final int Infinity_length = "Infinity".length();

        int index = start;
        int c = s.charAt(index++);
        boolean isPos = true;
        if (c == '+' || c == '-') {
            if (index >= end)
                return Double.NaN;
            isPos = (c == '+');
            c = s.charAt(index++);
        }
        if (c == 'I') {
            // Infinity
            if (index - 1 + Infinity_length <= end
                    && s.regionMatches(index - 1, "Infinity", 0, Infinity_length)) {
                return isPos ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
            }
            return Double.NaN;
        }
        prefix: {
            if (c == '.') {
                if (index >= end)
                    return Double.NaN;
                char d = s.charAt(index);
                if (!(d >= '0' && d <= '9')) {
                    return Double.NaN;
                }
            } else {
                if (!(c >= '0' && c <= '9')) {
                    return Double.NaN;
                }
                do {
                    if (!(c >= '0' && c <= '9')) {
                        break prefix;
                    }
                    if (index >= end) {
                        break;
                    }
                    c = s.charAt(index++);
                } while (c != '.' && c != 'e' && c != 'E');
            }
            if (c == '.') {
                do {
                    if (index >= end) {
                        break;
                    }
                    c = s.charAt(index++);
                } while (c >= '0' && c <= '9');
            }
            if (c == 'e' || c == 'E') {
                if (index >= end)
                    return Double.NaN;
                int exp = index;
                c = s.charAt(index++);
                if (c == '+' || c == '-') {
                    if (index >= end) {
                        index = exp;
                        break prefix;
                    }
                    c = s.charAt(index++);
                }
                if (!(c >= '0' && c <= '9')) {
                    index = exp;
                    break prefix;
                }
                do {
                    if (!(c >= '0' && c <= '9')) {
                        break prefix;
                    }
                    if (index >= end) {
                        break;
                    }
                    c = s.charAt(index++);
                } while (true);
            }
            if (index >= end) {
                return Double.parseDouble(s.substring(start, end));
            }
        } // prefix
        return Double.parseDouble(s.substring(start, index - 1));
    }

    private final static class URIFunctions {
        private URIFunctions() {
        }

        /**
         * 15.1.3.1 decodeURI (encodedURI)
         */
        public static String decodeURI(String encodedURI) {
            return decode(encodedURI, RESERVED_LO | HASH, RESERVED_HI);
        }

        /**
         * 15.1.3.2 decodeURIComponent (encodedURIComponent)
         */
        public static String decodeURIComponent(String encodedURIComponent) {
            return decode(encodedURIComponent, 0, 0);
        }

        /**
         * 15.1.3.3 encodeURI (uri)
         */
        public static String encodeURI(String uri) {
            return encode(uri, RESERVED_LO | UNESCAPED_LO | HASH, RESERVED_HI | UNESCAPED_HI);
        }

        /**
         * 15.1.3.4 encodeURIComponent (uriComponent)
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

            assert ALPHA_LO == URIFunctions.low("");
            assert ALPHA_HI == URIFunctions
                    .high("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");

            assert DIGIT_LO == URIFunctions.low("0123456789");
            assert DIGIT_HI == URIFunctions.high("");

            assert MARK_LO == URIFunctions.low("-.!*'()");
            assert MARK_HI == URIFunctions.high("_~");
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
                    if (c1 < 0x80 || c2 < 0x80 || c1 > 0xBF || c2 > 0xBF)
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
                    if (c1 < 0x80 || c2 < 0x80 || c3 < 0x80 || c1 > 0xBF || c2 > 0xBF || c3 > 0xBF)
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
        private static final long high(char c) {
            assert c >= 64 && c < 128;
            return (1L << (c - 64));
        }
    }
}
