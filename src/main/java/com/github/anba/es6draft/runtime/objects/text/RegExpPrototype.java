/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.text;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.text.RegExpConstructor.EscapeRegExpPattern;
import static com.github.anba.es6draft.runtime.objects.text.RegExpConstructor.RegExpCreate;
import static com.github.anba.es6draft.runtime.objects.text.RegExpConstructor.RegExpInitialize;
import static com.github.anba.es6draft.runtime.objects.text.RegExpStringIteratorPrototype.MatchAllIterator;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ArrayObject.ArrayCreate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.MatchResult;

import com.github.anba.es6draft.regexp.MatcherResult;
import com.github.anba.es6draft.regexp.MatcherState;
import com.github.anba.es6draft.regexp.RegExpMatcher;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.CompatibilityExtension;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.StrBuilder;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;
import com.github.anba.es6draft.runtime.types.builtins.NativeFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>21 Text Processing</h1><br>
 * <h2>21.2 RegExp (Regular Expression) Objects</h2>
 * <ul>
 * <li>21.2.5 Properties of the RegExp Prototype Object
 * <li>21.2.6 Properties of RegExp Instances
 * </ul>
 */
public final class RegExpPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new RegExp prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public RegExpPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
        createProperties(realm, this, AdditionalProperties.class);
        createProperties(realm, this, DotAllProperty.class);
        createProperties(realm, this, MatchAllProperty.class);
    }

    private static RegExpObject thisRegExpObject(ExecutionContext cx, Object value, String method) {
        if (value instanceof RegExpObject) {
            return (RegExpObject) value;
        }
        throw newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(value).toString());
    }

    private static boolean thisRegExpObjectOrPrototype(ExecutionContext cx, Object value, String method) {
        if (value instanceof RegExpObject) {
            return true;
        }
        if (value == cx.getIntrinsic(Intrinsics.RegExpPrototype)) {
            return false;
        }
        throw newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(value).toString());
    }

    /**
     * 21.2.5 Properties of the RegExp Prototype Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 21.2.5.1 RegExp.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.RegExp;

        /**
         * 21.2.5.2 RegExp.prototype.exec ( string )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param string
         *            the string
         * @return the match result array
         */
        @Function(name = "exec", arity = 1, nativeId = RegExpPrototypeExec.class)
        public static Object exec(ExecutionContext cx, Object thisValue, Object string) {
            /* steps 1-3 */
            RegExpObject r = thisRegExpObject(cx, thisValue, "RegExp.prototype.exec");
            /* step 4 */
            String s = ToFlatString(cx, string);
            /* step 5 */
            ArrayObject result = RegExpBuiltinExec(cx, r, s);
            return result != null ? result : NULL;
        }

        /**
         * 21.2.5.3 get RegExp.prototype.flags
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the regular expressions flags
         */
        @Accessor(name = "flags", type = Accessor.Type.Getter, nativeId = RegExpPrototypeFlags.class)
        public static Object flags(ExecutionContext cx, Object thisValue) {
            /* step 2 */
            if (!Type.isObject(thisValue)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 1 */
            ScriptObject r = Type.objectValue(thisValue);
            /* step 3 */
            int c = 0;
            char[] result = new char[6];
            /* steps 4-5 */
            if (RegExpGetFlag(cx, r, "global", RegExpObject.Flags.Global, RegExpPrototypeGlobal.class)) {
                result[c++] = 'g';
            }
            /* steps 6-7 */
            if (RegExpGetFlag(cx, r, "ignoreCase", RegExpObject.Flags.IgnoreCase, RegExpPrototypeIgnoreCase.class)) {
                result[c++] = 'i';
            }
            /* steps 8-9 */
            if (RegExpGetFlag(cx, r, "multiline", RegExpObject.Flags.Multiline, RegExpPrototypeMultiline.class)) {
                result[c++] = 'm';
            }
            if (cx.getRuntimeContext().isEnabled(CompatibilityOption.RegExpDotAll)) {
                if (RegExpGetFlag(cx, r, "dotAll", RegExpObject.Flags.DotAll, RegExpPrototypeDotAll.class)) {
                    result[c++] = 's';
                }
            }
            /* steps 10-11 */
            if (RegExpGetFlag(cx, r, "unicode", RegExpObject.Flags.Unicode, RegExpPrototypeUnicode.class)) {
                result[c++] = 'u';
            }
            /* steps 12-13 */
            if (RegExpGetFlag(cx, r, "sticky", RegExpObject.Flags.Sticky, RegExpPrototypeSticky.class)) {
                result[c++] = 'y';
            }
            /* step 14 */
            return String.valueOf(result, 0, c);
        }

        /**
         * 21.2.5.4 get RegExp.prototype.global
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the global flag
         */
        @Accessor(name = "global", type = Accessor.Type.Getter, nativeId = RegExpPrototypeGlobal.class)
        public static Object global(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            if (!thisRegExpObjectOrPrototype(cx, thisValue, "RegExp.prototype.global")) {
                return UNDEFINED;
            }
            RegExpObject r = (RegExpObject) thisValue;
            /* steps 4-6 */
            return r.isSet(RegExpObject.Flags.Global);
        }

        /**
         * 21.2.5.5 get RegExp.prototype.ignoreCase
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the ignoreCase flag
         */
        @Accessor(name = "ignoreCase", type = Accessor.Type.Getter, nativeId = RegExpPrototypeIgnoreCase.class)
        public static Object ignoreCase(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            if (!thisRegExpObjectOrPrototype(cx, thisValue, "RegExp.prototype.ignoreCase")) {
                return UNDEFINED;
            }
            RegExpObject r = (RegExpObject) thisValue;
            /* steps 4-6 */
            return r.isSet(RegExpObject.Flags.IgnoreCase);
        }

        /**
         * 21.2.5.7 get RegExp.prototype.multiline
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the multiline flag
         */
        @Accessor(name = "multiline", type = Accessor.Type.Getter, nativeId = RegExpPrototypeMultiline.class)
        public static Object multiline(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            if (!thisRegExpObjectOrPrototype(cx, thisValue, "RegExp.prototype.multiline")) {
                return UNDEFINED;
            }
            RegExpObject r = (RegExpObject) thisValue;
            /* steps 4-6 */
            return r.isSet(RegExpObject.Flags.Multiline);
        }

        /**
         * 21.2.5.10 get RegExp.prototype.source
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the source property
         */
        @Accessor(name = "source", type = Accessor.Type.Getter)
        public static Object source(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            if (!thisRegExpObjectOrPrototype(cx, thisValue, "RegExp.prototype.source")) {
                return "(?:)";
            }
            RegExpObject r = (RegExpObject) thisValue;
            /* steps 4-7 */
            return EscapeRegExpPattern(cx, r.getOriginalSource(), r.getOriginalFlags());
        }

        /**
         * 21.2.5.12 get RegExp.prototype.sticky
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the sticky flag
         */
        @Accessor(name = "sticky", type = Accessor.Type.Getter, nativeId = RegExpPrototypeSticky.class)
        public static Object sticky(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            if (!thisRegExpObjectOrPrototype(cx, thisValue, "RegExp.prototype.sticky")) {
                return UNDEFINED;
            }
            RegExpObject r = (RegExpObject) thisValue;
            /* steps 4-6 */
            return r.isSet(RegExpObject.Flags.Sticky);
        }

        /**
         * 21.2.5.13 RegExp.prototype.test ( S )
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
            /* step 1 */
            if (!Type.isObject(thisValue)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 2 */
            ScriptObject r = Type.objectValue(thisValue);
            /* step 3 */
            String s = ToFlatString(cx, string);
            /* steps 4-5 (fast path) */
            if (isDefaultRegExpObjectForExec(cx, r)) {
                return RegExpTest(cx, (RegExpObject) r, s);
            }
            /* step 4 */
            MatchResult match = matchResultOrNull(cx, r, s, true);
            /* step 5 */
            return match != null;
        }

        /**
         * 21.2.5.14 RegExp.prototype.toString()
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the string representation
         */
        @Function(name = "toString", arity = 0)
        public static Object toString(ExecutionContext cx, Object thisValue) {
            /* step 2 */
            if (!Type.isObject(thisValue)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 1 */
            ScriptObject r = Type.objectValue(thisValue);
            /* step 3 */
            CharSequence pattern = ToString(cx, Get(cx, r, "source"));
            /* step 4 */
            CharSequence flags = ToString(cx, Get(cx, r, "flags"));
            /* steps 5-6 */
            return new StrBuilder(cx).append('/').append(pattern).append('/').append(flags).toString();
        }

        /**
         * 21.2.5.15 get RegExp.prototype.unicode
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the unicode flag
         */
        @Accessor(name = "unicode", type = Accessor.Type.Getter, nativeId = RegExpPrototypeUnicode.class)
        public static Object unicode(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            if (!thisRegExpObjectOrPrototype(cx, thisValue, "RegExp.prototype.unicode")) {
                return UNDEFINED;
            }
            RegExpObject r = (RegExpObject) thisValue;
            /* steps 4-6 */
            return r.isSet(RegExpObject.Flags.Unicode);
        }

        /**
         * 21.2.5.6 RegExp.prototype[ @@match ] ( string )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param string
         *            the string
         * @return the match result array
         */
        @Function(name = "[Symbol.match]", symbol = BuiltinSymbol.match, arity = 1)
        public static Object match(ExecutionContext cx, Object thisValue, Object string) {
            /* step 2 */
            if (!Type.isObject(thisValue)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 1 */
            ScriptObject rx = Type.objectValue(thisValue);
            /* step 3 */
            String s = ToFlatString(cx, string);
            /* step 4 */
            boolean global = RegExpIsGlobal(cx, rx);
            /* step 6.a */
            boolean fullUnicode = global && RegExpIsUnicode(cx, rx);
            /* steps 5-6 (fast path) */
            if (isDefaultRegExpObjectForExec(cx, rx)) {
                return RegExpMatch(cx, (RegExpObject) rx, s, global, fullUnicode);
            }
            /* steps 5-6 */
            if (!global) {
                /* step 5.a */
                ScriptObject result = RegExpExec(cx, rx, s);
                return result != null ? result : NULL;
            } else {
                /* step 6.a (moved) */
                /* step 6.b */
                Set(cx, rx, "lastIndex", 0, true);
                /* step 6.c */
                ArrayObject array = ArrayCreate(cx, 0);
                /* steps 6.d-e */
                for (int n = 0;; ++n) {
                    /* step 6.e.i */
                    MatchResult result = matchResultOrNull(cx, rx, s, true);
                    /* step 6.e.ii */
                    if (result == null) {
                        return n == 0 ? NULL : array;
                    }
                    /* step 6.e.iii */
                    String matchStr = result.group();
                    boolean status = CreateDataProperty(cx, array, n, matchStr);
                    assert status;
                    if (matchStr.isEmpty()) {
                        long thisIndex = ToLength(cx, Get(cx, rx, "lastIndex"));
                        long nextIndex = AdvanceStringIndex(s, thisIndex, fullUnicode);
                        Set(cx, rx, "lastIndex", nextIndex, true);
                    }
                }
            }
        }

        /**
         * 21.2.5.8 RegExp.prototype[ @@replace ] ( string, replaceValue )
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
        @Function(name = "[Symbol.replace]", symbol = BuiltinSymbol.replace, arity = 2)
        public static Object replace(ExecutionContext cx, Object thisValue, Object string, Object replaceValue) {
            /* step 2 */
            if (!Type.isObject(thisValue)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 1 */
            ScriptObject rx = Type.objectValue(thisValue);
            /* step 3 */
            String s = ToFlatString(cx, string);
            /* step 4 */
            int lengthS = s.length();
            /* step 5 */
            boolean functionalReplace = IsCallable(replaceValue);
            /* step 6 */
            String replaceValueString = null;
            Callable replaceValueCallable = null;
            if (!functionalReplace) {
                replaceValueString = ToFlatString(cx, replaceValue);
            } else {
                replaceValueCallable = (Callable) replaceValue;
            }
            /* step 7 */
            boolean global = RegExpIsGlobal(cx, rx);
            /* step 8.a */
            boolean fullUnicode = global && RegExpIsUnicode(cx, rx);
            /* steps 8-11 (fast path) */
            List<MatchResult> results;
            if (isDefaultRegExpObjectForExec(cx, rx)) {
                results = RegExpReplace(cx, (RegExpObject) rx, s, global, fullUnicode);
            } else {
                /* step 8 */
                if (global) {
                    /* step 8.a (moved) */
                    /* step 8.b */
                    Set(cx, rx, "lastIndex", 0, true);
                }
                /* step 9 */
                results = new ArrayList<>();
                /* steps 10-11 */
                while (true) {
                    /* step 11.a */
                    MatchResult result = matchResultOrNull(cx, rx, s, false);
                    /* step 11.b */
                    if (result == null) {
                        break;
                    }
                    /* step 11.c */
                    results.add(result);
                    if (!global) {
                        break;
                    }
                    String matchStr = result.group();
                    if (matchStr.isEmpty()) {
                        long thisIndex = ToLength(cx, Get(cx, rx, "lastIndex"));
                        long nextIndex = AdvanceStringIndex(s, thisIndex, fullUnicode);
                        Set(cx, rx, "lastIndex", nextIndex, true);
                    }
                }
            }
            // fast-path if no matches were found
            if (results.isEmpty()) {
                return s;
            }
            /* step 12 */
            StrBuilder accumulatedResult = new StrBuilder(cx);
            /* step 13 */
            int nextSourcePosition = 0;
            /* step 14 */
            MatcherResult lastResult = null;
            boolean storeResult = true, invalidateResult = false;
            if (cx.getRuntimeContext().isEnabled(CompatibilityOption.LegacyRegExp) && rx instanceof RegExpObject) {
                // FIXME: spec issue - `"abc".replace(/./g, () => RegExp.lastMatch)` -> "abc" or "ccc"?
                RegExpObject r = (RegExpObject) rx;
                if (cx.getRealm() == r.getRealm()) {
                    if (!r.isLegacyFeaturesEnabled()) {
                        invalidateResult = true;
                        storeResult = false;
                    }
                } else {
                    storeResult = false;
                }
            }
            boolean namedCapture = cx.getRuntimeContext().isEnabled(CompatibilityOption.RegExpNamedCapture);
            for (MatchResult result : results) {
                if (result instanceof MatcherResult) {
                    MatcherResult matchResult = (MatcherResult) result;
                    if (storeResult) {
                        if (functionalReplace) {
                            RegExpConstructor.storeLastMatchResult(cx, s, matchResult);
                        } else {
                            lastResult = matchResult;
                        }
                    } else if (invalidateResult) {
                        RegExpConstructor.invalidateLastMatchResult(cx);
                        invalidateResult = false;
                    }
                }
                /* steps 14.a-b */
                int nCaptures = result.groupCount();
                /* step 14.c */
                String matched = result.group();
                /* step 14.d */
                int matchLength = matched.length();
                /* steps 14.e-f */
                int position = Math.max(Math.min(result.start(), lengthS), 0);
                /* steps 14.g-k */
                String replacement;
                if (functionalReplace) {
                    Object[] replacerArgs = GetReplacerArguments(cx, matched, s, position, result, nCaptures);
                    Object replValue = replaceValueCallable.call(cx, UNDEFINED, replacerArgs);
                    replacement = ToFlatString(cx, replValue);
                } else {
                    String[] captures = groups(result, nCaptures);
                    NamedGroups namedGroups = namedCapture ? namedGroups(cx, s, result) : null;
                    replacement = GetSubstitution(cx, matched, s, position, captures, namedGroups, replaceValueString);
                }
                /* step 14.l */
                if (position >= nextSourcePosition) {
                    accumulatedResult.append(s, nextSourcePosition, position).append(replacement);
                    nextSourcePosition = position + matchLength;
                }
            }
            if (lastResult != null) {
                RegExpConstructor.storeLastMatchResult(cx, s, lastResult);
            }
            /* step 15 */
            if (nextSourcePosition >= lengthS) {
                return accumulatedResult.toString();
            }
            /* step 16 */
            return accumulatedResult.append(s, nextSourcePosition, lengthS).toString();
        }

        /**
         * 21.2.5.9 RegExp.prototype[ @@search ] ( string )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param string
         *            the string
         * @return the string index of the first match
         */
        @Function(name = "[Symbol.search]", symbol = BuiltinSymbol.search, arity = 1)
        public static Object search(ExecutionContext cx, Object thisValue, Object string) {
            /* step 2 */
            if (!Type.isObject(thisValue)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 1 */
            ScriptObject rx = Type.objectValue(thisValue);
            /* step 3 */
            String s = ToFlatString(cx, string);
            /* steps 4-9 (fast path) */
            if (isDefaultRegExpObjectForExec(cx, rx)) {
                return RegExpSearch(cx, (RegExpObject) rx, s);
            }
            /* step 4 */
            Object previousLastIndex = Get(cx, rx, "lastIndex");
            /* step 5 */
            if (!SameValue(previousLastIndex, 0)) {
                Set(cx, rx, "lastIndex", 0, true);
            }
            /* step 6 */
            MatchResult result = matchResultOrNull(cx, rx, s, true);
            /* step 7 */
            Object currentLastIndex = Get(cx, rx, "lastIndex");
            if (!SameValue(currentLastIndex, previousLastIndex)) {
                Set(cx, rx, "lastIndex", previousLastIndex, true);
            }
            /* step 8 */
            if (result == null) {
                return -1;
            }
            /* step 9 */
            if (result instanceof ScriptObjectMatchResult) {
                // Extract wrapped script object to ensure no ToInteger conversion takes place
                ScriptObject object = ((ScriptObjectMatchResult) result).object;
                return Get(cx, object, "index");
            }
            return result.start();
        }

        /**
         * 21.2.5.11 RegExp.prototype[ @@split ] ( string, limit )
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
        @Function(name = "[Symbol.split]", symbol = BuiltinSymbol.split, arity = 2)
        public static Object split(ExecutionContext cx, Object thisValue, Object string, Object limit) {
            /* step 2 */
            if (!Type.isObject(thisValue)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 1 */
            ScriptObject rx = Type.objectValue(thisValue);
            /* step 3 */
            String s = ToFlatString(cx, string);
            /* step 4 */
            Constructor c = SpeciesConstructor(cx, rx, Intrinsics.RegExp);
            /* step 5 */
            String flags = RegExpFlags(cx, rx);
            /* steps 6-7 */
            boolean unicodeMatching = flags.indexOf('u') != -1;
            /* steps 8-10, 13 */
            ScriptObject splitter;
            int lim;
            if (rx instanceof RegExpObject && c instanceof RegExpConstructor
                    && c == cx.getIntrinsic(Intrinsics.RegExp)) {
                RegExpObject re = (RegExpObject) rx;
                // 21.2.3.1 RegExp ( pattern, flags ) - step 1
                // NB: Only executed for its side-effects.
                IsRegExp(cx, re);
                // Extract pattern before calling ToLength.
                String pattern = re.getOriginalSource();
                /* step 13 */
                lim = (int) Math.min(Type.isUndefined(limit) ? 0xFFFF_FFFFL : ToUint32(cx, limit), Integer.MAX_VALUE);
                // Test if current RegExp.prototype.exec is the built-in RegExp.prototype.exec method. Also test if
                // pattern/flags match the original pattern/flags. Side-effects in IsRegExp or ToUint32 may have called
                // RegExp.prototype.compile.
                if (isBuiltinRegExpPrototypeForExec(cx) && pattern.equals(re.getOriginalSource())
                        && flags.equals(re.getOriginalFlags())) {
                    return RegExpSplit(cx, re, s, unicodeMatching, lim, true);
                }
                // Optimization not applicable, fall back to slower spec algorithm. RegExpCreate must be side-effect
                // free, otherwise these steps are detectable from script code.
                /* steps 8-9 */
                String newFlags = flags.indexOf('y') != -1 ? flags : flags + 'y';
                /* step 10 */
                splitter = RegExpCreate(cx, pattern, newFlags);
            } else {
                /* steps 8-9 */
                String newFlags = flags.indexOf('y') != -1 ? flags : flags + 'y';
                /* step 10 */
                splitter = c.construct(cx, rx, newFlags);
                /* step 13 */
                lim = (int) Math.min(Type.isUndefined(limit) ? 0xFFFF_FFFFL : ToUint32(cx, limit), Integer.MAX_VALUE);
            }
            /* steps 11-12, 14-22 (fast path) */
            if (isDefaultRegExpObjectForExec(cx, splitter)) {
                RegExpObject re = (RegExpObject) splitter;
                if (re.isSet(RegExpObject.Flags.Sticky)) {
                    return RegExpSplit(cx, re, s, unicodeMatching, lim, false);
                }
            }
            /* step 11 */
            ArrayObject a = ArrayCreate(cx, 0);
            /* step 12 */
            int lengthA = 0;
            /* step 13 (above) */
            /* step 14 */
            int size = s.length();
            /* step 15 */
            int p = 0;
            /* step 16 */
            if (lim == 0) {
                return a;
            }
            /* step 17 */
            if (size == 0) {
                // ScriptObject z = RegExpExec(cx, splitter, s);
                MatchResult z = matchResultOrNull(cx, splitter, s, true);
                if (z != null) {
                    return a;
                }
                CreateDataProperty(cx, a, 0, s);
                return a;
            }
            /* step 18 */
            int q = p;
            /* step 19 */
            while (q < size) {
                /* step 19.a */
                Set(cx, splitter, "lastIndex", q, true);
                /* step 19.b */
                // ScriptObject z = RegExpExec(cx, splitter, s);
                MatchResult z = matchResultOrNull(cx, splitter, s, true);
                /* step 19.c */
                if (z == null) {
                    q = AdvanceStringIndex(s, q, unicodeMatching);
                    continue;
                }
                /* step 19.d */
                /* steps 19.d.i-ii */
                int e = Math.min((int) ToLength(cx, Get(cx, splitter, "lastIndex")), size);
                /* step 19.d.iii */
                if (e == p) {
                    q = AdvanceStringIndex(s, q, unicodeMatching);
                    continue;
                }
                /* step 19.d.iv */
                /* step 19.d.iv.1 */
                String t = s.substring(p, q);
                /* step 19.d.iv.2 */
                CreateDataProperty(cx, a, lengthA, t);
                /* step 19.d.iv.3 */
                lengthA += 1;
                /* step 19.d.iv.4 */
                if (lengthA == lim) {
                    return a;
                }
                /* steps 19.d.iv.6-9 */
                int groupCount = z.groupCount();
                for (int i = 1; i <= groupCount; ++i) {
                    Object nextCap = z.group(i);
                    CreateDataProperty(cx, a, lengthA, nextCap != null ? nextCap : UNDEFINED);
                    lengthA += 1;
                    if (lengthA == lim) {
                        return a;
                    }
                }
                /* step 19.d.iv.5 */
                p = e;
                /* step 19.d.iv.10 */
                q = p;
            }
            /* step 20 */
            String t = s.substring(p, size);
            /* step 21 */
            CreateDataProperty(cx, a, lengthA, t);
            /* step 22 */
            return a;
        }
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
        public static Object compile(ExecutionContext cx, Object thisValue, Object pattern, Object flags) {
            /* step 1 (omitted) */
            /* step 2 */
            if (!(thisValue instanceof RegExpObject)) {
                throw newTypeError(cx, Messages.Key.IncompatibleThis, "RegExp.prototype.compile",
                        Type.of(thisValue).toString());
            }
            RegExpObject r = (RegExpObject) thisValue;

            // Extension: Legacy RegExp features
            if (cx.getRuntimeContext().isEnabled(CompatibilityOption.LegacyRegExp)) {
                if (cx.getRealm() != r.getRealm() || !r.isLegacyFeaturesEnabled()) {
                    throw newTypeError(cx, Messages.Key.IncompatibleThis, "RegExp.prototype.compile",
                            Type.of(thisValue).toString());
                }
            }

            /* steps 3-4 */
            Object p, f;
            if (pattern instanceof RegExpObject) {
                /* step 3 */
                RegExpObject rx = (RegExpObject) pattern;
                if (!Type.isUndefined(flags)) {
                    throw newTypeError(cx, Messages.Key.NotUndefined);
                }
                p = rx.getOriginalSource();
                f = rx.getOriginalFlags();
            } else {
                /* step 4 */
                p = pattern;
                f = flags;
            }
            /* step 5 */
            return RegExpInitialize(cx, r, p, f);
        }
    }

    /**
     * Properties of the RegExp.prototype Object
     */
    @CompatibilityExtension(CompatibilityOption.RegExpDotAll)
    public enum DotAllProperty {
        ;

        @Accessor(name = "dotAll", type = Accessor.Type.Getter, nativeId = RegExpPrototypeDotAll.class)
        public static Object sticky(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            if (!thisRegExpObjectOrPrototype(cx, thisValue, "RegExp.prototype.dotAll")) {
                return UNDEFINED;
            }
            RegExpObject r = (RegExpObject) thisValue;
            /* steps 4-6 */
            return r.isSet(RegExpObject.Flags.DotAll);
        }
    }

    /**
     * Properties of the RegExp.prototype Object
     */
    @CompatibilityExtension(CompatibilityOption.StringMatchAll)
    public enum MatchAllProperty {
        ;

        @Function(name = "[Symbol.matchAll]", symbol = BuiltinSymbol.matchAll, arity = 1)
        public static Object matchAll(ExecutionContext cx, Object thisValue, Object string) {
            /* step 1 (omitted) */
            /* step 2 */
            if (!Type.isObject(thisValue)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 3 */
            return MatchAllIterator(cx, thisValue, string);
        }
    }

    /**
     * Marker class for {@code RegExp.prototype.exec}.
     */
    private static final class RegExpPrototypeExec {
    }

    /**
     * Marker class for {@code RegExp.prototype.dotAll}.
     */
    private static final class RegExpPrototypeDotAll {
    }

    /**
     * Marker class for {@code RegExp.prototype.global}.
     */
    private static final class RegExpPrototypeGlobal {
    }

    /**
     * Marker class for {@code RegExp.prototype.ignoreCase}.
     */
    private static final class RegExpPrototypeIgnoreCase {
    }

    /**
     * Marker class for {@code RegExp.prototype.multiline}.
     */
    private static final class RegExpPrototypeMultiline {
    }

    /**
     * Marker class for {@code RegExp.prototype.sticky}.
     */
    private static final class RegExpPrototypeSticky {
    }

    /**
     * Marker class for {@code RegExp.prototype.unicode}.
     */
    private static final class RegExpPrototypeUnicode {
    }

    /**
     * Marker class for {@code RegExp.prototype.flags}.
     */
    private static final class RegExpPrototypeFlags {
    }

    private static String RegExpFlags(ExecutionContext cx, ScriptObject r) {
        if (r instanceof RegExpObject) {
            RegExpObject rx = (RegExpObject) r;
            if (isBuiltinGetter(cx, rx, "flags", RegExpPrototypeFlags.class)) {
                return (String) Properties.flags(cx, rx);
            }
        }
        return ToFlatString(cx, Get(cx, r, "flags"));
    }

    private static boolean RegExpIsGlobal(ExecutionContext cx, ScriptObject r) {
        return RegExpGetFlag(cx, r, "global", RegExpObject.Flags.Global, RegExpPrototypeGlobal.class);
    }

    private static boolean RegExpIsUnicode(ExecutionContext cx, ScriptObject r) {
        return RegExpGetFlag(cx, r, "unicode", RegExpObject.Flags.Unicode, RegExpPrototypeUnicode.class);
    }

    private static boolean RegExpGetFlag(ExecutionContext cx, ScriptObject r, String name, RegExpObject.Flags flag,
            Class<?> nativeId) {
        if (r instanceof RegExpObject) {
            RegExpObject rx = (RegExpObject) r;
            if (isBuiltinGetter(cx, rx, name, nativeId)) {
                return rx.isSet(flag);
            }
        }
        return ToBoolean(Get(cx, r, name));
    }

    private static boolean isBuiltinRegExpPrototypeForExec(ExecutionContext cx) {
        OrdinaryObject prototype = cx.getIntrinsic(Intrinsics.RegExpPrototype);
        Property exec = prototype.lookupOwnProperty("exec");
        return exec != null && isBuiltinExec(cx.getRealm(), exec.getValue());
    }

    private static boolean isDefaultRegExpObjectForExec(ExecutionContext cx, ScriptObject r) {
        if (!(r instanceof RegExpObject)) {
            return false;
        }
        // Cannot use fast-path if "exec" is not the built-in RegExp.prototype.exec method.
        Property exec = findProperty((RegExpObject) r, "exec");
        return exec != null && isBuiltinExec(cx.getRealm(), exec.getValue());
    }

    private static boolean isBuiltinExec(Realm realm, Object value) {
        return NativeFunction.isNative(realm, value, RegExpPrototypeExec.class);
    }

    private static boolean isBuiltinGetter(ExecutionContext cx, OrdinaryObject r, String name, Class<?> nativeId) {
        Property p = findProperty(r, name);
        return p != null && NativeFunction.isNative(cx.getRealm(), p.getGetter(), nativeId);
    }

    private static Property findProperty(OrdinaryObject object, String name) {
        final int MAX_PROTO_CHAIN_LENGTH = 5;
        for (int i = 0; i < MAX_PROTO_CHAIN_LENGTH; ++i) {
            Property p = object.lookupOwnProperty(name);
            if (p != null) {
                return p;
            }
            ScriptObject proto = object.getPrototype();
            if (!(proto instanceof OrdinaryObject)) {
                break;
            }
            object = (OrdinaryObject) proto;
        }
        return null;
    }

    /**
     * 21.2.5.2.1 Runtime Semantics: RegExpExec ( R, S )
     * 
     * @param cx
     *            the execution context
     * @param r
     *            the regular expression object
     * @param s
     *            the string
     * @return the match result object or null
     */
    public static ScriptObject RegExpExec(ExecutionContext cx, ScriptObject r, String s) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        Object exec = Get(cx, r, "exec");
        /* step 4 */
        // Don't take the slow path for built-in RegExp.prototype.exec
        if (IsCallable(exec) && !isBuiltinExec(cx.getRealm(), exec)) {
            return RegExpUserExec(cx, (Callable) exec, r, s);
        }
        /* step 5 */
        RegExpObject rx = thisRegExpObject(cx, r, "RegExp.prototype.exec");
        /* step 6 */
        return RegExpBuiltinExec(cx, rx, s);
    }

    /**
     * 21.2.5.2.1 Runtime Semantics: RegExpExec ( R, S ) (1)
     * 
     * @param cx
     *            the execution context
     * @param r
     *            the regular expression object
     * @param s
     *            the string
     * @param storeResult
     *            {@code true} if the match result is stored
     * @return the match result object or null
     */
    private static MatchResult matchResultOrNull(ExecutionContext cx, ScriptObject r, String s, boolean storeResult) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        Object exec = Get(cx, r, "exec");
        /* step 4 */
        // Don't take the slow path for built-in RegExp.prototype.exec
        if (IsCallable(exec) && !isBuiltinExec(cx.getRealm(), exec)) {
            ScriptObject o = RegExpUserExec(cx, (Callable) exec, r, s);
            return o != null ? new ScriptObjectMatchResult(cx, o) : null;
        }
        /* step 5 */
        RegExpObject rx = thisRegExpObject(cx, r, "RegExp.prototype.exec");
        /* step 6 */
        return matchResultOrNull(cx, rx, s, storeResult);
    }

    private static ScriptObject RegExpUserExec(ExecutionContext cx, Callable exec, ScriptObject r, String s) {
        /* step 4.a */
        Object result = ((Callable) exec).call(cx, r, s);
        /* step 4.b */
        if (!Type.isObjectOrNull(result)) {
            throw newTypeError(cx, Messages.Key.NotObjectOrNull);
        }
        /* step 4.c */
        return Type.objectValueOrNull(result);
    }

    /**
     * 21.2.5.2.2 Runtime Semantics: RegExpBuiltinExec ( R, S )
     * 
     * @param cx
     *            the execution context
     * @param r
     *            the regular expression object
     * @param s
     *            the string
     * @return the match result object or {@code null}
     */
    private static ArrayObject RegExpBuiltinExec(ExecutionContext cx, RegExpObject r, String s) {
        /* steps 1-15 */
        MatcherResult m = matchResultOrNull(cx, r, s, true);
        if (m == null) {
            return null;
        }
        /* steps 16-25 */
        return toMatchArray(cx, s, m);
    }

    /**
     * 21.2.5.2.2 Runtime Semantics: RegExpBuiltinExec ( R, S ) (1)
     * 
     * @param cx
     *            the execution context
     * @param r
     *            the regular expression object
     * @param s
     *            the string
     * @param storeResult
     *            {@code true} if the match result is stored
     * @return the match result or {@code null}
     */
    private static MatcherResult matchResultOrNull(ExecutionContext cx, RegExpObject r, String s, boolean storeResult) {
        /* step 1 */
        assert r.getRegExpMatcher() != null;
        /* steps 2-3 (not applicable) */
        /* step 4 */
        int lastIndex = RegExpGetLastIndex(cx, r);
        /* step 5 (not applicable) */
        /* step 6 */
        boolean global = r.isSet(RegExpObject.Flags.Global);
        /* step 7 */
        boolean sticky = r.isSet(RegExpObject.Flags.Sticky);
        /* step 8 */
        if (!global && !sticky) {
            lastIndex = 0;
        }
        /* steps 9-15 */
        MatcherResult m = matchResultOrNull(cx, r, s, lastIndex, sticky, storeResult);
        /* steps 12.a, 12.c */
        if (m == null) {
            if (global || sticky) {
                RegExpSetLastIndex(cx, r, 0);
            }
            return null;
        }
        /* steps 13-15 */
        if (global || sticky) {
            RegExpSetLastIndex(cx, r, m.end());
        }
        return m;
    }

    /**
     * 21.2.5.2.2 Runtime Semantics: RegExpBuiltinExec ( R, S ) (1)
     * 
     * @param cx
     *            the execution context
     * @param r
     *            the regular expression object
     * @param s
     *            the string
     * @param lastIndex
     *            the lastIndex position
     * @param sticky
     *            the sticky flag
     * @param storeResult
     *            {@code true} if the match result is stored
     * @return the match result or {@code null}
     */
    private static MatcherResult matchResultOrNull(ExecutionContext cx, RegExpObject r, String s, int lastIndex,
            boolean sticky, boolean storeResult) {
        /* step 1 */
        assert r.getRegExpMatcher() != null;
        /* steps 2-8 (not applicable) */
        /* step 12.a */
        if (lastIndex > s.length()) {
            return null;
        }
        /* step 9 */
        RegExpMatcher matcher = r.getRegExpMatcher();
        /* step 10 (not applicable) */
        /* steps 11-12 */
        MatcherState m = matcher.matcher(s);
        boolean matchSucceeded;
        if (!sticky) {
            matchSucceeded = m.find(lastIndex);
        } else {
            matchSucceeded = m.matches(lastIndex);
        }
        /* steps 12.a, 12.c */
        if (!matchSucceeded) {
            return null;
        }
        if (cx.getRuntimeContext().isEnabled(CompatibilityOption.LegacyRegExp)) {
            if (cx.getRealm() == r.getRealm()) {
                if (!r.isLegacyFeaturesEnabled()) {
                    RegExpConstructor.invalidateLastMatchResult(cx);
                    storeResult = false;
                }
            } else {
                storeResult = false;
            }
        }
        MatcherResult matchResult = m.toMatchResult();
        if (storeResult) {
            RegExpConstructor.storeLastMatchResult(cx, s, matchResult);
        }
        return matchResult;
    }

    /**
     * 21.2.5.2.2 Runtime Semantics: RegExpBuiltinExec ( R, S ) (2)
     * 
     * @param cx
     *            the execution context
     * @param s
     *            the string
     * @param m
     *            the match result
     * @return the match result script object
     */
    private static ArrayObject toMatchArray(ExecutionContext cx, String s, MatcherResult m) {
        /* steps 13-14 */
        int e = m.end();
        /* step 16 */
        int n = m.groupCount();
        /* step 17 */
        ArrayObject array = ArrayCreate(cx, n + 1);
        /* step 18 (omitted) */
        /* step 19 */
        int matchIndex = m.start();
        /* steps 20-21 */
        CreateDataProperty(cx, array, "index", matchIndex);
        CreateDataProperty(cx, array, "input", s);
        /* step 22 */
        String matchedSubstr = s.substring(matchIndex, e);
        /* step 23 */
        CreateDataProperty(cx, array, 0, matchedSubstr);
        /* step 24 */
        for (int i = 1; i <= n; ++i) {
            String capture = m.group(i);
            CreateDataProperty(cx, array, i, (capture != null ? capture : UNDEFINED));
        }
        // Extension: Named capturing groups.
        if (cx.getRuntimeContext().isEnabled(CompatibilityOption.RegExpNamedCapture)) {
            CreateDataProperty(cx, array, "groups", createGroupsObjectOrUndefined(cx, m));
        }
        /* step 25 */
        return array;
    }

    private static Object createGroupsObjectOrUndefined(ExecutionContext cx, MatcherResult m) {
        if (m.groups().isEmpty()) {
            return UNDEFINED;
        }
        OrdinaryObject groups = ObjectCreate(cx, (ScriptObject) null);
        for (String name : m.groups()) {
            String capture = m.group(name);
            CreateDataProperty(cx, groups, name, (capture != null ? capture : UNDEFINED));
        }
        return groups;
    }

    /**
     * 21.2.5.2.3 AdvanceStringIndex ( S, index, unicode )
     * 
     * @param s
     *            the string
     * @param index
     *            the current string index
     * @param unicode
     *            the unicode flag
     * @return the next string index
     */
    private static int AdvanceStringIndex(String s, int index, boolean unicode) {
        /* steps 1-3 (not applicable) */
        /* step 4 */
        if (!unicode) {
            return index + 1;
        }
        /* steps 5-6 */
        if (index + 1 >= s.length()) {
            return index + 1;
        }
        /* steps 7-10 */
        int p = index;
        if (!Character.isHighSurrogate(s.charAt(p)) || !Character.isLowSurrogate(s.charAt(p + 1))) {
            return index + 1;
        }
        /* step 11 */
        return index + 2;
    }

    /**
     * 21.2.5.2.3 AdvanceStringIndex ( S, index, unicode )
     * 
     * @param s
     *            the string
     * @param index
     *            the current string index
     * @param unicode
     *            the unicode flag
     * @return the next string index
     */
    public static long AdvanceStringIndex(String s, long index, boolean unicode) {
        /* steps 1-3 (not applicable) */
        /* step 4 */
        if (!unicode) {
            return index + 1;
        }
        /* steps 5-6 */
        if (index + 1 >= s.length()) {
            return index + 1;
        }
        /* steps 7-10 */
        int p = (int) index;
        if (!Character.isHighSurrogate(s.charAt(p)) || !Character.isLowSurrogate(s.charAt(p + 1))) {
            return index + 1;
        }
        /* step 11 */
        return index + 2;
    }

    private static Object[] GetReplacerArguments(ExecutionContext cx, String matched, String string, int position,
            MatchResult matchResult, int groupCount) {
        if (groupCount > GROUP_COUNT_LIMIT
                || cx.getRuntimeContext().isEnabled(CompatibilityOption.RegExpNamedCapture)) {
            return unreasonableLargeGetReplacerArguments(cx, matched, string, position, matchResult, groupCount);
        }
        Object[] arguments = new Object[groupCount + 3];
        arguments[0] = matched;
        for (int i = 1; i <= groupCount; ++i) {
            String group = matchResult.group(i);
            arguments[i] = (group != null ? group : UNDEFINED);
        }
        arguments[groupCount + 1] = position;
        arguments[groupCount + 2] = string;
        return arguments;
    }

    private static Object[] unreasonableLargeGetReplacerArguments(ExecutionContext cx, String matched, String string,
            int position, MatchResult matchResult, int groupCount) {
        // Same as above, except Object[] is replaced with ArrayList.
        ArrayList<Object> arguments = new ArrayList<>();
        arguments.add(matched);
        for (int i = 1; i <= groupCount; ++i) {
            String group = matchResult.group(i);
            arguments.add(group != null ? group : UNDEFINED);
        }
        arguments.add(position);
        arguments.add(string);
        if (cx.getRuntimeContext().isEnabled(CompatibilityOption.RegExpNamedCapture)) {
            Object groupsObject = namedGroupsObject(cx, string, matchResult);
            if (!Type.isUndefined(groupsObject)) {
                arguments.add(groupsObject);
            }
        }
        return arguments.toArray();
    }

    /**
     * 21.1.3.14.1 Runtime Semantics: GetSubstitution(matched, str, position, captures, replacement)
     * 
     * @param cx
     *            the execution context
     * @param matched
     *            the matched substring
     * @param string
     *            the input string
     * @param position
     *            the match position
     * @param captures
     *            the captured groups
     * @param namedGroups
     *            the optional namedGroups or {@code null}
     * @param replacement
     *            the replace string
     * @return the replacement string
     */
    private static String GetSubstitution(ExecutionContext cx, String matched, String string, int position,
            String[] captures, NamedGroups namedGroups, String replacement) {
        /* step 1 (not applicable) */
        /* step 2 */
        int matchLength = matched.length();
        /* step 3 (not applicable) */
        /* step 4 */
        int stringLength = string.length();
        /* step 5 */
        assert position >= 0;
        /* step 6 */
        assert position <= stringLength;
        /* steps 7-8 (not applicable) */
        /* step 9 */
        int tailPos = Math.min(position + matchLength, stringLength);
        /* step 10 */
        int m = captures.length;
        /* step 11 */
        int cursor = replacement.indexOf('$');
        if (cursor < 0) {
            return replacement;
        }
        final int length = replacement.length();
        int lastCursor = 0;
        StrBuilder result = new StrBuilder(cx);
        for (;;) {
            if (lastCursor < cursor) {
                result.append(replacement, lastCursor, cursor);
            }
            if (++cursor == length) {
                result.append('$');
                break;
            }
            assert cursor < length;
            char c = replacement.charAt(cursor++);
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
                if (cursor < length) {
                    char d = replacement.charAt(cursor);
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
                    result.append('$').append(c);
                } else {
                    assert n >= 1 && n <= 99;
                    String capture = captures[n - 1];
                    if (capture != null) {
                        result.append(capture);
                    }
                }
                break;
            }
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
            case '<':
                if (namedGroups == null) {
                    result.append("$<");
                    break;
                }
                int closing = replacement.indexOf('>', cursor);
                if (closing < 0) {
                    result.append("$<");
                    break;
                }
                String groupName = replacement.substring(cursor, closing);
                String capture = namedGroups.group(groupName);
                if (capture != null) {
                    result.append(capture);
                }
                cursor = closing + 1;
                break;
            default:
                result.append('$').append(c);
                break;
            }
            lastCursor = cursor;
            cursor = replacement.indexOf('$', cursor);
            if (cursor < 0) {
                if (lastCursor < length) {
                    result.append(replacement, lastCursor, length);
                }
                break;
            }
        }
        /* step 12 */
        return result.toString();
    }

    /**
     * Internal {@code RegExp.prototype.test()} function.
     * 
     * @param cx
     *            the execution context
     * @param rx
     *            the regular expression object
     * @param string
     *            the string
     * @return the result string
     */
    public static boolean RegExpTest(ExecutionContext cx, RegExpObject rx, CharSequence string) {
        return matchResultOrNull(cx, rx, string.toString(), false) != null;
    }

    private static void RegExpThrowIfLastIndexNonWritable(ExecutionContext cx, RegExpObject rx) {
        if (!rx.getLastIndex().isWritable()) {
            throw newTypeError(cx, Messages.Key.PropertyNotModifiable, "lastIndex");
        }
    }

    private static int RegExpGetLastIndex(ExecutionContext cx, RegExpObject rx) {
        return (int) Math.min(ToLength(cx, rx.getLastIndex().getValue()), Integer.MAX_VALUE);
    }

    private static void RegExpSetLastIndex(ExecutionContext cx, RegExpObject rx, int lastIndex) {
        RegExpThrowIfLastIndexNonWritable(cx, rx);
        rx.getLastIndex().setValue(lastIndex);
    }

    private static boolean matchOrFind(MatcherState m, int lastIndex, boolean sticky) {
        if (!sticky) {
            return m.find(lastIndex);
        }
        return m.matches(lastIndex);
    }

    private static boolean RegExpTest(ExecutionContext cx, RegExpObject rx, String string) {
        return matchResultOrNull(cx, rx, string, true) != null;
    }

    private static Object RegExpSearch(ExecutionContext cx, RegExpObject rx, String s) {
        // Directly throw TypeErrors instead of saving and restoring the "lastIndex" property.
        Object previousLastIndex = rx.getLastIndex().getValue();
        boolean lastIndexIsZero = SameValue(previousLastIndex, 0);
        if (!lastIndexIsZero) {
            RegExpThrowIfLastIndexNonWritable(cx, rx);
        }
        /* steps 1-3 (not applicable) */
        /* steps 4-7 */
        boolean sticky = rx.isSet(RegExpObject.Flags.Sticky);
        boolean global = rx.isSet(RegExpObject.Flags.Global);
        MatchResult result = matchResultOrNull(cx, rx, s, 0, sticky, true);
        if (lastIndexIsZero && (global || sticky)) {
            // Emulate the lastIndex update from RegExpBuiltinExec.
            RegExpThrowIfLastIndexNonWritable(cx, rx);
        }
        /* step 8 */
        if (result == null) {
            return -1;
        }
        /* step 9 */
        return result.start();
    }

    private static Object RegExpMatch(ExecutionContext cx, RegExpObject rx, String s, boolean global,
            boolean fullUnicode) {
        /* step 5 */
        if (!global) {
            MatcherResult result = matchResultOrNull(cx, rx, s, true);
            if (result == null) {
                return NULL;
            }
            return toMatchArray(cx, s, result);
        }
        /* step 6 */
        /* step 6.a (not applicable) */
        /* step 6.b */
        RegExpSetLastIndex(cx, rx, 0);
        int lastIndex = 0;
        /* step 6.c */
        ArrayObject array = null;
        MatcherResult lastResult = null;
        /* steps 6.d-e */
        MatcherState matcher = rx.getRegExpMatcher().matcher(s);
        boolean sticky = rx.isSet(RegExpObject.Flags.Sticky);
        for (int n = 0;; ++n) {
            /* step 6.e.i */
            boolean matchSucceeded = matchOrFind(matcher, lastIndex, sticky);
            /* step 6.e.ii */
            if (!matchSucceeded) {
                break;
            }
            if (array == null) {
                array = ArrayCreate(cx, 0);
            }
            lastResult = matcher.toMatchResult();
            lastIndex = lastResult.end();
            /* step 6.e.iii */
            CreateDataProperty(cx, array, n, lastResult.group());
            if (lastResult.start() == lastIndex) {
                lastIndex = AdvanceStringIndex(s, lastIndex, fullUnicode);
            }
            if (lastIndex > s.length()) {
                break;
            }
        }
        if (lastResult == null) {
            return NULL;
        }
        boolean storeResult = true;
        if (cx.getRuntimeContext().isEnabled(CompatibilityOption.LegacyRegExp)) {
            if (cx.getRealm() == rx.getRealm()) {
                if (!rx.isLegacyFeaturesEnabled()) {
                    RegExpConstructor.invalidateLastMatchResult(cx);
                    storeResult = false;
                }
            } else {
                storeResult = false;
            }
        }
        if (storeResult) {
            RegExpConstructor.storeLastMatchResult(cx, s, lastResult);
        }
        return array;
    }

    private static List<MatchResult> RegExpReplace(ExecutionContext cx, RegExpObject rx, String s, boolean global,
            boolean fullUnicode) {
        if (!global) {
            /* step 11.a */
            MatchResult result = matchResultOrNull(cx, rx, s, false);
            /* step 11.b */
            if (result == null) {
                return Collections.emptyList();
            }
            /* step 11.c */
            return Collections.singletonList(result);
        }
        /* step 8 */
        /* step 8.a (not applicable) */
        /* step 8.b */
        RegExpSetLastIndex(cx, rx, 0);
        int lastIndex = 0;
        /* step 9 */
        ArrayList<MatchResult> results = new ArrayList<>();
        /* steps 10-11 */
        MatcherState matcher = rx.getRegExpMatcher().matcher(s);
        boolean sticky = rx.isSet(RegExpObject.Flags.Sticky);
        while (true) {
            /* step 11.a */
            boolean matchSucceeded = matchOrFind(matcher, lastIndex, sticky);
            /* step 11.b */
            if (!matchSucceeded) {
                break;
            }
            /* step 11.c */
            MatchResult result = matcher.toMatchResult();
            results.add(result);
            lastIndex = result.end();
            if (result.start() == lastIndex) {
                lastIndex = AdvanceStringIndex(s, lastIndex, fullUnicode);
            }
            if (lastIndex > s.length()) {
                break;
            }
        }
        return results;
    }

    private static ArrayObject RegExpSplit(ExecutionContext cx, RegExpObject rx, String s, boolean unicodeMatching,
            int lim, boolean isShared) {
        /* steps 1-10, 13 (not applicable) */
        /* step 11 */
        ArrayObject a = ArrayCreate(cx, 0);
        /* step 12 */
        int lengthA = 0;
        /* step 14 */
        int size = s.length();
        /* step 15 */
        int p = 0;
        /* step 16 */
        if (lim == 0) {
            return a;
        }
        /* step 17 */
        if (size == 0) {
            MatchResult result;
            if (isShared) {
                result = matchResultOrNull(cx, rx, s, 0, true, true);
            } else {
                result = matchResultOrNull(cx, rx, s, true);
            }
            if (result != null) {
                return a;
            }
            CreateDataProperty(cx, a, 0, s);
            return a;
        }
        if (!isShared) {
            /* step 19.a */
            RegExpSetLastIndex(cx, rx, 0);
        }
        /* step 18 */
        int q = p;
        /* step 19 */
        int lastStart = -1;
        MatcherState matcher = rx.getRegExpMatcher().matcher(s);
        boolean storeResult = true, invalidateResult = false;
        if (cx.getRuntimeContext().isEnabled(CompatibilityOption.LegacyRegExp)) {
            if (cx.getRealm() == rx.getRealm()) {
                if (!rx.isLegacyFeaturesEnabled()) {
                    invalidateResult = true;
                    storeResult = false;
                }
            } else {
                storeResult = false;
            }
        }
        while (q != size) {
            /* steps 19.a-c */
            boolean match = matcher.find(q);
            if (!match) {
                break;
            }
            MatcherResult result = matcher.toMatchResult();
            if (storeResult) {
                RegExpConstructor.storeLastMatchResult(cx, s, result);
            } else if (invalidateResult) {
                RegExpConstructor.invalidateLastMatchResult(cx);
                invalidateResult = false;
            }
            /* steps 19.d.i-ii */
            int e = result.end();
            /* steps 19.d.iii-iv */
            if (e == p) {
                /* step 19.d.iii */
                q = AdvanceStringIndex(s, q, unicodeMatching);
            } else {
                /* step 19.d.iv */
                String t = s.substring(p, lastStart = result.start());
                CreateDataProperty(cx, a, lengthA, t);
                lengthA += 1;
                if (lengthA == lim) {
                    if (!isShared) {
                        RegExpSetLastIndex(cx, rx, e);
                    }
                    return a;
                }
                int groupCount = result.groupCount();
                for (int i = 1; i <= groupCount; ++i) {
                    String cap = result.group(i);
                    CreateDataProperty(cx, a, lengthA, cap != null ? cap : UNDEFINED);
                    lengthA += 1;
                    if (lengthA == lim) {
                        if (!isShared) {
                            RegExpSetLastIndex(cx, rx, e);
                        }
                        return a;
                    }
                }
                p = e;
                q = p;
            }
        }
        if (lastStart == size) {
            return a;
        }
        /* step 20 */
        String t = s.substring(p, size);
        /* step 21 */
        CreateDataProperty(cx, a, lengthA, t);
        /* step 22 */
        return a;
    }

    // Group count is limited to 65535 for native RegExps, but user-defined regular expressions may use a higher limit.
    private static final int GROUP_COUNT_LIMIT = 0xFFFF;
    // String substitutions can only reference groups up to group-index 99.
    private static final int SUBST_GROUP_COUNT_LIMIT = 99;
    private static final String[] EMPTY_GROUPS = new String[0];

    /**
     * Returns the capturing groups of the {@link MatchResult} argument.
     * 
     * @param matchResult
     *            the match result
     * @param groupCount
     *            the number of capturing groups
     * @return the match groups
     */
    private static String[] groups(MatchResult matchResult, int groupCount) {
        if (groupCount == 0) {
            return EMPTY_GROUPS;
        }
        String[] groups = new String[Math.min(groupCount, SUBST_GROUP_COUNT_LIMIT)];
        for (int i = 0; i < groupCount; ++i) {
            String group = matchResult.group(i + 1);
            if (i < SUBST_GROUP_COUNT_LIMIT) {
                groups[i] = group;
            }
        }
        return groups;
    }

    private static NamedGroups namedGroups(ExecutionContext cx, String s, MatchResult matchResult) {
        if (matchResult instanceof ScriptObjectMatchResult) {
            ScriptObjectMatchResult scriptMatchResult = (ScriptObjectMatchResult) matchResult;
            Object groups = Get(cx, scriptMatchResult.object, "groups");
            if (Type.isUndefined(groups)) {
                return null;
            }
            return new ScriptNamedGroups(cx, ToObject(cx, groups));
        }
        assert matchResult instanceof MatcherResult;
        MatcherResult result = (MatcherResult) matchResult;
        if (result.groups().isEmpty()) {
            return null;
        }
        return new MatchNamedGroups(result);
    }

    private static Object namedGroupsObject(ExecutionContext cx, String s, MatchResult matchResult) {
        if (matchResult instanceof ScriptObjectMatchResult) {
            ScriptObjectMatchResult scriptMatchResult = (ScriptObjectMatchResult) matchResult;
            return Get(cx, scriptMatchResult.object, "groups");
        }
        assert matchResult instanceof MatcherResult;
        return createGroupsObjectOrUndefined(cx, (MatcherResult) matchResult);
    }

    private interface NamedGroups {
        String group(String name);
    }

    private static final class ScriptNamedGroups implements NamedGroups {
        private final ExecutionContext cx;
        private final ScriptObject groups;

        ScriptNamedGroups(ExecutionContext cx, ScriptObject groups) {
            this.cx = cx;
            this.groups = groups;
        }

        @Override
        public String group(String name) {
            Object group = Get(cx, groups, name);
            return !Type.isUndefined(group) ? ToFlatString(cx, group) : null;
        }
    }

    private static final class MatchNamedGroups implements NamedGroups {
        private final MatcherResult matchResult;

        MatchNamedGroups(MatcherResult matchResult) {
            this.matchResult = matchResult;
        }

        @Override
        public String group(String name) {
            if (!matchResult.groups().contains(name)) {
                return null;
            }
            return matchResult.group(name);
        }
    }

    private static final class ScriptObjectMatchResult implements MatchResult {
        private final ExecutionContext cx;
        private final ScriptObject object;

        ScriptObjectMatchResult(ExecutionContext cx, ScriptObject object) {
            this.cx = cx;
            this.object = object;
        }

        @Override
        public int start() {
            return start(0);
        }

        @Override
        public int start(int group) {
            if (group == 0) {
                return (int) ToInteger(cx, Get(cx, object, "index"));
            }
            throw new UnsupportedOperationException();
        }

        @Override
        public int end() {
            return end(0);
        }

        @Override
        public int end(int group) {
            return start(group) + group(group).length();
        }

        @Override
        public String group() {
            return group(0);
        }

        @Override
        public String group(int group) {
            Object captured = Get(cx, object, group);
            if (group > 0 && Type.isUndefined(captured)) {
                return null;
            }
            return ToFlatString(cx, captured);
        }

        @Override
        public int groupCount() {
            int nCaptures = (int) Math.min(ToLength(cx, Get(cx, object, "length")), Integer.MAX_VALUE);
            return Math.max(nCaptures - 1, 0);
        }
    }
}
