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
import static com.github.anba.es6draft.runtime.objects.intl.CollatorPrototype.CompareStrings;
import static com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.BestAvailableLocale;
import static com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.CanonicalizeLocaleList;
import static com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.DefaultLocale;
import static com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.StripUnicodeLocaleExtension;
import static com.github.anba.es6draft.runtime.objects.text.RegExpConstructor.RegExpCreate;
import static com.github.anba.es6draft.runtime.objects.text.StringIteratorPrototype.CreateStringIterator;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ArrayObject.ArrayCreate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.CompatibilityExtension;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.runtime.objects.intl.CollatorConstructor;
import com.github.anba.es6draft.runtime.objects.intl.CollatorObject;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.github.anba.es6draft.runtime.types.builtins.StringObject;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.util.ULocale;

/**
 * <h1>21 Text Processing</h1><br>
 * <h2>21.1 String Objects</h2>
 * <ul>
 * <li>21.1.3 Properties of the String Prototype Object
 * <li>21.1.4 Properties of String Instances
 * </ul>
 */
public final class StringPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new String prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public StringPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
        createProperties(realm, this, AdditionalProperties.class);
    }

    /**
     * 21.1.3 Properties of the String Prototype Object
     */
    public enum Properties {
        ;

        /**
         * Abstract operation thisStringValue(value)
         * 
         * @param cx
         *            the execution context
         * @param object
         *            the object value
         * @return the string value
         */
        private static CharSequence thisStringValue(ExecutionContext cx, Object object) {
            /* step 1 */
            if (Type.isString(object)) {
                return Type.stringValue(object);
            }
            /* step 2 */
            if (object instanceof StringObject) {
                return ((StringObject) object).getStringData();
            }
            /* step 3 */
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 21.1.3.5 String.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.String;

        /**
         * 21.1.3.23 String.prototype.toString ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the string representation
         */
        @Function(name = "toString", arity = 0)
        public static Object toString(ExecutionContext cx, Object thisValue) {
            return thisStringValue(cx, thisValue);
        }

        /**
         * 21.1.3.26 String.prototype.valueOf ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the string value
         */
        @Function(name = "valueOf", arity = 0)
        public static Object valueOf(ExecutionContext cx, Object thisValue) {
            return thisStringValue(cx, thisValue);
        }

        /**
         * 21.1.3.1 String.prototype.charAt (pos)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param pos
         *            the string index
         * @return the character or the empty string
         */
        @Function(name = "charAt", arity = 1)
        public static Object charAt(ExecutionContext cx, Object thisValue, Object pos) {
            /* step 1 */
            Object obj = RequireObjectCoercible(cx, thisValue);
            /* steps 2-3 */
            CharSequence s = ToString(cx, obj);
            /* steps 4-5 */
            double position = ToInteger(cx, pos);
            /* step 6 */
            int size = s.length();
            /* step 7 */
            if (position < 0 || position >= size) {
                return "";
            }
            /* step 8 */
            return String.valueOf(s.charAt((int) position));
        }

        /**
         * 21.1.3.2 String.prototype.charCodeAt (pos)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param pos
         *            the string index
         * @return the character code unit
         */
        @Function(name = "charCodeAt", arity = 1)
        public static Object charCodeAt(ExecutionContext cx, Object thisValue, Object pos) {
            /* step 1 */
            Object obj = RequireObjectCoercible(cx, thisValue);
            /* steps 2-3 */
            CharSequence s = ToString(cx, obj);
            /* steps 4-5 */
            double position = ToInteger(cx, pos);
            /* step 6 */
            int size = s.length();
            /* step 7 */
            if (position < 0 || position >= size) {
                return Double.NaN;
            }
            /* step 8 */
            return (int) s.charAt((int) position);
        }

        /**
         * 21.1.3.4 String.prototype.concat ( ...args )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param args
         *            the additional strings
         * @return the concatenated string
         */
        @Function(name = "concat", arity = 1)
        public static Object concat(ExecutionContext cx, Object thisValue, Object... args) {
            /* step 1 */
            Object obj = RequireObjectCoercible(cx, thisValue);
            /* steps 2-3 */
            CharSequence s = ToString(cx, obj);
            /* step 4 (omitted) */
            /* step 5 */
            StringBuilder r = new StringBuilder(s);
            /* step 6 */
            for (int i = 0; i < args.length; ++i) {
                Object next = args[i];
                CharSequence nextString = ToString(cx, next);
                r.append(nextString);
            }
            /* step 7 */
            return r.toString();
        }

        /**
         * 21.1.3.8 String.prototype.indexOf ( searchString [ , position ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param searchString
         *            the search string value
         * @param position
         *            the start position
         * @return the result index
         */
        @Function(name = "indexOf", arity = 1)
        public static Object indexOf(ExecutionContext cx, Object thisValue, Object searchString,
                Object position) {
            /* step 1 */
            Object obj = RequireObjectCoercible(cx, thisValue);
            /* steps 2-3 */
            String s = ToFlatString(cx, obj);
            /* steps 4-5 */
            String searchStr = ToFlatString(cx, searchString);
            /* steps 6-7 */
            double pos = ToInteger(cx, position);
            /* step 8 */
            int len = s.length();
            /* step 9 */
            int start = (int) Math.min(Math.max(pos, 0), len);
            /* steps 10-11 */
            return s.indexOf(searchStr, start);
        }

        /**
         * 21.1.3.9 String.prototype.lastIndexOf ( searchString [ , position ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param searchString
         *            the search string value
         * @param position
         *            the start position
         * @return the result index
         */
        @Function(name = "lastIndexOf", arity = 1)
        public static Object lastIndexOf(ExecutionContext cx, Object thisValue,
                Object searchString, Object position) {
            /* step 1 */
            Object obj = RequireObjectCoercible(cx, thisValue);
            /* steps 2-3 */
            String s = ToFlatString(cx, obj);
            /* steps 4-5 */
            String searchStr = ToFlatString(cx, searchString);
            /* steps 6-7 */
            double numPos = ToNumber(cx, position);
            /* step 8 */
            double pos = Double.isNaN(numPos) ? Double.POSITIVE_INFINITY : ToInteger(numPos);
            /* step 9 */
            int len = s.length();
            /* step 10 */
            int start = (int) Math.min(Math.max(pos, 0), len);
            /* steps 11-12 */
            return s.lastIndexOf(searchStr, start);
        }

        /**
         * 21.1.3.10 String.prototype.localeCompare ( that [, reserved1 [ , reserved2 ] ] )<br>
         * 13.1.1 String.prototype.localeCompare (that [, locales [, options ]])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param that
         *            the other string
         * @param locales
         *            the optional locales array
         * @param options
         *            the optional options object
         * @return the locale specific comparison result
         */
        @Function(name = "localeCompare", arity = 1)
        public static Object localeCompare(ExecutionContext cx, Object thisValue, Object that,
                Object locales, Object options) {
            /* step 1 */
            Object obj = RequireObjectCoercible(cx, thisValue);
            /* steps 2-3 */
            String s = ToFlatString(cx, obj);
            /* steps 4-5 */
            String t = ToFlatString(cx, that);

            // ES5/6
            // return cx.getRealm().getCollator().compare(s, t);

            // ECMA-402
            /* step 7 */
            CollatorConstructor ctor = (CollatorConstructor) cx
                    .getIntrinsic(Intrinsics.Intl_Collator);
            CollatorObject collator = ctor.construct(cx, ctor, locales, options);
            /* steps 8-9 */
            return CompareStrings(cx, collator, s, t);
        }

        /**
         * 21.1.3.11 String.prototype.match (regexp)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param regexp
         *            the regular expression object
         * @return the match result array
         */
        @Function(name = "match", arity = 1)
        public static Object match(ExecutionContext cx, Object thisValue, Object regexp) {
            /* steps 1-2 */
            Object obj = RequireObjectCoercible(cx, thisValue);
            /* step 3 */
            if (!Type.isUndefined(regexp)) {
                /* steps 3.a-b */
                Callable matcher = GetMethod(cx, regexp, BuiltinSymbol.match.get());
                /* step 3.c */
                if (matcher != null) {
                    return matcher.call(cx, regexp, obj);
                }
            }
            /* steps 4-5 */
            CharSequence s = ToString(cx, obj);
            /* steps 6-7 */
            RegExpObject rx = RegExpCreate(cx, regexp, UNDEFINED);
            /* step 8 */
            return Invoke(cx, rx, BuiltinSymbol.match.get(), s);
        }

        /**
         * 21.1.3.14 String.prototype.replace (searchValue, replaceValue)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param searchValue
         *            the search string
         * @param replaceValue
         *            the replace string or replacer function
         * @return the new string
         */
        @Function(name = "replace", arity = 2)
        public static Object replace(ExecutionContext cx, Object thisValue, Object searchValue,
                Object replaceValue) {
            /* steps 1-2 */
            Object obj = RequireObjectCoercible(cx, thisValue);
            /* step 3 */
            if (!Type.isUndefined(searchValue)) {
                /* steps 3.a-b */
                Callable replacer = GetMethod(cx, searchValue, BuiltinSymbol.replace.get());
                /* step 3.c */
                if (replacer != null) {
                    return replacer.call(cx, searchValue, obj, replaceValue);
                }
            }
            /* steps 4-5 */
            String string = ToFlatString(cx, obj);
            /* steps 6-7 */
            String searchString = ToFlatString(cx, searchValue);
            /* step 8 */
            boolean functionalReplace = IsCallable(replaceValue);
            /* step 9 */
            String replaceValueString = null;
            Callable replaceValueCallable = null;
            if (!functionalReplace) {
                replaceValueString = ToFlatString(cx, replaceValue);
            } else {
                replaceValueCallable = (Callable) replaceValue;
            }
            /* step 10 */
            int pos = string.indexOf(searchString);
            if (pos < 0) {
                return string;
            }
            String matched = searchString;
            /* steps 11-12 */
            String replStr;
            if (functionalReplace) {
                Object replValue = replaceValueCallable.call(cx, UNDEFINED, matched, pos, string);
                replStr = ToFlatString(cx, replValue);
            } else {
                replStr = GetReplaceSubstitution(matched, string, pos, replaceValueString);
            }
            /* step 13 */
            int tailPos = pos + searchString.length();
            /* steps 14-15 */
            return string.substring(0, pos) + replStr + string.substring(tailPos);
        }

        /**
         * 21.1.3.14.1 Runtime Semantics: GetReplaceSubstitution Abstract Operation
         * 
         * @param matched
         *            the matched substring
         * @param string
         *            the string
         * @param position
         *            the string index
         * @param replacement
         *            the replacement value
         * @return the replacement string
         */
        private static String GetReplaceSubstitution(String matched, String string, int position,
                String replacement) {
            /* step 1 (not applicable) */
            /* step 2 */
            int matchLength = matched.length();
            /* step 3 (not applicable) */
            /* step 4 */
            int stringLength = string.length();
            /* steps 5-6 */
            assert position >= 0 && position <= stringLength;
            /* steps 7-8 (not applicable) */
            /* step 9 */
            int tailPos = position + matchLength;
            assert tailPos >= 0 && tailPos <= stringLength;
            /* step 10 (not applicable) */

            /* step 11 */
            StringBuilder result = new StringBuilder();
            for (int cursor = 0, len = replacement.length(); cursor < len;) {
                char c = replacement.charAt(cursor++);
                if (c == '$' && cursor < len) {
                    c = replacement.charAt(cursor++);
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

            /* step 12 */
            return result.toString();
        }

        /**
         * 21.1.3.15 String.prototype.search (regexp)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param regexp
         *            the regular expression object
         * @return the first match index
         */
        @Function(name = "search", arity = 1)
        public static Object search(ExecutionContext cx, Object thisValue, Object regexp) {
            /* steps 1-2 */
            Object obj = RequireObjectCoercible(cx, thisValue);
            /* step 3 */
            if (!Type.isUndefined(regexp)) {
                /* steps 3.a-b */
                Callable searcher = GetMethod(cx, regexp, BuiltinSymbol.search.get());
                /* step 3.c */
                if (searcher != null) {
                    return searcher.call(cx, regexp, obj);
                }
            }
            /* steps 4-5 */
            CharSequence string = ToString(cx, obj);
            /* steps 6-7 */
            RegExpObject rx = RegExpCreate(cx, regexp, UNDEFINED);
            /* step 8 */
            return Invoke(cx, rx, BuiltinSymbol.search.get(), string);
        }

        /**
         * 21.1.3.16 String.prototype.slice (start, end)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param start
         *            the start position
         * @param end
         *            the end position
         * @return the substring
         */
        @Function(name = "slice", arity = 2)
        public static Object slice(ExecutionContext cx, Object thisValue, Object start, Object end) {
            /* step 1 */
            Object obj = RequireObjectCoercible(cx, thisValue);
            /* steps 2-3 */
            CharSequence s = ToString(cx, obj);
            /* step 4 */
            int len = s.length();
            /* steps 5-6 */
            double intStart = ToInteger(cx, start);
            /* steps 7-8 */
            double intEnd = Type.isUndefined(end) ? len : ToInteger(cx, end);
            /* step 9 */
            int from = (int) (intStart < 0 ? Math.max(len + intStart, 0) : Math.min(intStart, len));
            /* step 10 */
            int to = (int) (intEnd < 0 ? Math.max(len + intEnd, 0) : Math.min(intEnd, len));
            /* step 11 */
            int span = Math.max(to - from, 0);
            /* step 12 */
            return s.subSequence(from, from + span);
        }

        /**
         * 21.1.3.17 String.prototype.split (separator, limit)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param separator
         *            the string separator
         * @param limit
         *            the optional split array limit
         * @return the split array
         */
        @Function(name = "split", arity = 2)
        public static Object split(ExecutionContext cx, Object thisValue, Object separator,
                Object limit) {
            /* steps 1-2 */
            Object obj = RequireObjectCoercible(cx, thisValue);
            /* step 3 */
            if (!Type.isUndefined(separator)) {
                /* steps 3.a-b */
                Callable splitter = GetMethod(cx, separator, BuiltinSymbol.split.get());
                /* step 3.c */
                if (splitter != null) {
                    return splitter.call(cx, separator, obj, limit);
                }
            }
            /* steps 4-5 */
            String s = ToFlatString(cx, obj);
            /* step 6 */
            ArrayObject a = ArrayCreate(cx, 0);
            /* step 7 */
            int lengthA = 0;
            /* steps 8-9 */
            long lim = Type.isUndefined(limit) ? 0x1F_FFFF_FFFF_FFFFL : ToLength(cx, limit);
            /* step 10 */
            int size = s.length();
            /* step 11 */
            int p = 0;
            /* steps 12-13 */
            String r = ToFlatString(cx, separator);
            /* step 14 */
            if (lim == 0) {
                return a;
            }
            /* step 15 */
            if (Type.isUndefined(separator)) {
                CreateDataProperty(cx, a, 0, s);
                return a;
            }
            /* step 16 */
            if (size == 0) {
                if (r.length() == 0) {
                    return a;
                }
                CreateDataProperty(cx, a, 0, s);
                return a;
            }
            /* step 17 */
            int q = p;
            /* step 18 */
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
                        CreateDataProperty(cx, a, lengthA, t);
                        lengthA += 1;
                        if (lengthA == lim) {
                            return a;
                        }
                        p = e;
                        q = p;
                    }
                }
            }
            /* step 19 */
            String t = s.substring(p, size);
            /* steps 20-21 */
            CreateDataProperty(cx, a, lengthA, t);
            /* step 22 */
            return a;
        }

        /**
         * 21.1.3.17.1 Runtime Semantics: SplitMatch Abstract Operation
         * 
         * @param s
         *            the string
         * @param q
         *            the start position
         * @param r
         *            the search string
         * @return the index of the first match
         */
        public static int SplitMatch(String s, int q, String r) {
            // returns start instead of end position
            return s.indexOf(r, q);
        }

        /**
         * 21.1.3.19 String.prototype.substring (start, end)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param start
         *            the start position
         * @param end
         *            the end position
         * @return the substring
         */
        @Function(name = "substring", arity = 2)
        public static Object substring(ExecutionContext cx, Object thisValue, Object start,
                Object end) {
            /* step 1 */
            Object obj = RequireObjectCoercible(cx, thisValue);
            /* steps 2-3 */
            CharSequence s = ToString(cx, obj);
            /* step 4 */
            int len = s.length();
            /* steps 5-6 */
            double intStart = ToInteger(cx, start);
            /* steps 7-8 */
            double intEnd = Type.isUndefined(end) ? len : ToInteger(cx, end);
            /* step 9 */
            int finalStart = (int) Math.min(Math.max(intStart, 0), len);
            /* step 10 */
            int finalEnd = (int) Math.min(Math.max(intEnd, 0), len);
            /* step 11 */
            int from = Math.min(finalStart, finalEnd);
            /* step 12 */
            int to = Math.max(finalStart, finalEnd);
            /* step 13 */
            return s.subSequence(from, to);
        }

        /**
         * 21.1.3.22 String.prototype.toLowerCase ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the lower case string
         */
        @Function(name = "toLowerCase", arity = 0)
        public static Object toLowerCase(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            Object obj = RequireObjectCoercible(cx, thisValue);
            /* steps 2-3 */
            String s = ToFlatString(cx, obj);
            /* steps 4-9 */
            return replaceIWithDot(s).toLowerCase(Locale.ROOT);
        }

        /**
         * 21.1.3.20 String.prototype.toLocaleLowerCase ( )<br>
         * 13.1.2 String.prototype.toLocaleLowerCase ([locales])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param locales
         *            the optional locales array
         * @return the lower case string
         */
        @Function(name = "toLocaleLowerCase", arity = 0)
        public static Object toLocaleLowerCase(ExecutionContext cx, Object thisValue, Object locales) {
            /* step 1 */
            Object obj = RequireObjectCoercible(cx, thisValue);
            /* steps 2-3 */
            String s = ToFlatString(cx, obj);

            // ES5/6
            // return s.toLowerCase(cx.getRealm().getLocale());

            /* step 4 */
            Set<String> requestedLocales = CanonicalizeLocaleList(cx, locales);
            /* steps 5-7 */
            String requestedLocale = !requestedLocales.isEmpty() ? requestedLocales.iterator()
                    .next() : DefaultLocale(cx.getRealm());
            /* step 8 */
            String noExtensionsLocale = StripUnicodeLocaleExtension(requestedLocale);
            /* step 9 */
            HashSet<String> availableLocales = new HashSet<>(Arrays.asList("az", "lt", "tr"));
            /* step 10 */
            String locale = BestAvailableLocale(availableLocales, noExtensionsLocale);
            /* step 11 */
            String supportedLocale = locale == null ? "und" : locale;
            /* steps 12-17 */
            return UCharacter.toLowerCase(ULocale.forLanguageTag(supportedLocale), s);
        }

        /**
         * 21.1.3.24 String.prototype.toUpperCase ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the upper case string
         */
        @Function(name = "toUpperCase", arity = 0)
        public static Object toUpperCase(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            Object obj = RequireObjectCoercible(cx, thisValue);
            /* steps 2-3 */
            String s = ToFlatString(cx, obj);
            /* steps 4-9 */
            return s.toUpperCase(Locale.ROOT);
        }

        /**
         * 21.1.3.21 String.prototype.toLocaleUpperCase ( )<br>
         * 13.1.3 String.prototype.toLocaleUpperCase ([locales ])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param locales
         *            the optional locales array
         * @return the upper case string
         */
        @Function(name = "toLocaleUpperCase", arity = 0)
        public static Object toLocaleUpperCase(ExecutionContext cx, Object thisValue, Object locales) {
            /* step 1 */
            Object obj = RequireObjectCoercible(cx, thisValue);
            /* steps 2-3 */
            String s = ToFlatString(cx, obj);

            // ES5/6
            // return s.toUpperCase(cx.getRealm().getLocale());

            /* step 4 */
            Set<String> requestedLocales = CanonicalizeLocaleList(cx, locales);
            /* steps 5-7 */
            String requestedLocale = !requestedLocales.isEmpty() ? requestedLocales.iterator()
                    .next() : DefaultLocale(cx.getRealm());
            /* step 8 */
            String noExtensionsLocale = StripUnicodeLocaleExtension(requestedLocale);
            /* step 9 */
            HashSet<String> availableLocales = new HashSet<>(Arrays.asList("az", "lt", "tr"));
            /* step 10 */
            String locale = BestAvailableLocale(availableLocales, noExtensionsLocale);
            /* step 11 */
            String supportedLocale = locale == null ? "und" : locale;
            /* steps 12-17 */
            return UCharacter.toUpperCase(ULocale.forLanguageTag(supportedLocale), s);
        }

        /**
         * 21.1.3.25 String.prototype.trim ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the string with leading and trailing whitespace removed
         */
        @Function(name = "trim", arity = 0)
        public static Object trim(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            Object obj = RequireObjectCoercible(cx, thisValue);
            /* steps 2-3 */
            String s = ToFlatString(cx, obj);
            /* steps 4-5 */
            return Strings.trim(s);
        }

        /**
         * 21.1.3.13 String.prototype.repeat (count)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param count
         *            the repetition count
         * @return the string repeated <var>count</var> times
         */
        @Function(name = "repeat", arity = 1)
        public static Object repeat(ExecutionContext cx, Object thisValue, Object count) {
            /* step 1 */
            Object obj = RequireObjectCoercible(cx, thisValue);
            /* steps 2-3 */
            String s = ToFlatString(cx, obj);
            /* steps 4-5 */
            double n = ToInteger(cx, count);
            /* steps 6-7 */
            if (n < 0 || n == Double.POSITIVE_INFINITY) {
                throw newRangeError(cx, Messages.Key.InvalidStringRepeat);
            }
            /* step 8 */
            if (n == 0 || s.length() == 0) {
                return "";
            }
            double capacity = s.length() * n;
            if (capacity > 1 << 27) {
                // likely to exceed heap space, follow SpiderMonkey and throw RangeError
                throw newRangeError(cx, Messages.Key.InvalidStringRepeat);
            }
            /* step 8 */
            StringBuilder t = new StringBuilder((int) capacity);
            for (int c = (int) n; c > 0; --c) {
                t.append(s);
            }
            /* step 9 */
            return t.toString();
        }

        /**
         * 21.1.3.18 String.prototype.startsWith (searchString [, position ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param searchString
         *            the search string
         * @param position
         *            the start position
         * @return {@code true} if the string starts with <var>searchString</var>
         */
        @Function(name = "startsWith", arity = 1)
        public static Object startsWith(ExecutionContext cx, Object thisValue, Object searchString,
                Object position) {
            /* step 1 */
            Object obj = RequireObjectCoercible(cx, thisValue);
            /* steps 2-3 */
            String s = ToFlatString(cx, obj);
            /* steps 4-6 */
            if (IsRegExp(cx, searchString)) {
                throw newTypeError(cx, Messages.Key.InvalidRegExpArgument);
            }
            /* steps 7-8 */
            String searchStr = ToFlatString(cx, searchString);
            /* steps 9-10 */
            double pos = ToInteger(cx, position);
            /* step 11 */
            int len = s.length();
            /* step 12 */
            int start = (int) Math.min(Math.max(pos, 0), len);
            /* step 13 */
            int searchLength = searchStr.length();
            /* step 14 */
            if (searchLength + start > len) {
                return false;
            }
            /* steps 15-16 */
            return s.startsWith(searchStr, start);
        }

        /**
         * 21.1.3.6 String.prototype.endsWith (searchString [, endPosition] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param searchString
         *            the search string
         * @param endPosition
         *            the end position
         * @return {@code true} if the string ends with <var>searchString</var>
         */
        @Function(name = "endsWith", arity = 1)
        public static Object endsWith(ExecutionContext cx, Object thisValue, Object searchString,
                Object endPosition) {
            /* step 1 */
            Object obj = RequireObjectCoercible(cx, thisValue);
            /* steps 2-3 */
            String s = ToFlatString(cx, obj);
            /* steps 4-6 */
            if (IsRegExp(cx, searchString)) {
                throw newTypeError(cx, Messages.Key.InvalidRegExpArgument);
            }
            /* steps 7-8 */
            String searchStr = ToFlatString(cx, searchString);
            /* step 9 */
            int len = s.length();
            /* steps 10-11 */
            double pos = Type.isUndefined(endPosition) ? len : ToInteger(cx, endPosition);
            /* step 12 */
            int end = (int) Math.min(Math.max(pos, 0), len);
            /* step 13 */
            int searchLength = searchStr.length();
            /* step 14 */
            int start = end - searchLength;
            /* step 15 */
            if (start < 0) {
                return false;
            }
            /* steps 16-17 */
            return s.startsWith(searchStr, start);
        }

        /**
         * 21.1.3.7 String.prototype.includes ( searchString [ , position ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param searchString
         *            the search string
         * @param position
         *            the start position
         * @return {@code true} if the search string was found
         */
        @Function(name = "includes", arity = 1)
        public static Object includes(ExecutionContext cx, Object thisValue, Object searchString,
                Object position /* = 0 */) {
            /* step 1 */
            Object obj = RequireObjectCoercible(cx, thisValue);
            /* steps 2-3 */
            String s = ToFlatString(cx, obj);
            /* steps 4-6 */
            if (IsRegExp(cx, searchString)) {
                throw newTypeError(cx, Messages.Key.InvalidRegExpArgument);
            }
            /* steps 7-8 */
            String searchStr = ToFlatString(cx, searchString);
            /* steps 9-10 */
            double pos = ToInteger(cx, position);
            /* step 11 */
            int len = s.length();
            /* step 12 */
            int start = (int) Math.min(Math.max(pos, 0), len);
            /* step 13 */
            // int searchLen = searchStr.length();
            /* step 14 */
            return s.indexOf(searchStr, start) != -1;
        }

        /**
         * 21.1.3.3 String.prototype.codePointAt (pos)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param pos
         *            the start position
         * @return the code point
         */
        @Function(name = "codePointAt", arity = 1)
        public static Object codePointAt(ExecutionContext cx, Object thisValue, Object pos) {
            /* step 1 */
            Object obj = RequireObjectCoercible(cx, thisValue);
            /* steps 2-3 */
            String s = ToFlatString(cx, obj);
            /* steps 4-5 */
            double position = ToInteger(cx, pos);
            /* step 6 */
            int size = s.length();
            /* step 7 */
            if (position < 0 || position >= size) {
                return UNDEFINED;
            }
            /* steps 8-12 */
            return s.codePointAt((int) position);
        }

        /**
         * 21.1.3.12 String.prototype.normalize ( [ form ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param form
         *            the normalisation form
         * @return the normalized string
         */
        @Function(name = "normalize", arity = 0)
        public static Object normalize(ExecutionContext cx, Object thisValue, Object form) {
            /* step 1 */
            Object obj = RequireObjectCoercible(cx, thisValue);
            /* steps 2-3 */
            String s = ToFlatString(cx, obj);
            /* steps 4-6 */
            String f = "NFC";
            if (!Type.isUndefined(form)) {
                f = ToFlatString(cx, form);
            }
            /* steps 7-9 */
            switch (f) {
            case "NFC":
                return Normalizer.normalize(s, Normalizer.NFC);
            case "NFD":
                return Normalizer.normalize(s, Normalizer.NFD);
            case "NFKC":
                return Normalizer.normalize(s, Normalizer.NFKC);
            case "NFKD":
                return Normalizer.normalize(s, Normalizer.NFKD);
            default:
                throw newRangeError(cx, Messages.Key.InvalidNormalizationForm);
            }
        }

        /**
         * 21.1.3.27 String.prototype [ @@iterator ]( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the string iterator
         */
        @Function(name = "[Symbol.iterator]", symbol = BuiltinSymbol.iterator, arity = 0)
        public static Object iterator(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            Object obj = RequireObjectCoercible(cx, thisValue);
            /* steps 2-3 */
            String s = ToFlatString(cx, obj);
            /* step 4 */
            return CreateStringIterator(cx, s);
        }
    }

    /**
     * B.2.3 Additional Properties of the String.prototype Object
     */
    @CompatibilityExtension(CompatibilityOption.StringPrototype)
    public enum AdditionalProperties {
        ;

        /**
         * B.2.3.1 String.prototype.substr (start, length)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param start
         *            the start position
         * @param length
         *            the substring length
         * @return the substring
         */
        @Function(name = "substr", arity = 2)
        public static Object substr(ExecutionContext cx, Object thisValue, Object start,
                Object length) {
            /* step 1 */
            Object obj = RequireObjectCoercible(cx, thisValue);
            /* steps 2-3 */
            String s = ToFlatString(cx, obj);
            /* steps 4-5 */
            double intStart = ToInteger(cx, start);
            /* steps 6-7 */
            double end = Type.isUndefined(length) ? Double.POSITIVE_INFINITY
                    : ToInteger(cx, length);
            /* step 8 */
            int size = s.length();
            /* step 9 */
            if (intStart < 0) {
                intStart = Math.max(size + intStart, 0);
            }
            /* step 10 */
            double resultLength = Math.min(Math.max(end, 0), size - intStart);
            /* step 11 */
            if (resultLength <= 0) {
                return "";
            }
            assert 0 <= intStart && intStart + resultLength <= size;
            /* step 12 */
            return s.substring((int) intStart, (int) (intStart + resultLength));
        }

        /**
         * B.2.3.2.1 Runtime Semantics: CreateHTML ( string, tag, attribute, value )
         * 
         * @param cx
         *            the execution context
         * @param string
         *            the string
         * @param tag
         *            the html tag
         * @param attribute
         *            the html attribute name
         * @param value
         *            the html attribute value
         * @return the html string
         */
        private static String CreateHTML(ExecutionContext cx, Object string, String tag,
                String attribute, Object value) {
            /* step 1 */
            Object str = RequireObjectCoercible(cx, string);
            /* steps 2-3 */
            String s = ToFlatString(cx, str);
            /* steps 4-5 */
            StringBuilder p = new StringBuilder().append("<").append(tag);
            if (!attribute.isEmpty()) {
                String v = ToFlatString(cx, value);
                String escapedV = v.replace("\"", "&quot;");
                p.append(" ").append(attribute).append("=").append('"').append(escapedV)
                        .append('"');
            }
            /* steps 6-9 */
            return p.append(">").append(s).append("</").append(tag).append(">").toString();
        }

        /**
         * B.2.3.2 String.prototype.anchor ( name )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param name
         *            the anchor name
         * @return the html string
         */
        @Function(name = "anchor", arity = 1)
        public static Object anchor(ExecutionContext cx, Object thisValue, Object name) {
            Object s = thisValue;
            return CreateHTML(cx, s, "a", "name", name);
        }

        /**
         * B.2.3.3 String.prototype.big ()
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the html string
         */
        @Function(name = "big", arity = 0)
        public static Object big(ExecutionContext cx, Object thisValue) {
            Object s = thisValue;
            return CreateHTML(cx, s, "big", "", "");
        }

        /**
         * B.2.3.4 String.prototype.blink ()
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the html string
         */
        @Function(name = "blink", arity = 0)
        public static Object blink(ExecutionContext cx, Object thisValue) {
            Object s = thisValue;
            return CreateHTML(cx, s, "blink", "", "");
        }

        /**
         * B.2.3.5 String.prototype.bold ()
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the html string
         */
        @Function(name = "bold", arity = 0)
        public static Object bold(ExecutionContext cx, Object thisValue) {
            Object s = thisValue;
            return CreateHTML(cx, s, "b", "", "");
        }

        /**
         * B.2.3.6 String.prototype.fixed ()
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the html string
         */
        @Function(name = "fixed", arity = 0)
        public static Object fixed(ExecutionContext cx, Object thisValue) {
            Object s = thisValue;
            return CreateHTML(cx, s, "tt", "", "");
        }

        /**
         * B.2.3.7 String.prototype.fontcolor ( color )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param color
         *            the font color
         * @return the html string
         */
        @Function(name = "fontcolor", arity = 1)
        public static Object fontcolor(ExecutionContext cx, Object thisValue, Object color) {
            Object s = thisValue;
            return CreateHTML(cx, s, "font", "color", color);
        }

        /**
         * B.2.3.8 String.prototype.fontsize ( size )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param size
         *            the font size
         * @return the html string
         */
        @Function(name = "fontsize", arity = 1)
        public static Object fontsize(ExecutionContext cx, Object thisValue, Object size) {
            Object s = thisValue;
            return CreateHTML(cx, s, "font", "size", size);
        }

        /**
         * B.2.3.9 String.prototype.italics ()
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the html string
         */
        @Function(name = "italics", arity = 0)
        public static Object italics(ExecutionContext cx, Object thisValue) {
            Object s = thisValue;
            return CreateHTML(cx, s, "i", "", "");
        }

        /**
         * B.2.3.10 String.prototype.link ( url )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param url
         *            the url
         * @return the html string
         */
        @Function(name = "link", arity = 1)
        public static Object link(ExecutionContext cx, Object thisValue, Object url) {
            Object s = thisValue;
            return CreateHTML(cx, s, "a", "href", url);
        }

        /**
         * B.2.3.11 String.prototype.small ()
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the html string
         */
        @Function(name = "small", arity = 0)
        public static Object small(ExecutionContext cx, Object thisValue) {
            Object s = thisValue;
            return CreateHTML(cx, s, "small", "", "");
        }

        /**
         * B.2.3.12 String.prototype.strike ()
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the html string
         */
        @Function(name = "strike", arity = 0)
        public static Object strike(ExecutionContext cx, Object thisValue) {
            Object s = thisValue;
            return CreateHTML(cx, s, "strike", "", "");
        }

        /**
         * B.2.3.13 String.prototype.sub ()
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the html string
         */
        @Function(name = "sub", arity = 0)
        public static Object sub(ExecutionContext cx, Object thisValue) {
            Object s = thisValue;
            return CreateHTML(cx, s, "sub", "", "");
        }

        /**
         * B.2.3.14 String.prototype.sup ()
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the html string
         */
        @Function(name = "sup", arity = 0)
        public static Object sup(ExecutionContext cx, Object thisValue) {
            Object s = thisValue;
            return CreateHTML(cx, s, "sup", "", "");
        }
    }

    /**
     * SpecialCasing support for u+0130 (LATIN CAPITAL LETTER I WITH DOT ABOVE) was removed in
     * Java8: https://bugs.openjdk.java.net/browse/JDK-8020037
     * 
     * @param s
     *            the string
     * @return the string with u+0130 replaced
     */
    private static String replaceIWithDot(String s) {
        if (ReplaceIWithDot.specialCasingAvailable) {
            return s;
        }
        return ReplaceIWithDot.replace(s);
    }

    private static final class ReplaceIWithDot {
        static final boolean specialCasingAvailable = "".toLowerCase(Locale.ROOT).length() == 2;

        static String replace(String s) {
            int index = s.indexOf('\u0130');
            if (index < 0) {
                return s;
            }
            // string contains at least one u+0130 character, translate to u+0069 u+0307
            return replace(s, index);
        }

        private static String replace(String s, int index) {
            final int length = s.length();
            final int space = Math.min(10, length);
            int offset = 0, remaining = space;
            char[] replacement = new char[length + space];
            s.getChars(0, index, replacement, 0);
            for (; index < length; ++index) {
                char c = s.charAt(index);
                if (c == '\u0130') {
                    if (remaining == 0) {
                        replacement = Arrays.copyOf(replacement, replacement.length + space);
                        remaining = space;
                    }
                    replacement[offset + index + 0] = '\u0069';
                    replacement[offset + index + 1] = '\u0307';
                    offset += 1;
                    remaining -= 1;
                } else {
                    replacement[offset + index] = c;
                }
            }
            return new String(replacement, 0, length + offset);
        }
    }
}
