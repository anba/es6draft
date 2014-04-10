/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.text;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.text.RegExpConstructor.EscapeRegExpPattern;
import static com.github.anba.es6draft.runtime.objects.text.RegExpConstructor.RegExpInitialize;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArray.ArrayCreate;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.regex.MatchResult;

import com.github.anba.es6draft.regexp.MatchState;
import com.github.anba.es6draft.regexp.RegExpMatcher;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.CompatibilityExtension;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.NativeFunction;
import com.github.anba.es6draft.runtime.types.builtins.NativeFunction.NativeFunctionId;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>21 Text Processing</h1><br>
 * <h2>21.2 RegExp (Regular Expression) Objects</h2>
 * <ul>
 * <li>21.2.5 Properties of the RegExp Prototype Object
 * <li>21.2.6 Properties of RegExp Instances
 * </ul>
 */
public final class RegExpPrototype extends OrdinaryObject implements Initialisable {
    public RegExpPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
        createProperties(cx, this, AdditionalProperties.class);
    }

    /**
     * 21.2.5 Properties of the RegExp Prototype Object
     */
    public enum Properties {
        ;

        private static RegExpObject thisRegExpValue(ExecutionContext cx, Object object) {
            if (object instanceof RegExpObject) {
                RegExpObject obj = (RegExpObject) object;
                if (obj.isInitialised()) {
                    return obj;
                }
                throw newTypeError(cx, Messages.Key.UninitialisedObject);
            }
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 21.2.5.1 RegExp.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.RegExp;

        /**
         * 21.2.5.2 RegExp.prototype.exec(string)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param string
         *            the string
         * @return the match result array
         */
        @Function(name = "exec", arity = 1, nativeId = NativeFunctionId.RegExpPrototypeExec)
        public static Object exec(ExecutionContext cx, Object thisValue, Object string) {
            /* steps 1-4 */
            RegExpObject r = thisRegExpValue(cx, thisValue);
            /* steps 5-6 */
            String s = ToFlatString(cx, string);
            /* step 7 */
            return RegExpExec(cx, r, s);
        }

        /**
         * 21.2.5.3 get RegExp.prototype.global
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the global flag
         */
        @Accessor(name = "global", type = Accessor.Type.Getter)
        public static Object global(ExecutionContext cx, Object thisValue) {
            /* steps 1-5 */
            RegExpObject r = thisRegExpValue(cx, thisValue);
            /* steps 6-7 */
            return r.getOriginalFlags().indexOf('g') != -1;
        }

        /**
         * 21.2.5.4 get RegExp.prototype.ignoreCase
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the ignoreCase flag
         */
        @Accessor(name = "ignoreCase", type = Accessor.Type.Getter)
        public static Object ignoreCase(ExecutionContext cx, Object thisValue) {
            /* steps 1-5 */
            RegExpObject r = thisRegExpValue(cx, thisValue);
            /* steps 6-7 */
            return r.getOriginalFlags().indexOf('i') != -1;
        }

        /**
         * 21.2.5.6 get RegExp.prototype.multiline
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the multiline flag
         */
        @Accessor(name = "multiline", type = Accessor.Type.Getter)
        public static Object multiline(ExecutionContext cx, Object thisValue) {
            /* steps 1-5 */
            RegExpObject r = thisRegExpValue(cx, thisValue);
            /* steps 6-7 */
            return r.getOriginalFlags().indexOf('m') != -1;
        }

        /**
         * 21.2.5.9 get RegExp.prototype.source
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the source property
         */
        @Accessor(name = "source", type = Accessor.Type.Getter)
        public static Object source(ExecutionContext cx, Object thisValue) {
            /* steps 1-7 */
            RegExpObject r = thisRegExpValue(cx, thisValue);
            /* step 8 */
            return EscapeRegExpPattern(r.getOriginalSource(), r.getOriginalFlags());
        }

        /**
         * 21.2.5.11 get RegExp.prototype.sticky
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the sticky flag
         */
        @Accessor(name = "sticky", type = Accessor.Type.Getter)
        public static Object sticky(ExecutionContext cx, Object thisValue) {
            /* steps 1-5 */
            RegExpObject r = thisRegExpValue(cx, thisValue);
            /* steps 6-7 */
            return r.getOriginalFlags().indexOf('y') != -1;
        }

        /**
         * 21.2.5.12 RegExp.prototype.test(string)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param string
         *            the string
         * @return {@code true} if the string matches the pattern
         */
        @Function(name = "test", arity = 1)
        public static Object test(ExecutionContext cx, Object thisValue, Object string) {
            /* steps 1-2 */
            if (!Type.isObject(thisValue)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            /* steps 3-4 */
            // inlined: Invoke(cx, Type.objectValue(thisValue), "exec", string);
            ScriptObject object = Type.objectValue(thisValue);
            Object func = object.get(cx, "exec", object);
            // special cased to avoid RegExp.prototype.exec match array creation
            if (func instanceof NativeFunction
                    && ((NativeFunction) func).getId() == NativeFunctionId.RegExpPrototypeExec
                    && ((NativeFunction) func).getRealm() == cx.getRealm()) {
                RegExpObject r = thisRegExpValue(cx, thisValue);
                String s = ToFlatString(cx, string);
                MatchResult m = getMatcherOrNull(cx, r, s);
                if (m == null) {
                    return false;
                }
                RegExpConstructor.storeLastMatchResult(cx, r, s, m);
                return true;
            }
            /* steps 3-4 (cont'ed) */
            if (!IsCallable(func)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Object match = ((Callable) func).call(cx, object, string);
            /* step 5 */
            return !Type.isNull(match);
        }

        /**
         * 21.2.5.14 get RegExp.prototype.unicode
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the unicode flag
         */
        @Accessor(name = "unicode", type = Accessor.Type.Getter)
        public static Object unicode(ExecutionContext cx, Object thisValue) {
            /* steps 1-5 */
            RegExpObject r = thisRegExpValue(cx, thisValue);
            /* steps 6-7 */
            return r.getOriginalFlags().indexOf('u') != -1;
        }

        /**
         * 21.2.5.13 RegExp.prototype.toString()
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the string representation
         */
        @Function(name = "toString", arity = 0)
        public static Object toString(ExecutionContext cx, Object thisValue) {
            /* steps 1-4 */
            RegExpObject r = thisRegExpValue(cx, thisValue);
            /* steps 5-6 */
            CharSequence source = ToString(cx, Get(cx, r, "source"));
            if (source.length() == 0) {
                source = "(?:)";
            }
            /* step 7 */
            StringBuilder result = new StringBuilder().append('/').append(source).append('/');
            /* steps 8-10 */
            if (ToBoolean(Get(cx, r, "global"))) {
                result.append('g');
            }
            /* steps 11-13 */
            if (ToBoolean(Get(cx, r, "ignoreCase"))) {
                result.append('i');
            }
            /* steps 14-16 */
            if (ToBoolean(Get(cx, r, "multiline"))) {
                result.append('m');
            }
            /* steps 17-19 */
            if (ToBoolean(Get(cx, r, "unicode"))) {
                result.append('u');
            }
            /* steps 20-22 */
            if (ToBoolean(Get(cx, r, "sticky"))) {
                result.append('y');
            }
            /* step 23 */
            return result.toString();
        }

        /**
         * 21.2.5.5 RegExp.prototype.match (string)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param string
         *            the string
         * @return the match result array
         */
        @Function(name = "match", arity = 1)
        public static Object match(ExecutionContext cx, Object thisValue, Object string) {
            /* steps 1-4 */
            RegExpObject rx = thisRegExpValue(cx, thisValue);
            /* steps 5-6 */
            String s = ToFlatString(cx, string);
            /* steps 7-8 */
            boolean global = ToBoolean(Get(cx, rx, "global"));
            if (!global) {
                /* step 9 */
                return RegExpExec(cx, rx, s);
            } else {
                /* step 10 */
                Put(cx, rx, "lastIndex", 0, true);
                ScriptObject array = ArrayCreate(cx, 0);
                int n = 0;
                boolean lastMatch = true;
                MatchResult lastMatchResult = null;
                while (lastMatch) {
                    // Object result = RegExpExec(realm, rx, s);
                    MatchResult result = getMatcherOrNull(cx, rx, s);
                    if (result == null) {
                        lastMatch = false;
                    } else {
                        lastMatchResult = result;
                        // FIXME: spec issue (bug 1467)
                        if (result.start() == result.end()) {
                            int thisIndex = (int) ToInteger(cx, Get(cx, rx, "lastIndex"));
                            Put(cx, rx, "lastIndex", thisIndex + 1, true);
                        }
                        // Object matchStr = Get(Type.objectValue(result), "0");
                        String matchStr = s.substring(result.start(), result.end());
                        CreateDataPropertyOrThrow(cx, array, ToString(n), matchStr);
                        n += 1;
                    }
                }
                if (n == 0) {
                    return NULL;
                }
                RegExpConstructor.storeLastMatchResult(cx, rx, s, lastMatchResult);
                return array;
            }
        }

        /**
         * 21.2.5.7 RegExp.prototype.replace (S, replaceValue)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param string
         *            the string
         * @param replaceValue
         *            the replace string or replacer function
         * @return the new string
         */
        @Function(name = "replace", arity = 2)
        public static Object replace(ExecutionContext cx, Object thisValue, Object string,
                Object replaceValue) {
            /* steps 1-4 */
            RegExpObject rx = thisRegExpValue(cx, thisValue);
            /* step 5 (not applicable) */
            /* steps 6-7 */
            String s = ToFlatString(cx, string);
            /* step 8 */
            boolean functionalReplace = IsCallable(replaceValue);
            // FIXME: spec issue - always call ToString(replValue) even if no match
            String replaceValueString = null;
            Callable replaceValueCallable = null;
            if (!functionalReplace) {
                replaceValueString = ToFlatString(cx, replaceValue);
            } else {
                replaceValueCallable = (Callable) replaceValue;
            }
            /* steps 9-10 */
            boolean global = ToBoolean(Get(cx, rx, "global"));
            /* step 13 */
            if (global) {
                Put(cx, rx, "lastIndex", 0, true);
            }
            /* step 14 */
            // int previousLastIndex = 0;
            /* step 15 */
            boolean done = false;
            /* step 16 */
            ArrayList<MatchResult> matches = new ArrayList<MatchResult>();
            while (!done) {
                /* step 16.a-16.b */
                // Object result = RegExpExec(realm, rx, s);
                MatchResult result = getMatcherOrNull(cx, rx, s);
                if (result == null) {
                    /* step 16.c */
                    done = true;
                } else {
                    matches.add(result);
                    /* step 16.d */
                    if (global) {
                        int thisIndex = (int) ToInteger(cx, Get(cx, rx, "lastIndex"));
                        // FIXME: spec issue (bug 1467)
                        // if (thisIndex == previousLastIndex) {
                        if (result.start() == result.end()) {
                            Put(cx, rx, "lastIndex", thisIndex + 1, true);
                            // previousLastIndex = thisIndex + 1;
                        } else {
                            // previousLastIndex = thisIndex;
                        }
                    }
                    // FIXME: spec issue (bug 2618)
                    if (!global) {
                        done = true;
                    }
                }
            }
            // fast-path if no match was found
            if (matches.isEmpty()) {
                return s;
            }
            // FIXME: spec issue - create replacement after match (bug 2617)
            /* step 11 */
            StringBuilder accumulatedResult = new StringBuilder();
            /* step 12 */
            int nextSrcPosition = 0;
            MatchResult lastMatchResult = null;
            for (MatchResult result : matches) {
                lastMatchResult = result;
                // String matched = result.group();
                int position = result.start();
                // GroupIterator captures = new GroupIterator(rx, result);
                String replacement;
                if (functionalReplace) {
                    RegExpConstructor.storeLastMatchResult(cx, rx, s, result);
                    Object[] replacerArgs = GetReplacerArguments(rx, result, s);
                    Object replValue = replaceValueCallable.call(cx, UNDEFINED, replacerArgs);
                    replacement = ToFlatString(cx, replValue);
                } else {
                    replacement = GetReplaceSubstitution(rx, result, replaceValueString, s);
                }
                // FIXME: spec issue (bug 2625)
                if (nextSrcPosition > position) {
                    break;
                }
                accumulatedResult.append(s, nextSrcPosition, position).append(replacement);
                nextSrcPosition = result.end();
            }
            if (!functionalReplace) {
                assert lastMatchResult != null;
                RegExpConstructor.storeLastMatchResult(cx, rx, s, lastMatchResult);
            }
            /* step 17 */
            return accumulatedResult.append(s, nextSrcPosition, s.length()).toString();
        }

        private static Object[] GetReplacerArguments(RegExpObject rx, MatchResult matchResult,
                String string) {
            int m = matchResult.groupCount();
            Object[] arguments = new Object[m + 3];
            arguments[0] = matchResult.group();
            GroupIterator iterator = new GroupIterator(rx, matchResult);
            for (int i = 1; iterator.hasNext(); ++i) {
                String group = iterator.next();
                arguments[i] = (group != null ? group : UNDEFINED);
            }
            arguments[m + 1] = matchResult.start();
            arguments[m + 2] = string;

            return arguments;
        }

        /**
         * Runtime Semantics: GetReplaceSubstitution Abstract Operation
         * 
         * @param rx
         *            the regular expression object
         * @param matchResult
         *            the match result
         * @param replaceValue
         *            the replace string
         * @param string
         *            the string
         * @return the replacement string
         */
        private static String GetReplaceSubstitution(RegExpObject rx, MatchResult matchResult,
                String replaceValue, String string) {
            int m = matchResult.groupCount();
            String[] groups = null;
            StringBuilder replacement = new StringBuilder();

            for (int cursor = 0, len = replaceValue.length(); cursor < len;) {
                char c = replaceValue.charAt(cursor++);
                if (c == '$' && cursor < len) {
                    c = replaceValue.charAt(cursor++);
                    switch (c) {
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9': {
                        int n = c - '0';
                        if (cursor < len) {
                            char d = replaceValue.charAt(cursor);
                            if (d >= (n == 0 ? '1' : '0') && d <= '9') {
                                int nn = n * 10 + (d - '0');
                                if (nn <= m) {
                                    cursor += 1;
                                    n = nn;
                                }
                            }
                        }
                        if (n == 0 || n > m) {
                            assert n >= 0 && n <= 9;
                            replacement.append('$').append(c);
                        } else {
                            assert n >= 1 && n <= 99;
                            if (groups == null) {
                                groups = RegExpPrototype.groups(rx, matchResult);
                            }
                            String group = groups[n];
                            if (group != null) {
                                replacement.append(group);
                            }
                        }
                        break;
                    }
                    case '&':
                        replacement.append(matchResult.group());
                        break;
                    case '`':
                        replacement.append(string, 0, matchResult.start());
                        break;
                    case '\'':
                        replacement.append(string, matchResult.end(), string.length());
                        break;
                    case '$':
                        replacement.append('$');
                        break;
                    default:
                        replacement.append('$').append(c);
                        break;
                    }
                } else {
                    replacement.append(c);
                }
            }

            return replacement.toString();
        }

        /**
         * 21.2.5.8 RegExp.prototype.search (S)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param s
         *            the string
         * @return the string index of the first match
         */
        @Function(name = "search", arity = 1)
        public static Object search(ExecutionContext cx, Object thisValue, Object s) {
            /* steps 1-4 */
            RegExpObject rx = thisRegExpValue(cx, thisValue);
            /* steps 5-6 */
            String string = ToFlatString(cx, s);
            /* steps 7-8 */
            MatchResult result = getMatcherOrNull(cx, rx, string, true);
            /* step 9 */
            if (result == null) {
                return -1;
            }
            RegExpConstructor.storeLastMatchResult(cx, rx, string, result);
            /* step 10 */
            return result.start();
        }

        /**
         * 21.2.5.10 RegExp.prototype.split (string, limit)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param string
         *            the string
         * @param limit
         *            the optional split array limit
         * @return the split array object
         */
        @Function(name = "split", arity = 2)
        public static Object split(ExecutionContext cx, Object thisValue, Object string,
                Object limit) {
            /* steps 1-4 */
            RegExpObject rx = thisRegExpValue(cx, thisValue);
            /* step 5 (moved after step 14) */
            /* steps 6-7 */
            String s = ToFlatString(cx, string);
            /* steps 8-9 */
            ScriptObject a = ArrayCreate(cx, 0);
            /* step 10 */
            int lengthA = 0;
            /* step 11 */
            long lim = Type.isUndefined(limit) ? 0x1F_FFFF_FFFF_FFFFL : ToLength(cx, limit);
            /* step 12 */
            int size = s.length();
            /* step 13 */
            int p = 0;
            /* step 14 */
            if (lim == 0) {
                return a;
            }
            /* step 5 */
            MatchState matcher = rx.getRegExpMatcher().matcher(s);
            /* step 15 */
            if (size == 0) {
                if (matcher.find()) {
                    RegExpConstructor.storeLastMatchResult(cx, rx, s, matcher.toMatchResult());
                    return a;
                }
                CreateDataProperty(cx, a, "0", s);
                return a;
            }
            /* step 16 */
            int q = p;
            int lastStart = -1;
            while (q != size) {
                boolean match = matcher.find();
                if (!match) {
                    break;
                }
                RegExpConstructor.storeLastMatchResult(cx, rx, s, matcher.toMatchResult());
                int e = matcher.end();
                if (e == p) {
                    q = q + 1;
                } else {
                    String t = s.substring(p, lastStart = matcher.start());
                    CreateDataProperty(cx, a, ToString(lengthA), t);
                    lengthA += 1;
                    if (lengthA == lim) {
                        return a;
                    }
                    p = e;
                    GroupIterator iterator = new GroupIterator(rx, matcher);
                    while (iterator.hasNext()) {
                        String cap = iterator.next();
                        CreateDataProperty(cx, a, ToString(lengthA), cap != null ? cap : UNDEFINED);
                        lengthA += 1;
                        if (lengthA == lim) {
                            return a;
                        }
                    }
                    q = p;
                }
            }
            if (lastStart == size) {
                return a;
            }
            /* step 18 */
            String t = s.substring(p, size);
            /* steps 19-20 */
            CreateDataProperty(cx, a, ToString(lengthA), t);
            /* step 21 */
            return a;
        }

        /**
         * 21.2.5.15 RegExp.prototype.@@isRegExp
         */
        @Value(name = "[Symbol.isRegExp]", symbol = BuiltinSymbol.isRegExp,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final boolean isRegExp = true;

    }

    /**
     * B.2.5 Additional Properties of the RegExp.prototype Object
     */
    @CompatibilityExtension(CompatibilityOption.RegExpPrototype)
    public enum AdditionalProperties {
        ;

        /**
         * B.2.5.1 RegExp.prototype.compile (pattern, flags )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param pattern
         *            the regular expression pattern
         * @param flags
         *            the regular expression flags
         * @return the regular expression object
         */
        @Function(name = "compile", arity = 2)
        public static Object compile(ExecutionContext cx, Object thisValue, Object pattern,
                Object flags) {
            /* step 1 (omitted) */
            /* step 2 */
            if (!(thisValue instanceof RegExpObject)) {
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            RegExpObject r = (RegExpObject) thisValue;
            // FIXME: spec issue - delay extensible check after side effects?
            /*
             * re = /abc/;
             * re.compile({
             *   toString() {
             *     re.compile("def");
             *     Object.preventExtensions(re);
             *     return "ghi";
             *   }
             * });
             */
            /* step 3 */
            boolean extensible = IsExtensible(cx, r);
            /* step 4 */
            if (!extensible) {
                throw newTypeError(cx, Messages.Key.NotExtensible);
            }
            Object p, f;
            if (pattern instanceof RegExpObject) {
                /* step 5 */
                RegExpObject rx = (RegExpObject) pattern;
                if (!rx.isInitialised()) {
                    throw newTypeError(cx, Messages.Key.UninitialisedObject);
                }
                if (!Type.isUndefined(flags)) {
                    throw newTypeError(cx, Messages.Key.NotUndefined);
                }
                p = rx.getOriginalSource();
                f = rx.getOriginalFlags();
            } else {
                /* step 6 */
                p = pattern;
                f = flags;
            }
            /* step 7 */
            return RegExpInitialize(cx, r, p, f);
        }
    }

    /**
     * Runtime Semantics: RegExpExec Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param r
     *            the regular expression object
     * @param s
     *            the string
     * @return the match result object or null
     */
    public static Object RegExpExec(ExecutionContext cx, RegExpObject r, String s) {
        /* step 5, default 'ignore' to false */
        return RegExpExec(cx, r, s, false);
    }

    /**
     * Runtime Semantics: RegExpExec Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param r
     *            the regular expression object
     * @param s
     *            the string
     * @param ignore
     *            the ignore flag
     * @return the match result object or null
     */
    public static Object RegExpExec(ExecutionContext cx, RegExpObject r, String s, boolean ignore) {
        /* steps 1-19 */
        MatchResult m = getMatcherOrNull(cx, r, s, ignore);
        if (m == null) {
            return NULL;
        }
        RegExpConstructor.storeLastMatchResult(cx, r, s, m);
        /* steps 20-30 */
        return toMatchResult(cx, r, s, m);
    }

    /**
     * Runtime Semantics: RegExpExec Abstract Operation (1)
     * 
     * @param cx
     *            the execution context
     * @param r
     *            the regular expression object
     * @param s
     *            the string
     * @return the match result or {@code null}
     */
    private static MatchResult getMatcherOrNull(ExecutionContext cx, RegExpObject r, String s) {
        /* step 5, default 'ignore' to false */
        return getMatcherOrNull(cx, r, s, false);
    }

    /**
     * Runtime Semantics: RegExpExec Abstract Operation (1)
     * 
     * @param cx
     *            the execution context
     * @param r
     *            the regular expression object
     * @param s
     *            the string
     * @param ignore
     *            the ignore flag
     * @return the match result or {@code null}
     */
    private static MatchResult getMatcherOrNull(ExecutionContext cx, RegExpObject r, String s,
            boolean ignore) {
        /* step 1 */
        assert r.isInitialised();
        /* step 2 (not applicable) */
        /* steps 3-4 (not applicable) */
        /* step 6 */
        int length = s.length();
        /* steps 7-8 */
        double i;
        boolean global, sticky;
        if (ignore) {
            /* step 7 */
            global = false;
            sticky = false;
            i = 0;
        } else {
            /* step 8a */
            Object lastIndex = Get(cx, r, "lastIndex");
            /* steps 8b-8c */
            i = ToInteger(cx, lastIndex);
            /* steps 8d-8e */
            global = ToBoolean(Get(cx, r, "global"));
            /* steps 8f-8g (steps 9-10) */
            sticky = ToBoolean(Get(cx, r, "sticky"));
            /* step 8h (step 11) */
            if (!global && !sticky) {
                i = 0;
            }
        }
        /* step 16.a */
        if (i < 0 || i > length) {
            if (!ignore) {
                Put(cx, r, "lastIndex", 0, true);
            }
            return null;
        }
        /* step 12 */
        RegExpMatcher matcher = r.getRegExpMatcher();
        /* steps 13-14 (not applicable) */
        /* steps 15-16 */
        MatchState m = matcher.matcher(s);
        boolean matchSucceeded;
        if (!sticky) {
            matchSucceeded = m.find((int) i);
        } else {
            matchSucceeded = m.matches((int) i);
        }
        /* step 16.a, 16.c */
        if (!matchSucceeded) {
            if (!ignore) {
                Put(cx, r, "lastIndex", 0, true);
            }
            return null;
        }
        /* steps 17-18 */
        int e = m.end();
        /* step 19 */
        if (global || sticky) {
            assert !ignore;
            Put(cx, r, "lastIndex", e, true);
        }
        return m.toMatchResult();
    }

    /**
     * Runtime Semantics: RegExpExec Abstract Operation (2)
     * 
     * @param cx
     *            the execution context
     * @param r
     *            the regular expression object
     * @param s
     *            the string
     * @param m
     *            the match result
     * @return the match result script object
     */
    private static ScriptObject toMatchResult(ExecutionContext cx, RegExpObject r, String s,
            MatchResult m) {
        assert r.isInitialised();
        /* steps 17-18 */
        int e = m.end();
        /* step 20 */
        int n = m.groupCount();
        /* step 21 */
        ScriptObject array = ArrayCreate(cx, n + 1);
        /* step 22 */
        int matchIndex = m.start();
        /* steps 23-26 */
        CreateDataProperty(cx, array, "index", matchIndex);
        CreateDataProperty(cx, array, "input", s);
        /* step 27 */
        String matchedSubstr = s.substring(matchIndex, e);
        /* step 28 */
        CreateDataProperty(cx, array, "0", matchedSubstr);
        /* step 29 */
        GroupIterator iterator = new GroupIterator(r, m);
        for (int i = 1; iterator.hasNext(); ++i) {
            String capture = iterator.next();
            CreateDataProperty(cx, array, ToString(i), (capture != null ? capture : UNDEFINED));
        }
        /* step 30 */
        return array;
    }

    /**
     * Returns the filtered capturing groups of the {@link MatchResult} argument
     * 
     * @param r
     *            the regular expression object
     * @param m
     *            the match result
     * @return the match groups
     */
    public static String[] groups(RegExpObject r, MatchResult m) {
        assert r.isInitialised();
        GroupIterator iterator = new GroupIterator(r, m);
        int c = m.groupCount();
        String[] groups = new String[c + 1];
        groups[0] = m.group();
        for (int i = 1; iterator.hasNext(); ++i) {
            groups[i] = iterator.next();
        }
        return groups;
    }

    private static final class GroupIterator implements Iterator<String> {
        private final MatchResult result;
        private final BitSet negativeLAGroups;
        private int group = 1;
        // start index of last valid group in matched string
        private int last;

        GroupIterator(RegExpObject r, MatchResult result) {
            this.result = result;
            this.negativeLAGroups = r.getRegExpMatcher().getNegativeLookaheadGroups();
            this.last = result.start();
        }

        @Override
        public boolean hasNext() {
            return group <= result.groupCount();
        }

        @Override
        public String next() {
            int group = this.group++;
            if (result.start(group) >= last && !negativeLAGroups.get(group)) {
                last = result.start(group);
                return result.group(group);
            } else {
                return null;
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
