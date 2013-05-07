/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.throwRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.RegExpConstructor.RegExpCreate;
import static com.github.anba.es6draft.runtime.objects.intl.CollatorPrototype.CompareStrings;
import static com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.CanonicalizeLocaleList;
import static com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.DefaultLocale;
import static com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.LookupSupportedLocales;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArray.ArrayCreate;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.runtime.objects.intl.CollatorConstructor;
import com.github.anba.es6draft.runtime.objects.intl.CollatorObject;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.ExoticString;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.util.ULocale;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.5 String Objects</h2>
 * <ul>
 * <li>15.5.4 Properties of the String Prototype Object
 * <li>15.5.5 Properties of String Instances
 * </ul>
 */
public class StringPrototype extends OrdinaryObject implements Initialisable {
    public StringPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
    }

    /**
     * 15.5.4 Properties of the String Prototype Object
     */
    public enum Properties {
        ;

        /**
         * Abstract operation thisStringValue(value)
         */
        private static CharSequence thisStringValue(ExecutionContext cx, Object object) {
            if (Type.isString(object)) {
                return Type.stringValue(object);
            }
            if (object instanceof ExoticString) {
                CharSequence s = ((ExoticString) object).getStringData();
                if (s != null) {
                    return s;
                }
            }
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.5.4.1 String.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.String;

        /**
         * 15.5.4.2 String.prototype.toString ( )
         */
        @Function(name = "toString", arity = 0)
        public static Object toString(ExecutionContext cx, Object thisValue) {
            return thisStringValue(cx, thisValue);
        }

        /**
         * 15.5.4.3 String.prototype.valueOf ( )
         */
        @Function(name = "valueOf", arity = 0)
        public static Object valueOf(ExecutionContext cx, Object thisValue) {
            return thisStringValue(cx, thisValue);
        }

        /**
         * 15.5.4.4 String.prototype.charAt (pos)
         */
        @Function(name = "charAt", arity = 1)
        public static Object charAt(ExecutionContext cx, Object thisValue, Object pos) {
            Object obj = CheckObjectCoercible(cx, thisValue);
            CharSequence s = ToString(cx, obj);
            double position = ToInteger(cx, pos);
            int size = s.length();
            if (position < 0 || position >= size) {
                return "";
            }
            return String.valueOf(s.charAt((int) position));
        }

        /**
         * 15.5.4.5 String.prototype.charCodeAt (pos)
         */
        @Function(name = "charCodeAt", arity = 1)
        public static Object charCodeAt(ExecutionContext cx, Object thisValue, Object pos) {
            Object obj = CheckObjectCoercible(cx, thisValue);
            CharSequence s = ToString(cx, obj);
            double position = ToInteger(cx, pos);
            int size = s.length();
            if (position < 0 || position >= size) {
                return Double.NaN;
            }
            return (int) s.charAt((int) position);
        }

        /**
         * 15.5.4.6 String.prototype.concat ( ...args )
         */
        @Function(name = "concat", arity = 1)
        public static Object concat(ExecutionContext cx, Object thisValue, Object... args) {
            Object obj = CheckObjectCoercible(cx, thisValue);
            CharSequence s = ToString(cx, obj);
            StringBuilder r = new StringBuilder(s);
            for (int i = 0; i < args.length; ++i) {
                Object next = args[i];
                CharSequence nextString = ToString(cx, next);
                r.append(nextString);
            }
            return r.toString();
        }

        /**
         * 15.5.4.7 String.prototype.indexOf (searchString, position)
         */
        @Function(name = "indexOf", arity = 1)
        public static Object indexOf(ExecutionContext cx, Object thisValue, Object searchString,
                Object position) {
            Object obj = CheckObjectCoercible(cx, thisValue);
            String s = ToFlatString(cx, obj);
            String searchStr = ToFlatString(cx, searchString);
            double pos = ToInteger(cx, position);
            int len = s.length();
            int start = (int) Math.min(Math.max(pos, 0), len);
            return s.indexOf(searchStr, start);
        }

        /**
         * 15.5.4.8 String.prototype.lastIndexOf (searchString, position)
         */
        @Function(name = "lastIndexOf", arity = 1)
        public static Object lastIndexOf(ExecutionContext cx, Object thisValue,
                Object searchString, Object position) {
            Object obj = CheckObjectCoercible(cx, thisValue);
            String s = ToFlatString(cx, obj);
            String searchStr = ToFlatString(cx, searchString);
            double numPos = ToNumber(cx, position);
            double pos = Double.isNaN(numPos) ? Double.POSITIVE_INFINITY : ToInteger(cx, position);
            int len = s.length();
            int start = (int) Math.min(Math.max(pos, 0), len);
            return s.lastIndexOf(searchStr, start);
        }

        /**
         * 15.5.4.9 String.prototype.localeCompare (that)<br>
         * 13.1.1 String.prototype.localeCompare (that [, locales [, options]])
         */
        @Function(name = "localeCompare", arity = 1)
        public static Object localeCompare(ExecutionContext cx, Object thisValue, Object that,
                Object locales, Object options) {
            Object obj = CheckObjectCoercible(cx, thisValue);
            String s = ToFlatString(cx, obj);
            String t = ToFlatString(cx, that);

            // ES5/6
            // return cx.getRealm().getCollator().compare(s, t);

            // ECMA-402
            CollatorConstructor constructor = (CollatorConstructor) cx
                    .getIntrinsic(Intrinsics.Intl_Collator);
            CollatorObject collator = (CollatorObject) constructor.construct(cx, locales, options);
            return CompareStrings(cx, collator, s, t);
        }

        /**
         * 15.5.4.10 String.prototype.match (regexp)
         */
        @Function(name = "match", arity = 1)
        public static Object match(ExecutionContext cx, Object thisValue, Object regexp) {
            Object obj = CheckObjectCoercible(cx, thisValue);
            CharSequence s = ToString(cx, obj);
            ScriptObject rx;
            if (Type.isObject(regexp)
                    && HasProperty(cx, Type.objectValue(regexp), BuiltinSymbol.isRegExp.get())) {
                rx = Type.objectValue(regexp);
            } else {
                String p = Type.isUndefined(regexp) ? "" : ToFlatString(cx, regexp);
                rx = RegExpCreate(cx, p, "");
            }
            return Invoke(cx, rx, "match", s);
        }

        /**
         * 15.5.4.11 String.prototype.replace (searchValue, replaceValue)
         */
        @Function(name = "replace", arity = 2)
        public static Object replace(ExecutionContext cx, Object thisValue, Object searchValue,
                Object replaceValue) {
            Object obj = CheckObjectCoercible(cx, thisValue);
            String string = ToFlatString(cx, obj);
            if (Type.isObject(searchValue)
                    && HasProperty(cx, Type.objectValue(searchValue), BuiltinSymbol.isRegExp.get())) {
                return Invoke(cx, Type.objectValue(searchValue), "replace", string, replaceValue);
            }
            String searchString = ToFlatString(cx, searchValue);
            // FIXME: always call ToString(replValue) even if no match
            if (!IsCallable(replaceValue)) {
                replaceValue = ToFlatString(cx, replaceValue);
            }
            int pos = string.indexOf(searchString);
            if (pos < 0) {
                return string;
            }
            String matched = searchString;
            String replStr;
            if (IsCallable(replaceValue)) {
                Object replValue = ((Callable) replaceValue).call(cx, UNDEFINED, matched, pos,
                        string);
                replStr = ToFlatString(cx, replValue);
            } else {
                String replValue = (String) replaceValue;
                replStr = GetReplaceSubstitution(matched, replValue, string, pos);
            }
            int tailPos = pos + searchString.length();
            return string.substring(0, pos) + replStr + string.substring(tailPos);
        }

        /**
         * Runtime Semantics: GetReplaceSubstitution Abstract Operation
         */
        private static String GetReplaceSubstitution(String matched, String replValue,
                String string, int position) {
            int matchLength = matched.length();
            int stringLength = string.length();
            assert position >= 0 && position <= stringLength;
            int tailPos = position + matchLength;
            assert tailPos >= 0 && tailPos <= stringLength;

            StringBuilder result = new StringBuilder();
            for (int cursor = 0, len = replValue.length(); cursor < len;) {
                char c = replValue.charAt(cursor++);
                if (c == '$' && cursor < len) {
                    c = replValue.charAt(cursor++);
                    switch (c) {
                    case '&':
                        result.append(matched);
                        break;
                    case '`':
                        result.append(string, 0, position);
                        break;
                    case '\'':
                        result.append(string, tailPos, stringLength);
                        break;
                    case '$':
                        result.append('$');
                        break;
                    default:
                        result.append('$').append(c);
                        break;
                    }
                } else {
                    result.append(c);
                }
            }

            return result.toString();
        }

        /**
         * 15.5.4.12 String.prototype.search (regexp)
         */
        @Function(name = "search", arity = 1)
        public static Object search(ExecutionContext cx, Object thisValue, Object regexp) {
            Object obj = CheckObjectCoercible(cx, thisValue);
            CharSequence string = ToString(cx, obj);
            ScriptObject rx;
            if (Type.isObject(regexp)
                    && HasProperty(cx, Type.objectValue(regexp), BuiltinSymbol.isRegExp.get())) {
                rx = Type.objectValue(regexp);
            } else {
                String p = Type.isUndefined(regexp) ? "" : ToFlatString(cx, regexp);
                rx = RegExpCreate(cx, p, "");
            }
            return Invoke(cx, rx, "search", string);
        }

        /**
         * 15.5.4.13 String.prototype.slice (start, end)
         */
        @Function(name = "slice", arity = 2)
        public static Object slice(ExecutionContext cx, Object thisValue, Object start, Object end) {
            Object obj = CheckObjectCoercible(cx, thisValue);
            CharSequence s = ToString(cx, obj);
            int len = s.length();
            double intStart = ToInteger(cx, start);
            double intEnd = (Type.isUndefined(end) ? len : ToInteger(cx, end));
            int from = (int) (intStart < 0 ? Math.max(len + intStart, 0) : Math.min(intStart, len));
            int to = (int) (intEnd < 0 ? Math.max(len + intEnd, 0) : Math.min(intEnd, len));
            int span = Math.max(to - from, 0);
            return s.subSequence(from, from + span);
        }

        /**
         * 15.5.4.14 String.prototype.split (separator, limit)
         */
        @Function(name = "split", arity = 2)
        public static Object split(ExecutionContext cx, Object thisValue, Object separator,
                Object limit) {
            Object obj = CheckObjectCoercible(cx, thisValue);
            String s = ToFlatString(cx, obj);
            if (Type.isObject(separator)
                    && HasProperty(cx, Type.objectValue(separator), BuiltinSymbol.isRegExp.get())) {
                return Invoke(cx, separator, "split", s, limit);
            }
            ScriptObject a = ArrayCreate(cx, 0);
            int lengthA = 0;
            long lim = Type.isUndefined(limit) ? 0xFFFFFFFFL : ToUint32(cx, limit);
            int size = s.length();
            int p = 0;
            String r = ToFlatString(cx, separator);
            if (lim == 0) {
                return a;
            }
            if (Type.isUndefined(separator)) {
                a.defineOwnProperty(cx, "0", new PropertyDescriptor(s, true, true, true));
                return a;
            }
            if (size == 0) {
                if (s.startsWith(r)) {
                    return a;
                }
                a.defineOwnProperty(cx, "0", new PropertyDescriptor(s, true, true, true));
                return a;
            }
            int q = p;
            while (q != size) {
                int z = SplitMatch(s, q, r);
                if (z == -1) {
                    break;
                } else {
                    int e = z + r.length();
                    if (e == p) {
                        q = q + 1;
                    } else {
                        String t = s.substring(p, z);
                        a.defineOwnProperty(cx, ToString(lengthA), new PropertyDescriptor(t, true,
                                true, true));
                        lengthA += 1;
                        if (lengthA == lim) {
                            return a;
                        }
                        p = e;
                        q = p;
                    }
                }
            }
            String t = s.substring(p, size);
            a.defineOwnProperty(cx, ToString(lengthA), new PropertyDescriptor(t, true, true, true));
            return a;
        }

        /**
         * Runtime Semantics: SplitMatch Abstract Operation
         */
        public static int SplitMatch(String s, int q, String r) {
            // returns start instead of end position
            return s.indexOf(r, q);
        }

        /**
         * 15.5.4.15 String.prototype.substring (start, end)
         */
        @Function(name = "substring", arity = 2)
        public static Object substring(ExecutionContext cx, Object thisValue, Object start,
                Object end) {
            Object obj = CheckObjectCoercible(cx, thisValue);
            CharSequence s = ToString(cx, obj);
            int len = s.length();
            double intStart = ToInteger(cx, start);
            double intEnd = (Type.isUndefined(end) ? len : ToInteger(cx, end));
            int finalStart = (int) Math.min(Math.max(intStart, 0), len);
            int finalEnd = (int) Math.min(Math.max(intEnd, 0), len);
            int from = Math.min(finalStart, finalEnd);
            int to = Math.max(finalStart, finalEnd);
            return s.subSequence(from, to);
        }

        /**
         * 15.5.4.16 String.prototype.toLowerCase ( )
         */
        @Function(name = "toLowerCase", arity = 0)
        public static Object toLowerCase(ExecutionContext cx, Object thisValue) {
            Object obj = CheckObjectCoercible(cx, thisValue);
            String s = ToFlatString(cx, obj);
            return s.toLowerCase(Locale.ROOT);
        }

        /**
         * 15.5.4.17 String.prototype.toLocaleLowerCase ( )<br>
         * 13.1.2 String.prototype.toLocaleLowerCase ([locales])
         */
        @Function(name = "toLocaleLowerCase", arity = 0)
        public static Object toLocaleLowerCase(ExecutionContext cx, Object thisValue, Object locales) {
            Object obj = CheckObjectCoercible(cx, thisValue);
            String s = ToFlatString(cx, obj);

            // ES5/6
            // return s.toLowerCase(cx.getRealm().getLocale());

            Set<String> requestedLocales = CanonicalizeLocaleList(cx, locales);
            int len = requestedLocales.size();
            String requestedLocale = (len > 0 ? requestedLocales.iterator().next()
                    : DefaultLocale(cx.getRealm()));
            Set<String> availableLocales = new HashSet<>(Arrays.asList("az", "lt", "tr"));
            // FIXME: spec issue? spec should just call LookupSupportedLocales abstract operation..
            List<String> supportedLocales = LookupSupportedLocales(cx, availableLocales,
                    Collections.singleton(requestedLocale));
            String supportedLocale = (supportedLocales.isEmpty() ? "und" : supportedLocales.get(0));
            ULocale locale = ULocale.forLanguageTag(supportedLocale);
            return UCharacter.toLowerCase(locale, s);
        }

        /**
         * 15.5.4.18 String.prototype.toUpperCase ( )
         */
        @Function(name = "toUpperCase", arity = 0)
        public static Object toUpperCase(ExecutionContext cx, Object thisValue) {
            Object obj = CheckObjectCoercible(cx, thisValue);
            String s = ToFlatString(cx, obj);
            return s.toUpperCase(Locale.ROOT);
        }

        /**
         * 15.5.4.19 String.prototype.toLocaleUpperCase ( )
         */
        @Function(name = "toLocaleUpperCase", arity = 0)
        public static Object toLocaleUpperCase(ExecutionContext cx, Object thisValue, Object locales) {
            Object obj = CheckObjectCoercible(cx, thisValue);
            String s = ToFlatString(cx, obj);

            // ES5/6
            // return s.toUpperCase(cx.getRealm().getLocale());

            Set<String> requestedLocales = CanonicalizeLocaleList(cx, locales);
            int len = requestedLocales.size();
            String requestedLocale = (len > 0 ? requestedLocales.iterator().next()
                    : DefaultLocale(cx.getRealm()));
            Set<String> availableLocales = new HashSet<>(Arrays.asList("az", "lt", "tr"));
            // FIXME: spec issue? spec should just call LookupSupportedLocales abstract operation..
            List<String> supportedLocales = LookupSupportedLocales(cx, availableLocales,
                    Collections.singleton(requestedLocale));
            String supportedLocale = (supportedLocales.isEmpty() ? "und" : supportedLocales.get(0));
            ULocale locale = ULocale.forLanguageTag(supportedLocale);
            return UCharacter.toUpperCase(locale, s);
        }

        /**
         * 15.5.4.20 String.prototype.trim ( )
         */
        @Function(name = "trim", arity = 0)
        public static Object trim(ExecutionContext cx, Object thisValue) {
            Object obj = CheckObjectCoercible(cx, thisValue);
            CharSequence s = ToString(cx, obj);
            return Strings.trim(s);
        }

        /**
         * 15.5.4.21 String.prototype.repeat (count)
         */
        @Function(name = "repeat", arity = 1)
        public static Object repeat(ExecutionContext cx, Object thisValue, Object count) {
            Object obj = CheckObjectCoercible(cx, thisValue);
            String s = ToFlatString(cx, obj);
            double n = ToInteger(cx, count);
            if (n == 0) {
                return "";
            } else if (n < 0 || n == Double.POSITIVE_INFINITY) {
                throw throwRangeError(cx, Messages.Key.InvalidStringRepeat);
            }
            int capacity = Math.max(s.length() * (int) n, 0);
            StringBuilder t = new StringBuilder(capacity);
            for (int c = (int) n; c > 0; --c) {
                t.append(s);
            }
            return t.toString();
        }

        /**
         * 15.5.4.22 String.prototype.startsWith (searchString [, position ] )
         */
        @Function(name = "startsWith", arity = 1)
        public static Object startsWith(ExecutionContext cx, Object thisValue, Object searchString,
                Object position) {
            Object obj = CheckObjectCoercible(cx, thisValue);
            String s = ToFlatString(cx, obj);
            String searchStr = ToFlatString(cx, searchString);
            double pos = ToInteger(cx, position);
            int len = s.length();
            int start = (int) Math.min(Math.max(pos, 0), len);
            int searchLength = searchStr.length();
            if (searchLength + start > len) {
                return false;
            }
            return s.startsWith(searchStr, start);
        }

        /**
         * 15.5.4.23 String.prototype.endsWith (searchString [, endPosition] )
         */
        @Function(name = "endsWith", arity = 1)
        public static Object endsWith(ExecutionContext cx, Object thisValue, Object searchString,
                Object endPosition) {
            Object obj = CheckObjectCoercible(cx, thisValue);
            String s = ToFlatString(cx, obj);
            String searchStr = ToFlatString(cx, searchString);
            int len = s.length();
            double pos = Type.isUndefined(endPosition) ? len : ToInteger(cx, endPosition);
            int end = (int) Math.min(Math.max(pos, 0), len);
            int searchLength = searchStr.length();
            int start = end - searchLength;
            if (start < 0) {
                return false;
            }
            return s.startsWith(searchStr, start);
        }

        /**
         * 15.5.4.24 String.prototype.contains (searchString, position = 0 )
         */
        @Function(name = "contains", arity = 1)
        public static Object contains(ExecutionContext cx, Object thisValue, Object searchString,
                Object position /* = 0 */) {
            Object obj = CheckObjectCoercible(cx, thisValue);
            String s = ToFlatString(cx, obj);
            String searchStr = ToFlatString(cx, searchString);
            double pos = ToInteger(cx, position);
            int len = s.length();
            int start = (int) Math.min(Math.max(pos, 0), len);
            // int searchLen = searchStr.length();
            return s.indexOf(searchStr, start) != -1;
        }

        /**
         * 15.5.4.25 String.prototype.codePointAt (pos)
         */
        @Function(name = "codePointAt", arity = 1)
        public static Object codePointAt(ExecutionContext cx, Object thisValue, Object pos) {
            Object obj = CheckObjectCoercible(cx, thisValue);
            String s = ToFlatString(cx, obj);
            double position = ToInteger(cx, pos);
            int size = s.length();
            if (position < 0 || position >= size) {
                // FIXME: spec bug undefined /= NaN (Bug 1153)
                return UNDEFINED;
            }
            /* step 8-12 */
            return s.codePointAt((int) position);
        }

        /**
         * 15.5.4.26 String.prototype.normalize ( form = "NFC" )
         */
        @Function(name = "normalize", arity = 1)
        public static Object normalize(ExecutionContext cx, Object thisValue, Object form) {
            Object obj = CheckObjectCoercible(cx, thisValue);
            CharSequence s = ToString(cx, obj);
            String f = "NFC";
            if (!Type.isUndefined(form)) {
                f = ToFlatString(cx, form);
            }
            if (!("NFC".equals(f) || "NFD".equals(f) || "NFKC".equals(f) || "NFKD".equals(f))) {
                throw throwRangeError(cx, Messages.Key.InvalidNormalizationForm);
            }
            return Normalizer.normalize(s, Normalizer.Form.valueOf(f));
        }

        /**
         * B.2.2.1 String.prototype.substr (start, length)
         */
        @Function(name = "substr", arity = 2)
        public static Object substr(ExecutionContext cx, Object thisValue, Object start,
                Object length) {
            Object obj = CheckObjectCoercible(cx, thisValue);
            String s = ToFlatString(cx, obj);
            double intStart = ToInteger(cx, start);
            double end = (Type.isUndefined(length) ? Double.POSITIVE_INFINITY : ToInteger(cx,
                    length));
            int size = s.length();
            if (intStart < 0) {
                intStart = Math.max(size + intStart, 0);
            }
            double resultLength = Math.min(Math.max(end, 0), size - intStart);
            if (resultLength <= 0) {
                return "";
            }
            assert 0 <= intStart && intStart + resultLength <= size;
            return s.substring((int) intStart, (int) (intStart + resultLength));
        }

        /**
         * Abstract operation CreateHTML
         */
        private static String CreateHTML(ExecutionContext cx, Object string, String tag,
                String attribute, Object value) {
            Object str = CheckObjectCoercible(cx, string);
            String s = ToFlatString(cx, str);
            StringBuilder p = new StringBuilder().append("<").append(tag);
            if (!attribute.isEmpty()) {
                String v = ToFlatString(cx, value);
                String escapedV = v.replace("\"", "&quot;");
                p.append(" ").append(attribute).append("=").append('"').append(escapedV)
                        .append('"');
            }
            // FIXME: spec bug (string 'S' not added to final string) (Bug 1411)
            return p.append(">").append(s).append("</").append(tag).append(">").toString();
        }

        /**
         * B.2.2.2 String.prototype.anchor ( name )
         */
        @Function(name = "anchor", arity = 1)
        public static Object anchor(ExecutionContext cx, Object thisValue, Object name) {
            Object s = thisValue;
            return CreateHTML(cx, s, "a", "name", name);
        }

        /**
         * B.2.2.3 String.prototype.big ()
         */
        @Function(name = "big", arity = 0)
        public static Object big(ExecutionContext cx, Object thisValue) {
            Object s = thisValue;
            return CreateHTML(cx, s, "big", "", "");
        }

        /**
         * B.2.2.4 String.prototype.blink ()
         */
        @Function(name = "blink", arity = 0)
        public static Object blink(ExecutionContext cx, Object thisValue) {
            Object s = thisValue;
            return CreateHTML(cx, s, "blink", "", "");
        }

        /**
         * B.2.2.5 String.prototype.bold ()
         */
        @Function(name = "bold", arity = 0)
        public static Object bold(ExecutionContext cx, Object thisValue) {
            Object s = thisValue;
            return CreateHTML(cx, s, "b", "", "");
        }

        /**
         * B.2.2.6 String.prototype.fixed ()
         */
        @Function(name = "fixed", arity = 0)
        public static Object fixed(ExecutionContext cx, Object thisValue) {
            Object s = thisValue;
            return CreateHTML(cx, s, "tt", "", "");
        }

        /**
         * B.2.2.7 String.prototype.fontcolor ( color )
         */
        @Function(name = "fontcolor", arity = 1)
        public static Object fontcolor(ExecutionContext cx, Object thisValue, Object color) {
            Object s = thisValue;
            return CreateHTML(cx, s, "font", "color", color);
        }

        /**
         * B.2.2.8 String.prototype.fontsize ( size )
         */
        @Function(name = "fontsize", arity = 1)
        public static Object fontsize(ExecutionContext cx, Object thisValue, Object size) {
            Object s = thisValue;
            return CreateHTML(cx, s, "font", "size", size);
        }

        /**
         * B.2.2.9 String.prototype.italics ()
         */
        @Function(name = "italics", arity = 0)
        public static Object italics(ExecutionContext cx, Object thisValue) {
            Object s = thisValue;
            return CreateHTML(cx, s, "i", "", "");
        }

        /**
         * B.2.2.10 String.prototype.link ( url )
         */
        @Function(name = "link", arity = 1)
        public static Object link(ExecutionContext cx, Object thisValue, Object url) {
            Object s = thisValue;
            return CreateHTML(cx, s, "a", "href", url);
        }

        /**
         * B.2.2.11 String.prototype.small ()
         */
        @Function(name = "small", arity = 0)
        public static Object small(ExecutionContext cx, Object thisValue) {
            Object s = thisValue;
            return CreateHTML(cx, s, "small", "", "");
        }

        /**
         * B.2.2.12 String.prototype.strike ()
         */
        @Function(name = "strike", arity = 0)
        public static Object strike(ExecutionContext cx, Object thisValue) {
            Object s = thisValue;
            return CreateHTML(cx, s, "strike", "", "");
        }

        /**
         * B.2.2.13 String.prototype.sub ()
         */
        @Function(name = "sub", arity = 0)
        public static Object sub(ExecutionContext cx, Object thisValue) {
            Object s = thisValue;
            return CreateHTML(cx, s, "sub", "", "");
        }

        /**
         * B.2.2.14 String.prototype.sup ()
         */
        @Function(name = "sup", arity = 0)
        public static Object sup(ExecutionContext cx, Object thisValue) {
            Object s = thisValue;
            return CreateHTML(cx, s, "sup", "", "");
        }
    }
}
