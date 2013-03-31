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
import static com.github.anba.es6draft.runtime.objects.RegExpConstructor.RegExpInitialize;
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
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
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
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.10 RegExp (Regular Expression) Objects</h2>
 * <ul>
 * <li>15.10.6 Properties of the RegExp Prototype Object
 * <li>15.10.7 Properties of RegExp Instances
 * </ul>
 */
public class RegExpPrototype extends OrdinaryObject implements Initialisable {
    public RegExpPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
    }

    /**
     * 15.10.6 Properties of the RegExp Prototype Object
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

        @Function(name = "toSource", arity = 0)
        public static Object toSource(ExecutionContext cx, Object thisValue) {
            return toString(cx, thisValue);
        }

        /**
         * 15.10.6.1 RegExp.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.RegExp;

        /**
         * 15.10.6.2 RegExp.prototype.exec(string)
         */
        @Function(name = "exec", arity = 1)
        public static Object exec(ExecutionContext cx, Object thisValue, Object string) {
            RegExpObject r = thisRegExpValue(cx, thisValue);
            CharSequence s = ToString(cx, string);
            return RegExpExec(cx, r, s);
        }

        /**
         * 15.10.6.3 get RegExp.prototype.global
         */
        @Accessor(name = "global", type = Accessor.Type.Getter)
        public static Object global(ExecutionContext cx, Object thisValue) {
            RegExpObject r = thisRegExpValue(cx, thisValue);
            return r.getOriginalFlags().indexOf('g') != -1;
        }

        /**
         * 15.10.6.4 get RegExp.prototype.ignoreCase
         */
        @Accessor(name = "ignoreCase", type = Accessor.Type.Getter)
        public static Object ignoreCase(ExecutionContext cx, Object thisValue) {
            RegExpObject r = thisRegExpValue(cx, thisValue);
            return r.getOriginalFlags().indexOf('i') != -1;
        }

        /**
         * 15.10.6.5 get RegExp.prototype.multiline
         */
        @Accessor(name = "multiline", type = Accessor.Type.Getter)
        public static Object multiline(ExecutionContext cx, Object thisValue) {
            RegExpObject r = thisRegExpValue(cx, thisValue);
            return r.getOriginalFlags().indexOf('m') != -1;
        }

        /**
         * 15.10.6.6 get RegExp.prototype.source
         */
        @Accessor(name = "source", type = Accessor.Type.Getter)
        public static Object source(ExecutionContext cx, Object thisValue) {
            RegExpObject r = thisRegExpValue(cx, thisValue);
            return EscapeRegExpPattern(r.getOriginalSource(), r.getOriginalFlags());
        }

        /**
         * 15.10.6.7 get RegExp.prototype.sticky
         */
        @Accessor(name = "sticky", type = Accessor.Type.Getter)
        public static Object sticky(ExecutionContext cx, Object thisValue) {
            RegExpObject r = thisRegExpValue(cx, thisValue);
            return r.getOriginalFlags().indexOf('y') != -1;
        }

        /**
         * 15.10.6.8 RegExp.prototype.test(string)
         */
        @Function(name = "test", arity = 1)
        public static Object test(ExecutionContext cx, Object thisValue, Object string) {
            RegExpObject r = thisRegExpValue(cx, thisValue);
            CharSequence s = ToString(cx, string);
            return getMatcherOrNull(cx, r, s) != null;
        }

        /**
         * 15.10.6.9 get RegExp.prototype.unicode
         */
        @Accessor(name = "unicode", type = Accessor.Type.Getter)
        public static Object unicode(ExecutionContext cx, Object thisValue) {
            RegExpObject r = thisRegExpValue(cx, thisValue);
            return r.getOriginalFlags().indexOf('u') != -1;
        }

        /**
         * 15.10.6.10 RegExp.prototype.toString()
         */
        @Function(name = "toString", arity = 0)
        public static Object toString(ExecutionContext cx, Object thisValue) {
            RegExpObject r = thisRegExpValue(cx, thisValue);
            CharSequence source = ToString(cx, Get(cx, r, "source"));
            if (source.length() == 0) {
                source = "(?:)";
            }
            StringBuilder sb = new StringBuilder().append('/').append(source).append('/');
            if (ToBoolean(Get(cx, r, "global"))) {
                sb.append('g');
            }
            if (ToBoolean(Get(cx, r, "ignoreCase"))) {
                sb.append('i');
            }
            if (ToBoolean(Get(cx, r, "multiline"))) {
                sb.append('m');
            }
            if (ToBoolean(Get(cx, r, "sticky"))) {
                sb.append('y');
            }
            if (ToBoolean(Get(cx, r, "unicode"))) {
                sb.append('u');
            }
            return sb.toString();
        }

        /**
         * 15.10.6.11 RegExp.prototype.match (string)
         */
        @Function(name = "match", arity = 1)
        public static Object match(ExecutionContext cx, Object thisValue, Object string) {
            RegExpObject rx = thisRegExpValue(cx, thisValue);
            String s = ToFlatString(cx, string);
            boolean global = ToBoolean(Get(cx, rx, "global"));
            if (!global) {
                return RegExpExec(cx, rx, s);
            } else {
                Put(cx, rx, "lastIndex", 0, true);
                ScriptObject array = ArrayCreate(cx, 0);
                int previousLastIndex = 0;
                int n = 0;
                boolean lastMatch = true;
                while (lastMatch) {
                    // Object result = RegExpExec(realm, rx, s);
                    Matcher result = getMatcherOrNull(cx, rx, s);
                    if (result == null) {
                        lastMatch = false;
                    } else {
                        int thisIndex = (int) ToInteger(cx, Get(cx, rx, "lastIndex"));
                        if (thisIndex == previousLastIndex) {
                            Put(cx, rx, "lastIndex", thisIndex + 1, true);
                            previousLastIndex = thisIndex + 1;
                        } else {
                            previousLastIndex = thisIndex;
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
                return array;
            }
        }

        /**
         * 15.10.6.12 RegExp.prototype.replace (S, replaceValue)
         */
        @Function(name = "replace", arity = 2)
        public static Object replace(ExecutionContext cx, Object thisValue, Object s,
                Object replaceValue) {
            RegExpObject rx = thisRegExpValue(cx, thisValue);
            String string = ToFlatString(cx, s);
            List<MatchResult> matches = new ArrayList<>();
            // cf. RegExp.prototype.match
            boolean global = ToBoolean(Get(cx, rx, "global"));
            if (!global) {
                // cf. RegExpExec
                Object lastIndex = Get(cx, rx, "lastIndex");
                // call ToInteger(realm,) in order to trigger possible side-effects...
                ToInteger(cx, lastIndex);
                Matcher m = rx.getRegExpMatcher().matcher(string);
                boolean matchSucceeded = m.find(0);
                if (!matchSucceeded) {
                    Put(cx, rx, "lastIndex", 0, true);
                    return string;
                }
                matches.add(m.toMatchResult());
            } else {
                // cf. RegExpExec
                Put(cx, rx, "lastIndex", 0, true);
                int previousLastIndex = 0;
                int n = 0;
                boolean lastMatch = true;
                while (lastMatch) {
                    // Object result = RegExpExec(realm, rx, s);
                    Matcher result = getMatcherOrNull(cx, rx, string);
                    if (result == null) {
                        lastMatch = false;
                    } else {
                        int thisIndex = (int) ToInteger(cx, Get(cx, rx, "lastIndex"));
                        if (thisIndex == previousLastIndex) {
                            Put(cx, rx, "lastIndex", thisIndex + 1, true);
                            previousLastIndex = thisIndex + 1;
                        } else {
                            previousLastIndex = thisIndex;
                        }
                        matches.add(result.toMatchResult());
                        n += 1;
                    }
                }
                if (n == 0) {
                    return string;
                }
            }

            if (IsCallable(replaceValue)) {
                StringBuilder result = new StringBuilder();
                int lastMatch = 0;
                Callable fun = (Callable) replaceValue;
                for (MatchResult matchResult : matches) {
                    int m = matchResult.groupCount();
                    Object[] arguments = new Object[m + 3];
                    arguments[0] = matchResult.group();
                    GroupIterator iterator = newGroupIterator(rx, matchResult);
                    for (int i = 1; iterator.hasNext(); ++i) {
                        Object group = iterator.next();
                        arguments[i] = group;
                    }
                    arguments[m + 1] = matchResult.start();
                    arguments[m + 2] = string;

                    CharSequence replacement = ToString(cx, fun.call(cx, UNDEFINED, arguments));
                    result.append(string, lastMatch, matchResult.start());
                    result.append(replacement);
                    lastMatch = matchResult.end();
                }
                result.append(string, lastMatch, string.length());
                return result.toString();
            } else {
                String replValue = ToFlatString(cx, replaceValue);
                StringBuilder result = new StringBuilder();
                int lastMatch = 0;
                for (MatchResult matchResult : matches) {
                    int m = matchResult.groupCount();
                    Object[] groups = null;
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
                                if (n == 0) {
                                    replacement.append("$0");
                                } else {
                                    assert n >= 1 && n <= 99;
                                    if (n <= m) {
                                        if (groups == null) {
                                            groups = RegExpPrototype.groups(rx, matchResult);
                                        }
                                        Object group = groups[n];
                                        if (group != UNDEFINED) {
                                            replacement.append((String) group);
                                        }
                                    } else {
                                        replacement.append('$').append(n);
                                        break;
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
                    result.append(string, lastMatch, matchResult.start());
                    result.append(replacement);
                    lastMatch = matchResult.end();
                }
                result.append(string, lastMatch, string.length());
                return result.toString();
            }
        }

        /**
         * 15.10.4.13 RegExp.prototype.search (S)
         */
        @Function(name = "search", arity = 1)
        public static Object search(ExecutionContext cx, Object thisValue, Object s) {
            RegExpObject rx = thisRegExpValue(cx, thisValue);
            String string = ToFlatString(cx, s);
            Matcher matcher = rx.getRegExpMatcher().matcher(string);
            int result = (matcher.find() ? matcher.start() : -1);
            return result;
        }

        /**
         * 15.10.4.14 RegExp.prototype.split (string, limit)
         */
        @Function(name = "split", arity = 2)
        public static Object split(ExecutionContext cx, Object thisValue, Object string,
                Object limit) {
            RegExpObject rx = thisRegExpValue(cx, thisValue);
            String s = ToFlatString(cx, string);
            ScriptObject a = ArrayCreate(cx, 0);
            int lengthA = 0;
            long lim = Type.isUndefined(limit) ? 0xFFFFFFFFL : ToUint32(cx, limit);
            int size = s.length();
            int p = 0;
            if (lim == 0) {
                return a;
            }
            Matcher matcher = rx.getRegExpMatcher().matcher(s);
            if (size == 0) {
                if (matcher.find()) {
                    return a;
                }
                a.defineOwnProperty(cx, "0", new PropertyDescriptor(s, true, true, true));
                return a;
            }
            // Note: omitted index q in the following code
            int lastStart = -1;
            while (matcher.find()) {
                int e = matcher.end();
                if (e != p) {
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
                        Object cap = iterator.next();
                        a.defineOwnProperty(cx, ToString(lengthA), new PropertyDescriptor(cap,
                                true, true, true));
                        lengthA += 1;
                        if (lengthA == lim) {
                            return a;
                        }
                    }
                }
            }
            if (p == lastStart) {
                return a;
            }
            String t = s.substring(p, size);
            a.defineOwnProperty(cx, ToString(lengthA), new PropertyDescriptor(t, true, true, true));
            return a;
        }

        /**
         * 15.10.4.14 RegExp.prototype.@@isRegExp
         */
        @Value(name = "@@isRegExp", symbol = BuiltinSymbol.isRegExp)
        public static final boolean isRegExp = true;

        /**
         * RegExp.prototype.compile(pattern, flags)
         */
        @Function(name = "compile", arity = 2)
        public static Object compile(ExecutionContext cx, Object thisValue, Object pattern,
                Object flags) {
            RegExpObject r = thisRegExpValue(cx, thisValue);
            String p = Type.isUndefined(pattern) ? "" : ToFlatString(cx, pattern);
            String f = Type.isUndefined(flags) ? "" : ToFlatString(cx, flags);
            return RegExpInitialize(cx, r, p, f);
        }
    }

    /**
     * Runtime Semantics: RegExpExec Abstract Operation
     */
    public static Object RegExpExec(ExecutionContext cx, RegExpObject r, CharSequence s) {
        assert r.isInitialised();
        Matcher m = getMatcherOrNull(cx, r, s);
        if (m == null) {
            return NULL;
        }
        return toMatchResult(cx, r, s, m);
    }

    /**
     * Runtime Semantics: RegExpExec Abstract Operation (1)
     */
    private static Matcher getMatcherOrNull(ExecutionContext cx, RegExpObject r, CharSequence s) {
        assert r.isInitialised();
        Pattern matcher = r.getRegExpMatcher();
        int length = s.length();
        Object lastIndex = Get(cx, r, "lastIndex");
        double i = ToInteger(cx, lastIndex);
        boolean global = ToBoolean(Get(cx, r, "global"));
        if (!global) {
            i = 0;
        }
        if (i < 0 || i > length) {
            Put(cx, r, "lastIndex", 0, true);
            return null;
        }
        Matcher m = matcher.matcher(s);
        boolean matchSucceeded = m.find((int) i);
        if (!matchSucceeded) {
            Put(cx, r, "lastIndex", 0, true);
            return null;
        }
        int e = m.end();
        if (global) {
            Put(cx, r, "lastIndex", e, true);
        }
        return m;
    }

    /**
     * Runtime Semantics: RegExpExec Abstract Operation (2)
     */
    private static ScriptObject toMatchResult(ExecutionContext cx, RegExpObject r, CharSequence s,
            MatchResult m) {
        assert r.isInitialised();
        int matchIndex = m.start();
        int e = m.end();
        int n = m.groupCount();

        ScriptObject array = ArrayCreate(cx, 0);
        array.defineOwnProperty(cx, "index", new PropertyDescriptor(matchIndex, true, true, true));
        array.defineOwnProperty(cx, "input", new PropertyDescriptor(s, true, true, true));
        array.defineOwnProperty(cx, "length", new PropertyDescriptor(n + 1));

        CharSequence matchedSubstr = s.subSequence(matchIndex, e);
        array.defineOwnProperty(cx, "0", new PropertyDescriptor(matchedSubstr, true, true, true));
        GroupIterator iterator = newGroupIterator(r, m);
        for (int i = 1; iterator.hasNext(); ++i) {
            Object capture = iterator.next();
            array.defineOwnProperty(cx, ToString(i), new PropertyDescriptor(capture, true, true,
                    true));
        }
        return array;
    }

    private static Object[] groups(RegExpObject r, MatchResult m) {
        assert r.isInitialised();
        GroupIterator iterator = newGroupIterator(r, m);
        int c = m.groupCount();
        Object[] groups = new Object[c + 1];
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

    private static class GroupIterator implements Iterator<Object> {
        private final MatchResult result;
        private final BitSet negativeLAGroups;
        private int group = 1;
        // start index of last valid group in matched string
        private int last;

        private GroupIterator(MatchResult result, BitSet negativeLAGroups) {
            this.result = result;
            this.negativeLAGroups = negativeLAGroups;
            this.last = result.start();
        }

        @Override
        public boolean hasNext() {
            return group <= result.groupCount();
        }

        @Override
        public Object next() {
            int group = this.group++;
            if (result.start(group) >= last && !negativeLAGroups.get(group)) {
                last = result.start(group);
                Object capture = result.group(group);
                return (capture != null ? capture : UNDEFINED);
            } else {
                return UNDEFINED;
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
