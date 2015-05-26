/**
 * Copyright (c) 2012-2015 André Bargull
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
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ArrayObject.ArrayCreate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.MatchResult;

import com.github.anba.es6draft.regexp.IterableMatchResult;
import com.github.anba.es6draft.regexp.MatchState;
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
    }

    private static RegExpObject thisRegExpObject(ExecutionContext cx, Object object) {
        if (object instanceof RegExpObject) {
            return (RegExpObject) object;
        }
        throw newTypeError(cx, Messages.Key.IncompatibleObject);
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
            /* steps 1-2 */
            if (!Type.isObject(thisValue)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 3 */
            RegExpObject r = thisRegExpObject(cx, thisValue);
            /* steps 4-5 */
            String s = ToFlatString(cx, string);
            /* step 6 */
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
        @Accessor(name = "flags", type = Accessor.Type.Getter)
        public static Object flags(ExecutionContext cx, Object thisValue) {
            /* step 2 */
            if (!Type.isObject(thisValue)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 1 */
            ScriptObject r = Type.objectValue(thisValue);
            /* step 3 */
            StringBuilder result = new StringBuilder(5);
            /* steps 4-6 */
            if (ToBoolean(Get(cx, r, "global"))) {
                result.append('g');
            }
            /* steps 7-9 */
            if (ToBoolean(Get(cx, r, "ignoreCase"))) {
                result.append('i');
            }
            /* steps 10-12 */
            if (ToBoolean(Get(cx, r, "multiline"))) {
                result.append('m');
            }
            /* steps 13-15 */
            if (ToBoolean(Get(cx, r, "unicode"))) {
                result.append('u');
            }
            /* steps 16-18 */
            if (ToBoolean(Get(cx, r, "sticky"))) {
                result.append('y');
            }
            /* step 19 */
            return result.toString();
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
        @Accessor(name = "global", type = Accessor.Type.Getter,
                nativeId = RegExpPrototypeGlobal.class)
        public static Object global(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            RegExpObject r = thisRegExpObject(cx, thisValue);
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
        @Accessor(name = "ignoreCase", type = Accessor.Type.Getter)
        public static Object ignoreCase(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            RegExpObject r = thisRegExpObject(cx, thisValue);
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
        @Accessor(name = "multiline", type = Accessor.Type.Getter)
        public static Object multiline(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            RegExpObject r = thisRegExpObject(cx, thisValue);
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
            /* steps 1-4 */
            RegExpObject r = thisRegExpObject(cx, thisValue);
            /* steps 5-7 */
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
        @Accessor(name = "sticky", type = Accessor.Type.Getter,
                nativeId = RegExpPrototypeSticky.class)
        public static Object sticky(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            RegExpObject r = thisRegExpObject(cx, thisValue);
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
            /* steps 3-4 */
            // TODO: ToFlatString for small strings, ToString for large strings?
            String s = ToFlatString(cx, string);
            /* steps 5-6 */
            MatchResult match = getMatcherOrNull(cx, r, s, true);
            /* step 7 */
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
            /* steps 3-4 */
            CharSequence pattern = ToString(cx, Get(cx, r, "source"));
            if (pattern.length() == 0
                    && cx.getRealm().isEnabled(CompatibilityOption.RegExpEmptySource)) {
                pattern = "(?:)";
            }
            /* steps 5-6 */
            CharSequence flags = ToString(cx, Get(cx, r, "flags"));
            /* step 7 */
            return new StringBuilder().append('/').append(pattern).append('/').append(flags)
                    .toString();
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
        @Accessor(name = "unicode", type = Accessor.Type.Getter)
        public static Object unicode(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            RegExpObject r = thisRegExpObject(cx, thisValue);
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
            /* steps 3-4 */
            String s = ToFlatString(cx, string);
            /* steps 5-6 */
            boolean global = ToBoolean(Get(cx, rx, "global"));
            /* steps 7-8 */
            if (!global) {
                ScriptObject result = RegExpExec(cx, rx, s);
                return result != null ? result : NULL;
            } else {
                /* steps 8.a-b */
                boolean fullUnicode = ToBoolean(Get(cx, rx, "unicode"));
                /* steps 8.c-d */
                Set(cx, rx, "lastIndex", 0, true);
                /* step 8.e */
                ArrayObject array = ArrayCreate(cx, 0);
                /* steps 8.f-g */
                for (int n = 0;; ++n) {
                    // ScriptObject result = RegExpExec(cx, rx, s);
                    MatchResult result = getMatcherOrNull(cx, rx, s, true);
                    if (result == null) {
                        return n == 0 ? NULL : array;
                    }
                    // String matchStr = ToFlatString(cx, Get(cx, result, "0"));
                    String matchStr = result.group(0);
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
        public static Object replace(ExecutionContext cx, Object thisValue, Object string,
                Object replaceValue) {
            /* step 2 */
            if (!Type.isObject(thisValue)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 1 */
            ScriptObject rx = Type.objectValue(thisValue);
            /* steps 3-4 */
            String s = ToFlatString(cx, string);
            /* step 5 */
            int lengthS = s.length();
            /* step 6 */
            boolean functionalReplace = IsCallable(replaceValue);
            /* step 7 */
            String replaceValueString = null;
            Callable replaceValueCallable = null;
            if (!functionalReplace) {
                replaceValueString = ToFlatString(cx, replaceValue);
            } else {
                replaceValueCallable = (Callable) replaceValue;
            }
            /* steps 8-9 */
            boolean global = ToBoolean(Get(cx, rx, "global"));
            /* step 10 */
            boolean fullUnicode = false;
            if (global) {
                /* steps 10.a-b */
                fullUnicode = ToBoolean(Get(cx, rx, "unicode"));
                /* steps 10.c-d */
                Set(cx, rx, "lastIndex", 0, true);
            }
            /* step 11 */
            ArrayList<MatchResult> results = new ArrayList<>();
            /* step 12 */
            boolean done = false;
            /* step 13 */
            while (!done) {
                /* steps 13.a-b */
                MatchResult result = getMatcherOrNull(cx, rx, s, false);
                /* steps 13.c-d */
                if (result == null) {
                    /* step 13.c */
                    done = true;
                } else {
                    /* step 13.d */
                    results.add(result);
                    if (!global) {
                        done = true;
                    } else {
                        String matchStr = result.group(0);
                        if (matchStr.isEmpty()) {
                            long thisIndex = ToLength(cx, Get(cx, rx, "lastIndex"));
                            long nextIndex = AdvanceStringIndex(s, thisIndex, fullUnicode);
                            Set(cx, rx, "lastIndex", nextIndex, true);
                        }
                    }
                }
            }
            // fast-path if no matches were found
            if (results.isEmpty()) {
                return s;
            }
            /* step 14 */
            StringBuilder accumulatedResult = new StringBuilder();
            /* step 15 */
            int nextSourcePosition = 0;
            /* step 16 */
            for (MatchResult result : results) {
                if (!(result instanceof ScriptObjectMatchResult)) {
                    RegExpConstructor.storeLastMatchResult(cx, s, result);
                }
                /* steps 16.a-c */
                int nCaptures = result.groupCount();
                /* steps 16.d-e */
                String matched = result.group(0);
                /* step 16.f */
                int matchLength = matched.length();
                /* steps 16.g-i */
                int position = Math.max(Math.min(result.start(), lengthS), 0);
                /* steps 16.j-o */
                String replacement;
                if (functionalReplace) {
                    Object[] replacerArgs = GetReplacerArguments(matched, s, position, result,
                            nCaptures);
                    Object replValue = replaceValueCallable.call(cx, UNDEFINED, replacerArgs);
                    replacement = ToFlatString(cx, replValue);
                } else {
                    String[] captures = groups(result, nCaptures);
                    replacement = GetSubstitution(matched, s, position, captures,
                            replaceValueString);
                }
                /* step 16.p */
                if (position >= nextSourcePosition) {
                    accumulatedResult.append(s, nextSourcePosition, position).append(replacement);
                    nextSourcePosition = position + matchLength;
                }
            }
            /* step 17 */
            if (nextSourcePosition >= lengthS) {
                return accumulatedResult.toString();
            }
            /* step 18 */
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
            /* steps 3-4 */
            String s = ToFlatString(cx, string);
            /* steps 5-6 */
            Object previousLastIndex = Get(cx, rx, "lastIndex");
            /* steps 7-8 */
            Set(cx, rx, "lastIndex", 0, true);
            /* steps 9-10 */
            MatchResult result = getMatcherOrNull(cx, rx, s, true);
            /* steps 11-12 */
            Set(cx, rx, "lastIndex", previousLastIndex, true);
            /* step 13 */
            if (result == null) {
                return -1;
            }
            /* step 14 */
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
        public static Object split(ExecutionContext cx, Object thisValue, Object string,
                Object limit) {
            /* step 2 */
            if (!Type.isObject(thisValue)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 1 */
            ScriptObject rx = Type.objectValue(thisValue);
            /* steps 3-4 */
            String s = ToFlatString(cx, string);
            /* steps 5-6 */
            Constructor c = SpeciesConstructor(cx, rx, Intrinsics.RegExp);
            /* steps 7-8 */
            String flags = ToFlatString(cx, Get(cx, rx, "flags"));
            /* steps 9-10 */
            boolean unicodeMatching = flags.indexOf('u') != -1;
            /* steps 11-12 */
            String newFlags = flags.indexOf('y') != -1 ? flags : flags + 'y';

            // Optimization
            ScriptObject splitter;
            long lim;
            if (rx instanceof RegExpObject && c instanceof RegExpConstructor
                    && c == cx.getIntrinsic(Intrinsics.RegExp)) {
                RegExpObject re = (RegExpObject) rx;
                RegExpConstructor reConstructor = (RegExpConstructor) c;
                // 21.2.3.1 RegExp ( pattern, flags ) - steps 1-2
                // NB: Only executed for its side-effects.
                IsRegExp(cx, re);
                // Extract pattern and default multiline before calling ToLength.
                String pattern = re.getOriginalSource();
                boolean defaultMultiline = RegExpConstructor.isDefaultMultiline(cx);
                /* steps 17-18 */
                lim = Type.isUndefined(limit) ? 0x1F_FFFF_FFFF_FFFFL : ToLength(cx, limit);
                // Test if required RegExp.prototype methods are still the default values.
                // Also test if pattern/flags match the original pattern/flags. Side-effects in
                // IsRegExp or ToLength may have called RegExp.prototype.compile.
                if (isBuiltinRegExpPrototypeForExec(cx) && pattern.equals(re.getOriginalSource())
                        && flags.equals(re.getOriginalFlags())) {
                    return RegExpSplit(cx, re, s, unicodeMatching, lim);
                }
                // Optimization not applicable, fall back to slower spec algorithm. RegExpCreate
                // must be side-effect free, otherwise these steps are detectable from script code.
                splitter = RegExpCreate(cx, reConstructor, pattern, newFlags, defaultMultiline);
            } else {
                /* steps 13-14 */
                splitter = c.construct(cx, c, rx, newFlags);
                /* steps 17-18 */
                lim = Type.isUndefined(limit) ? 0x1F_FFFF_FFFF_FFFFL : ToLength(cx, limit);
            }

            /* step 15 */
            ArrayObject a = ArrayCreate(cx, 0);
            /* step 16 */
            int lengthA = 0;
            /* steps 17-18 (above) */
            /* step 19 */
            int size = s.length();
            /* step 20 */
            int p = 0;
            /* step 21 */
            if (lim == 0) {
                return a;
            }
            /* step 22 */
            if (size == 0) {
                // ScriptObject z = RegExpExec(cx, splitter, s);
                MatchResult z = getMatcherOrNull(cx, splitter, s, true);
                if (z != null) {
                    return a;
                }
                CreateDataProperty(cx, a, 0, s);
                return a;
            }
            /* step 23 */
            int q = p;
            /* step 24 */
            while (q < size) {
                /* steps 24.a-b */
                Set(cx, splitter, "lastIndex", q, true);
                /* steps 24.c-d */
                // ScriptObject z = RegExpExec(cx, splitter, s);
                MatchResult z = getMatcherOrNull(cx, splitter, s, true);
                /* step 24.e */
                if (z == null) {
                    q = AdvanceStringIndex(s, q, unicodeMatching);
                    continue;
                }
                /* step 24.f */
                /* step 24.f.i-ii */
                int e = (int) ToLength(cx, Get(cx, splitter, "lastIndex"));
                /* step 24.f.iii */
                if (e == p) {
                    q = AdvanceStringIndex(s, q, unicodeMatching);
                    continue;
                }
                /* step 24.f.iv */
                String t = s.substring(p, q);
                CreateDataProperty(cx, a, lengthA, t);
                lengthA += 1;
                if (lengthA == lim) {
                    return a;
                }
                Iterator<String> iterator = groupIterator(z, z.groupCount());
                while (iterator.hasNext()) {
                    Object nextCap = iterator.next();
                    CreateDataProperty(cx, a, lengthA, nextCap != null ? nextCap : UNDEFINED);
                    lengthA += 1;
                    if (lengthA == lim) {
                        return a;
                    }
                }
                p = e;
                q = p;
            }
            /* step 25 */
            String t = s.substring(p, size);
            /* steps 26-27 */
            CreateDataProperty(cx, a, lengthA, t);
            /* step 28 */
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
        public static Object compile(ExecutionContext cx, Object thisValue, Object pattern,
                Object flags) {
            /* step 1 (omitted) */
            /* step 2 */
            if (!(thisValue instanceof RegExpObject)) {
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            RegExpObject r = (RegExpObject) thisValue;
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
        /* steps 3-4 */
        // FIXME: spec bug - change to GetMethod ?
        Object exec = Get(cx, r, "exec");
        /* step 5 */
        // Don't take the slow path for built-in RegExp.prototype.exec
        if (IsCallable(exec) && !isBuiltin(cx, exec, RegExpPrototypeExec.class)) {
            return RegExpUserExec(cx, (Callable) exec, r, s);
        }
        /* step 6 */
        RegExpObject rx = thisRegExpObject(cx, r);
        /* step 7 */
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
    private static MatchResult getMatcherOrNull(ExecutionContext cx, ScriptObject r, String s,
            boolean storeResult) {
        /* steps 1-2 (not applicable) */
        /* steps 3-4 */
        Object exec = Get(cx, r, "exec");
        /* step 5 */
        // Don't take the slow path for built-in RegExp.prototype.exec
        if (IsCallable(exec) && !isBuiltin(cx, exec, RegExpPrototypeExec.class)) {
            ScriptObject o = RegExpUserExec(cx, (Callable) exec, r, s);
            return o != null ? new ScriptObjectMatchResult(cx, o) : null;
        }
        /* step 6 */
        RegExpObject rx = thisRegExpObject(cx, r);
        /* step 7 */
        return getMatcherOrNull(cx, rx, s, storeResult);
    }

    /**
     * Marker class for {@code RegExp.prototype.exec}.
     */
    private static final class RegExpPrototypeExec {
    }

    /**
     * Marker class for {@code RegExp.prototype.global}.
     */
    private static final class RegExpPrototypeGlobal {
    }

    /**
     * Marker class for {@code RegExp.prototype.sticky}.
     */
    private static final class RegExpPrototypeSticky {
    }

    private static boolean isBuiltin(ExecutionContext cx, Object value, Class<?> nativeId) {
        return value instanceof NativeFunction && ((NativeFunction) value).getId() == nativeId
                && ((NativeFunction) value).getRealm() == cx.getRealm();
    }

    private static ScriptObject RegExpUserExec(ExecutionContext cx, Callable exec, ScriptObject r,
            String s) {
        /* steps 5.a-5.b */
        Object result = ((Callable) exec).call(cx, r, s);
        /* step 5.c */
        if (!Type.isObjectOrNull(result)) {
            throw newTypeError(cx, Messages.Key.NotObjectOrNull);
        }
        /* step 5.d */
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
     * @return the match result object or null
     */
    private static ArrayObject RegExpBuiltinExec(ExecutionContext cx, RegExpObject r, String s) {
        /* steps 1-18 */
        MatchResult m = getMatcherOrNull(cx, r, s, true);
        if (m == null) {
            return null;
        }
        /* steps 19-29 */
        return toMatchResult(cx, s, m);
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
    private static MatchResult getMatcherOrNull(ExecutionContext cx, RegExpObject r, String s,
            boolean storeResult) {
        /* step 1 */
        assert r.getRegExpMatcher() != null;
        /* step 2 (not applicable) */
        /* step 3 */
        int length = s.length();
        /* steps 4-5 */
        long lastIndex = ToLength(cx, Get(cx, r, "lastIndex"));
        /* steps 6-7 */
        boolean global = ToBoolean(Get(cx, r, "global"));
        /* steps 8-9 */
        boolean sticky = ToBoolean(Get(cx, r, "sticky"));
        /* step 10 */
        if (!global && !sticky) {
            lastIndex = 0;
        }
        /* step 15.a */
        if (lastIndex > length) {
            Set(cx, r, "lastIndex", 0, true);
            return null;
        }
        /* step 11 */
        RegExpMatcher matcher = r.getRegExpMatcher();
        /* steps 12-13 (not applicable) */
        /* steps 14-15 */
        MatchState m = matcher.matcher(s);
        boolean matchSucceeded;
        if (!sticky) {
            matchSucceeded = m.find((int) lastIndex);
        } else {
            matchSucceeded = m.matches((int) lastIndex);
        }
        /* step 15.a, 15.c */
        if (!matchSucceeded) {
            Set(cx, r, "lastIndex", 0, true);
            return null;
        }
        /* steps 16-17 */
        int e = m.end();
        /* step 18 */
        if (global || sticky) {
            Set(cx, r, "lastIndex", e, true);
        }
        MatchResult result = m.toMatchResult();
        if (storeResult) {
            RegExpConstructor.storeLastMatchResult(cx, s, result);
        }
        return result;
    }

    /**
     * 21.2.5.2.2 Runtime Semantics: RegExpBuiltinExec ( R, S ) (1)
     * 
     * @param r
     *            the regular expression object
     * @param s
     *            the string
     * @param lastIndex
     *            the lastIndex position
     * @return the match result or {@code null}
     */
    private static MatchResult getMatcherOrNull(RegExpObject r, String s, int lastIndex) {
        /* step 1 */
        assert r.getRegExpMatcher() != null;
        /* steps 2-5 (not applicable) */
        /* steps 6-7 */
        boolean global = r.isSet(RegExpObject.Flags.Global);
        /* steps 8-9 */
        boolean sticky = r.isSet(RegExpObject.Flags.Sticky);
        /* step 10 */
        if (!global && !sticky) {
            lastIndex = 0;
        }
        /* step 15.a */
        if (lastIndex > s.length()) {
            return null;
        }
        /* step 11 */
        RegExpMatcher matcher = r.getRegExpMatcher();
        /* steps 12-13 (not applicable) */
        /* steps 14-15 */
        MatchState m = matcher.matcher(s);
        boolean matchSucceeded;
        if (!sticky) {
            matchSucceeded = m.find(lastIndex);
        } else {
            matchSucceeded = m.matches(lastIndex);
        }
        /* step 15.a, 15.c */
        if (!matchSucceeded) {
            return null;
        }
        return m;
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
    private static ArrayObject toMatchResult(ExecutionContext cx, String s, MatchResult m) {
        /* steps 16-17 */
        int e = m.end();
        /* step 19 */
        int n = m.groupCount();
        /* step 20 */
        ArrayObject array = ArrayCreate(cx, n + 1);
        /* step 21 (omitted) */
        /* step 22 */
        int matchIndex = m.start();
        /* steps 23-25 */
        CreateDataProperty(cx, array, "index", matchIndex);
        CreateDataProperty(cx, array, "input", s);
        /* step 26 */
        String matchedSubstr = s.substring(matchIndex, e);
        /* step 27 */
        CreateDataProperty(cx, array, 0, matchedSubstr);
        /* step 28 */
        Iterator<String> iterator = groupIterator(m, n);
        for (int i = 1; iterator.hasNext(); ++i) {
            String capture = iterator.next();
            CreateDataProperty(cx, array, i, (capture != null ? capture : UNDEFINED));
        }
        /* step 29 */
        return array;
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
        /* step 12 */
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
    private static long AdvanceStringIndex(String s, long index, boolean unicode) {
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
        /* step 12 */
        return index + 2;
    }

    private static boolean isBuiltinRegExpPrototypeForExec(ExecutionContext cx) {
        OrdinaryObject prototype = cx.getIntrinsic(Intrinsics.RegExpPrototype);
        Property execProp = prototype.getOwnProperty(cx, "exec");
        if (execProp == null || !isBuiltin(cx, execProp.getValue(), RegExpPrototypeExec.class)) {
            return false;
        }
        Property globalProp = prototype.getOwnProperty(cx, "global");
        if (globalProp == null
                || !isBuiltin(cx, globalProp.getGetter(), RegExpPrototypeGlobal.class)) {
            return false;
        }
        Property stickyProp = prototype.getOwnProperty(cx, "sticky");
        if (stickyProp == null
                || !isBuiltin(cx, stickyProp.getGetter(), RegExpPrototypeSticky.class)) {
            return false;
        }
        return true;
    }

    private static Object[] GetReplacerArguments(String matched, String string, int position,
            MatchResult matchResult, int groupCount) {
        Object[] arguments = new Object[groupCount + 3];
        arguments[0] = matched;
        Iterator<String> iterator = groupIterator(matchResult, groupCount);
        for (int i = 1; iterator.hasNext(); ++i) {
            String group = iterator.next();
            arguments[i] = (group != null ? group : UNDEFINED);
        }
        arguments[groupCount + 1] = position;
        arguments[groupCount + 2] = string;
        return arguments;
    }

    /**
     * 21.1.3.14.1 Runtime Semantics: GetSubstitution(matched, str, position, captures, replacement)
     * 
     * @param matched
     *            the matched substring
     * @param string
     *            the input string
     * @param position
     *            the match position
     * @param captures
     *            the captured groups
     * @param replacement
     *            the replace string
     * @return the replacement string
     */
    private static String GetSubstitution(String matched, String string, int position,
            String[] captures, String replacement) {
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
        StringBuilder result = new StringBuilder();
        for (int cursor = 0, len = replacement.length(); cursor < len;) {
            char c = replacement.charAt(cursor++);
            if (c == '$' && cursor < len) {
                c = replacement.charAt(cursor++);
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
    public static boolean RegExpTest(ExecutionContext cx, RegExpObject rx, String string) {
        int lastIndex = 0;
        return getMatcherOrNull(rx, string, lastIndex) != null;
    }

    /**
     * Internal {@code RegExp.prototype.replace()} function.
     * 
     * @param cx
     *            the execution context
     * @param rx
     *            the regular expression object
     * @param string
     *            the string
     * @param replaceValue
     *            the replacement string
     * @return the result string
     */
    public static String RegExpReplace(ExecutionContext cx, RegExpObject rx, String string,
            String replaceValue) {
        /* steps 1-5 (not applicable) */
        /* step 6 */
        int lengthS = string.length();
        /* steps 7-8 (not applicable) */
        /* steps 9-10 */
        boolean global = rx.isSet(RegExpObject.Flags.Global);
        /* step 11 */
        int lastIndex = 0;
        /* steps 12-14 (not applicable) */
        /* step 16 */
        StringBuilder accumulatedResult = new StringBuilder();
        /* step 17 */
        int nextSrcPosition = 0;
        /* step 18 (omitted) */
        /* steps 15, 19 */
        do {
            /* steps 15.a-15.b */
            MatchResult result = getMatcherOrNull(rx, string, lastIndex);
            /* step 15.c */
            if (result == null) {
                break;
            }
            /* step 15.d */
            String matched = result.group(0);
            int matchLength = matched.length();
            lastIndex = (matchLength > 0 ? result.end() : result.end() + 1);
            /* step 15.e (not applicable) */
            /* step 19 */
            int position = result.start();
            assert 0 <= position && position < lengthS;
            assert position >= nextSrcPosition;
            String[] captures = groups(result);
            String replacement = GetSubstitution(matched, string, position, captures, replaceValue);
            accumulatedResult.append(string, nextSrcPosition, position).append(replacement);
            nextSrcPosition = position + matchLength;
        } while (global);
        /* step 20 (not applicable) */
        assert nextSrcPosition <= lengthS;
        /* step 21 */
        return accumulatedResult.append(string, nextSrcPosition, lengthS).toString();
    }

    /**
     * Internal {@code RegExp.prototype.split()} function.
     * 
     * @param cx
     *            the execution context
     * @param rx
     *            the regular expression object
     * @param s
     *            the string
     * @param unicodeMatching
     *            {@code true} for unicode matching
     * @param lim
     *            the split limit
     * @return the split result array
     */
    private static ArrayObject RegExpSplit(ExecutionContext cx, RegExpObject rx, String s,
            boolean unicodeMatching, long lim) {
        /* steps 1-12, 17-18 (not applicable) */
        /* step 15 */
        ArrayObject a = ArrayCreate(cx, 0);
        /* step 16 */
        int lengthA = 0;
        /* step 19 */
        int size = s.length();
        /* step 20 */
        int p = 0;
        /* step 21 */
        if (lim == 0) {
            return a;
        }
        /* step 22 */
        if (size == 0) {
            MatchState matcher = rx.getRegExpMatcher().matcher(s);
            if (matcher.find(0)) {
                RegExpConstructor.storeLastMatchResult(cx, s, matcher.toMatchResult());
                return a;
            }
            CreateDataProperty(cx, a, 0, s);
            return a;
        }
        /* step 23 */
        int q = p;
        /* step 24 */
        int lastStart = -1;
        MatchState matcher = rx.getRegExpMatcher().matcher(s);
        while (q != size) {
            /* steps 24.a-d */
            boolean match = matcher.find(q);
            /* step 24.e (implicit) */
            if (!match) {
                break;
            }
            RegExpConstructor.storeLastMatchResult(cx, s, matcher.toMatchResult());
            /* steps 24.f.i-ii */
            int e = matcher.end();
            /* step 24.f.iii-iv */
            if (e == p) {
                /* step 24.f.iii */
                q = AdvanceStringIndex(s, q, unicodeMatching);
            } else {
                /* step 24.f.iv */
                String t = s.substring(p, lastStart = matcher.start());
                CreateDataProperty(cx, a, lengthA, t);
                lengthA += 1;
                if (lengthA == lim) {
                    return a;
                }
                Iterator<String> iterator = groupIterator(matcher, matcher.groupCount());
                while (iterator.hasNext()) {
                    String cap = iterator.next();
                    CreateDataProperty(cx, a, lengthA, cap != null ? cap : UNDEFINED);
                    lengthA += 1;
                    if (lengthA == lim) {
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
        /* step 25 */
        String t = s.substring(p, size);
        /* steps 26-27 */
        CreateDataProperty(cx, a, lengthA, t);
        /* step 28 */
        return a;
    }

    /**
     * Returns the capturing groups of the {@link MatchResult} argument.
     * 
     * @param matchResult
     *            the match result
     * @return the match groups
     */
    /*package*/static String[] groups(MatchResult matchResult) {
        return groups(matchResult, matchResult.groupCount());
    }

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
        Iterator<String> iterator = groupIterator(matchResult, groupCount);
        String[] groups = new String[groupCount];
        for (int i = 0; iterator.hasNext(); ++i) {
            groups[i] = iterator.next();
        }
        return groups;
    }

    private static Iterator<String> groupIterator(MatchResult matchResult, int groupCount) {
        if (matchResult instanceof IterableMatchResult) {
            return ((IterableMatchResult) matchResult).iterator();
        }
        return new GroupIterator(matchResult, groupCount);
    }

    private static final class GroupIterator implements Iterator<String> {
        private final MatchResult result;
        private final int groupCount;
        private int group = 1;

        GroupIterator(MatchResult result, int groupCount) {
            this.result = result;
            this.groupCount = groupCount;
        }

        @Override
        public boolean hasNext() {
            return group <= groupCount;
        }

        @Override
        public String next() {
            int group = this.group++;
            return result.group(group);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
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
            long nCaptures = ToLength(cx, Get(cx, object, "length"));
            return (int) Math.max(nCaptures - 1, 0);
        }
    }
}
