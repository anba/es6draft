/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.RegExpConstructor.EscapeRegExpPattern;
import static com.github.anba.es6draft.runtime.objects.RegExpConstructor.RegExpInitialise;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArray.ArrayCreate;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>21 Text Processing</h1><br>
 * <h2>21.2 RegExp (Regular Expression) Objects</h2>
 * <ul>
 * <li>21.2.5 Properties of the RegExp Prototype Object
 * <li>21.2.5 Properties of RegExp Instances
 * </ul>
 */
public class RegExpPrototype extends OrdinaryObject implements Initialisable {
    public RegExpPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        createProperties(this, cx, AdditionalProperties.class);
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
            }
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
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
         */
        @Function(name = "exec", arity = 1)
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
         */
        @Function(name = "test", arity = 1)
        public static Object test(ExecutionContext cx, Object thisValue, Object string) {
            /* steps 1-4 */
            RegExpObject r = thisRegExpValue(cx, thisValue);
            /* steps 5-6 */
            String s = ToFlatString(cx, string);
            /* steps 7-9 */
            Matcher m = getMatcherOrNull(cx, r, s);
            if (m == null) {
                return false;
            }
            RegExpConstructor.storeLastMatchResult(cx, r, s, m);
            return true;
        }

        /**
         * 21.2.5.14 get RegExp.prototype.unicode
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
                Matcher lastMatchResult = null;
                while (lastMatch) {
                    // Object result = RegExpExec(realm, rx, s);
                    Matcher result = getMatcherOrNull(cx, rx, s);
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
                        CharSequence matchStr = s.subSequence(result.start(), result.end());
                        DefinePropertyOrThrow(cx, array, ToString(n), new PropertyDescriptor(
                                matchStr, true, true, true));
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
         */
        @Function(name = "replace", arity = 2)
        public static Object replace(ExecutionContext cx, Object thisValue, Object s,
                Object replaceValue) {
            /* steps 1-4 */
            RegExpObject rx = thisRegExpValue(cx, thisValue);
            /* steps 5-6 */
            String string = ToFlatString(cx, s);
            // FIXME: spec issue - always call ToString(replValue) even if no match
            if (!IsCallable(replaceValue)) {
                replaceValue = ToFlatString(cx, replaceValue);
            }
            List<Matcher> matches = new ArrayList<>();
            // cf. RegExp.prototype.match
            boolean global = ToBoolean(Get(cx, rx, "global"));
            if (!global) {
                Matcher result = getMatcherOrNull(cx, rx, string);
                if (result == null) {
                    return string;
                }
                matches.add(result);
            } else {
                // cf. RegExp.prototype.match, step 10.a
                Put(cx, rx, "lastIndex", 0, true);
                int n = 0;
                boolean lastMatch = true;
                while (lastMatch) {
                    // Object result = RegExpExec(realm, rx, s);
                    Matcher result = getMatcherOrNull(cx, rx, string);
                    if (result == null) {
                        lastMatch = false;
                    } else {
                        // FIXME: spec issue (bug 1467)
                        if (result.start() == result.end()) {
                            int thisIndex = (int) ToInteger(cx, Get(cx, rx, "lastIndex"));
                            Put(cx, rx, "lastIndex", thisIndex + 1, true);
                        }
                        matches.add(result);
                        n += 1;
                    }
                }
                if (n == 0) {
                    return string;
                }
            }

            if (IsCallable(replaceValue)) {
                Callable fun = (Callable) replaceValue;
                StringBuilder result = new StringBuilder();
                int lastMatch = 0;
                for (Matcher matchResult : matches) {
                    RegExpConstructor.storeLastMatchResult(cx, rx, string, matchResult);
                    Object[] arguments = GetReplaceArguments(rx, matchResult, string);
                    CharSequence replacement = ToString(cx, fun.call(cx, UNDEFINED, arguments));
                    result.append(string, lastMatch, matchResult.start());
                    result.append(replacement);
                    lastMatch = matchResult.end();
                }
                result.append(string, lastMatch, string.length());
                return result.toString();
            } else {
                RegExpConstructor.storeLastMatchResult(cx, rx, string,
                        matches.get(matches.size() - 1));
                String replValue = (String) replaceValue;
                StringBuilder result = new StringBuilder();
                int lastMatch = 0;
                for (MatchResult matchResult : matches) {
                    String replacement = GetReplaceSubstitution(rx, matchResult, replValue, string);
                    result.append(string, lastMatch, matchResult.start());
                    result.append(replacement);
                    lastMatch = matchResult.end();
                }
                result.append(string, lastMatch, string.length());
                return result.toString();
            }
        }

        private static Object[] GetReplaceArguments(RegExpObject rx, MatchResult matchResult,
                String string) {
            int m = matchResult.groupCount();
            Object[] arguments = new Object[m + 3];
            arguments[0] = matchResult.group();
            GroupIterator iterator = newGroupIterator(rx, matchResult);
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
         */
        private static String GetReplaceSubstitution(RegExpObject rx, MatchResult matchResult,
                String replValue, String string) {
            int m = matchResult.groupCount();
            String[] groups = null;
            StringBuilder replacement = new StringBuilder();

            for (int cursor = 0, len = replValue.length(); cursor < len;) {
                char c = replValue.charAt(cursor++);
                if (c == '$' && cursor < len) {
                    c = replValue.charAt(cursor++);
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
                            char d = replValue.charAt(cursor);
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
         * 21.2.4.8 RegExp.prototype.search (S)
         */
        @Function(name = "search", arity = 1)
        public static Object search(ExecutionContext cx, Object thisValue, Object s) {
            /* steps 1-4 */
            RegExpObject rx = thisRegExpValue(cx, thisValue);
            /* steps 5-6 */
            String string = ToFlatString(cx, s);
            /* steps 7-8 */
            Matcher matcher = rx.getRegExpMatcher().matcher(string);
            if (matcher.find()) {
                RegExpConstructor.storeLastMatchResult(cx, rx, string, matcher);
                int result = matcher.start();
                return result;
            }
            return -1;
        }

        /**
         * 21.2.4.10 RegExp.prototype.split (string, limit)
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
            long lim = Type.isUndefined(limit) ? 0x1FFFFFFFFFFFFFL : ToLength(cx, limit);
            /* step 12 */
            int size = s.length();
            /* step 13 */
            int p = 0;
            /* step 14 */
            if (lim == 0) {
                return a;
            }
            /* step 5 */
            Matcher matcher = rx.getRegExpMatcher().matcher(s);
            /* step 15 */
            if (size == 0) {
                if (matcher.find()) {
                    RegExpConstructor.storeLastMatchResult(cx, rx, s, matcher);
                    return a;
                }
                a.defineOwnProperty(cx, "0", new PropertyDescriptor(s, true, true, true));
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
                RegExpConstructor.storeLastMatchResult(cx, rx, s, matcher);
                int e = matcher.end();
                if (e == p) {
                    q = q + 1;
                } else {
                    String t = s.substring(p, lastStart = matcher.start());
                    a.defineOwnProperty(cx, ToString(lengthA), new PropertyDescriptor(t, true,
                            true, true));
                    lengthA += 1;
                    if (lengthA == lim) {
                        return a;
                    }
                    p = e;
                    GroupIterator iterator = newGroupIterator(rx, matcher);
                    while (iterator.hasNext()) {
                        String cap = iterator.next();
                        a.defineOwnProperty(cx, ToString(lengthA), new PropertyDescriptor(
                                (cap != null ? cap : UNDEFINED), true, true, true));
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
            a.defineOwnProperty(cx, ToString(lengthA), new PropertyDescriptor(t, true, true, true));
            /* step 21 */
            return a;
        }

        /**
         * 21.2.4.15 RegExp.prototype.@@isRegExp
         */
        @Value(name = "@@isRegExp", symbol = BuiltinSymbol.isRegExp, attributes = @Attributes(
                writable = false, enumerable = false, configurable = true))
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
         */
        @Function(name = "compile", arity = 2)
        public static Object compile(ExecutionContext cx, Object thisValue, Object pattern,
                Object flags) {
            /* step 1 (omitted) */
            /* step 2 */
            if (!(thisValue instanceof RegExpObject)) {
                throw throwTypeError(cx, Messages.Key.IncompatibleObject);
            }
            RegExpObject r = (RegExpObject) thisValue;
            /* step 3 */
            boolean extensible = IsExtensible(cx, r);
            /* step 4 */
            if (!extensible) {
                throw throwTypeError(cx, Messages.Key.NotExtensible);
            }
            Object p, f;
            if (pattern instanceof RegExpObject) {
                /* step 5 */
                RegExpObject rx = (RegExpObject) pattern;
                if (!rx.isInitialised()) {
                    throw throwTypeError(cx, Messages.Key.IncompatibleObject);
                }
                if (!Type.isUndefined(flags)) {
                    throw throwTypeError(cx, Messages.Key.NotUndefined);
                }
                p = rx.getOriginalSource();
                f = rx.getOriginalFlags();
            } else {
                /* step 6 */
                p = pattern;
                f = flags;
            }
            /* step 7 */
            return RegExpInitialise(cx, r, p, f);
        }
    }

    /**
     * Runtime Semantics: RegExpExec Abstract Operation
     */
    public static Object RegExpExec(ExecutionContext cx, RegExpObject r, CharSequence s) {
        /* steps 1-14 */
        Matcher m = getMatcherOrNull(cx, r, s);
        if (m == null) {
            return NULL;
        }
        RegExpConstructor.storeLastMatchResult(cx, r, s, m);
        /* steps 15-25 */
        return toMatchResult(cx, r, s, m);
    }

    /**
     * Runtime Semantics: RegExpExec Abstract Operation (1)
     */
    private static Matcher getMatcherOrNull(ExecutionContext cx, RegExpObject r, CharSequence s) {
        /* step 1 */
        assert r.isInitialised();

        /* step 3 */
        int length = s.length();
        /* step 4 */
        Object lastIndex = Get(cx, r, "lastIndex");
        /* steps 5-6 */
        double i = ToInteger(cx, lastIndex);
        /* steps 7-8 */
        boolean global = ToBoolean(Get(cx, r, "global"));
        boolean sticky = ToBoolean(Get(cx, r, "sticky"));
        /* step 9 */
        if (!global && !sticky) {
            i = 0;
        }
        /* step 12.a */
        if (i < 0 || i > length) {
            Put(cx, r, "lastIndex", 0, true);
            return null;
        }
        /* step 10 */
        Pattern matcher = r.getRegExpMatcher();
        /* steps 11-12 */
        Matcher m = matcher.matcher(s);
        if (!sticky) {
            boolean matchSucceeded = m.find((int) i);
            if (!matchSucceeded) {
                Put(cx, r, "lastIndex", 0, true);
                return null;
            }
            /* step 13 */
            int e = m.end();
            /* step 14 */
            if (global) {
                Put(cx, r, "lastIndex", e, true);
            }
            return m;
        } else {
            m.region((int) i, m.regionEnd());
            boolean matchSucceeded = m.lookingAt();
            if (!matchSucceeded) {
                if (global) {
                    Put(cx, r, "lastIndex", 0, true);
                }
                return null;
            }
            int e = m.end();
            Put(cx, r, "lastIndex", e, true);
            return m;
        }
    }

    /**
     * Runtime Semantics: RegExpExec Abstract Operation (2)
     */
    private static ScriptObject toMatchResult(ExecutionContext cx, RegExpObject r, CharSequence s,
            Matcher m) {
        assert r.isInitialised();
        /* step 13 */
        int e = m.end();
        /* step 15 */
        int n = m.groupCount();
        /* step 17 */
        int matchIndex = m.start();

        /* step 16 */
        ScriptObject array = ArrayCreate(cx, 0);
        /* steps 18-21 */
        array.defineOwnProperty(cx, "index", new PropertyDescriptor(matchIndex, true, true, true));
        array.defineOwnProperty(cx, "input", new PropertyDescriptor(s, true, true, true));
        array.defineOwnProperty(cx, "length", new PropertyDescriptor(n + 1));

        /* step 22 */
        CharSequence matchedSubstr = s.subSequence(matchIndex, e);
        /* step 23 */
        array.defineOwnProperty(cx, "0", new PropertyDescriptor(matchedSubstr, true, true, true));
        /* step 24 */
        GroupIterator iterator = newGroupIterator(r, m);
        for (int i = 1; iterator.hasNext(); ++i) {
            String capture = iterator.next();
            array.defineOwnProperty(cx, ToString(i), new PropertyDescriptor(
                    (capture != null ? capture : UNDEFINED), true, true, true));
        }
        /* step 25 */
        return array;
    }

    /**
     * Returns the filtered capturing groups of the {@link MatchResult} argument
     */
    public static String[] groups(RegExpObject r, MatchResult m) {
        assert r.isInitialised();
        GroupIterator iterator = newGroupIterator(r, m);
        int c = m.groupCount();
        String[] groups = new String[c + 1];
        groups[0] = m.group();
        for (int i = 1; iterator.hasNext(); ++i) {
            groups[i] = iterator.next();
        }
        return groups;
    }

    private static GroupIterator newGroupIterator(RegExpObject r, MatchResult m) {
        assert r.isInitialised();
        return new GroupIterator(m, r.getNegativeLookaheadGroups());
    }

    private static final class GroupIterator implements Iterator<String> {
        private final MatchResult result;
        private final BitSet negativeLAGroups;
        private int group = 1;
        // start index of last valid group in matched string
        private int last;

        GroupIterator(MatchResult result, BitSet negativeLAGroups) {
            this.result = result;
            this.negativeLAGroups = negativeLAGroups;
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
