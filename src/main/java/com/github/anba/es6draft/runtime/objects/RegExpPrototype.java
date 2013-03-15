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
import static com.github.anba.es6draft.runtime.objects.RegExpConstructor.TestInitialisedOrThrow;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArray.ArrayCreate;

import java.util.BitSet;
import java.util.Iterator;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.Scriptable;
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
public class RegExpPrototype extends OrdinaryObject implements Scriptable, Initialisable {
    public RegExpPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);
    }

    /**
     * 15.10.6 Properties of the RegExp Prototype Object
     */
    public enum Properties {
        ;

        private static RegExpObject thisRegExpValue(Realm realm, Object object) {
            if (object instanceof RegExpObject) {
                return TestInitialisedOrThrow(realm, (RegExpObject) object);
            }
            throw throwTypeError(realm, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        @Function(name = "toSource", arity = 0)
        public static Object toSource(Realm realm, Object thisValue) {
            return toString(realm, thisValue);
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
        public static Object exec(Realm realm, Object thisValue, Object string) {
            RegExpObject r = thisRegExpValue(realm, thisValue);
            CharSequence s = ToString(realm, string);
            return RegExpExec(realm, r, s);
        }

        /**
         * 15.10.6.3 get RegExp.prototype.global
         */
        @Accessor(name = "global", type = Accessor.Type.Getter)
        public static Object global(Realm realm, Object thisValue) {
            RegExpObject r = thisRegExpValue(realm, thisValue);
            return r.getOriginalFlags().indexOf('g') != -1;
        }

        /**
         * 15.10.6.4 get RegExp.prototype.ignoreCase
         */
        @Accessor(name = "ignoreCase", type = Accessor.Type.Getter)
        public static Object ignoreCase(Realm realm, Object thisValue) {
            RegExpObject r = thisRegExpValue(realm, thisValue);
            return r.getOriginalFlags().indexOf('i') != -1;
        }

        /**
         * 15.10.6.5 get RegExp.prototype.multiline
         */
        @Accessor(name = "multiline", type = Accessor.Type.Getter)
        public static Object multiline(Realm realm, Object thisValue) {
            RegExpObject r = thisRegExpValue(realm, thisValue);
            return r.getOriginalFlags().indexOf('m') != -1;
        }

        /**
         * 15.10.6.6 get RegExp.prototype.source
         */
        @Accessor(name = "source", type = Accessor.Type.Getter)
        public static Object source(Realm realm, Object thisValue) {
            RegExpObject r = thisRegExpValue(realm, thisValue);
            return EscapeRegExpPattern(realm, r.getOriginalSource(), r.getOriginalFlags());
        }

        /**
         * 15.10.6.7 get RegExp.prototype.sticky
         */
        @Accessor(name = "sticky", type = Accessor.Type.Getter)
        public static Object sticky(Realm realm, Object thisValue) {
            RegExpObject r = thisRegExpValue(realm, thisValue);
            return r.getOriginalFlags().indexOf('y') != -1;
        }

        /**
         * 15.10.6.8 RegExp.prototype.test(string)
         */
        @Function(name = "test", arity = 1)
        public static Object test(Realm realm, Object thisValue, Object string) {
            RegExpObject r = thisRegExpValue(realm, thisValue);
            CharSequence s = ToString(realm, string);
            return getMatcherOrNull(realm, r, s) != null;
        }

        /**
         * 15.10.6.9 get RegExp.prototype.unicode
         */
        @Accessor(name = "unicode", type = Accessor.Type.Getter)
        public static Object unicode(Realm realm, Object thisValue) {
            RegExpObject r = thisRegExpValue(realm, thisValue);
            return r.getOriginalFlags().indexOf('u') != -1;
        }

        /**
         * 15.10.6.10 RegExp.prototype.toString()
         */
        @Function(name = "toString", arity = 0)
        public static Object toString(Realm realm, Object thisValue) {
            RegExpObject r = thisRegExpValue(realm, thisValue);
            CharSequence source = ToString(realm, Get(r, "source"));
            if (source.length() == 0) {
                source = "(?:)";
            }
            StringBuilder sb = new StringBuilder().append('/').append(source).append('/');
            if (ToBoolean(Get(r, "global"))) {
                sb.append('g');
            }
            if (ToBoolean(Get(r, "ignoreCase"))) {
                sb.append('i');
            }
            if (ToBoolean(Get(r, "multiline"))) {
                sb.append('m');
            }
            if (ToBoolean(Get(r, "sticky"))) {
                sb.append('y');
            }
            if (ToBoolean(Get(r, "unicode"))) {
                sb.append('u');
            }
            return sb.toString();
        }

        /**
         * 15.10.6.11 RegExp.prototype.match (string)
         */
        @Function(name = "match", arity = 1)
        public static Object match(Realm realm, Object thisValue, Object string) {
            return UNDEFINED;
        }

        /**
         * 15.10.6.12 RegExp.prototype.replace (S, replaceValue)
         */
        @Function(name = "replace", arity = 2)
        public static Object replace(Realm realm, Object thisValue, Object s, Object replaceValue) {
            return UNDEFINED;
        }

        /**
         * 15.10.4.13 RegExp.prototype.search (S)
         */
        @Function(name = "search", arity = 1)
        public static Object search(Realm realm, Object thisValue, Object s) {
            RegExpObject rx = thisRegExpValue(realm, thisValue);
            String string = ToFlatString(realm, s);
            Matcher matcher = rx.getRegExpMatcher().matcher(string);
            int result = (matcher.find() ? matcher.start() : -1);
            return result;
        }

        /**
         * 15.10.4.14 RegExp.prototype.split (string, limit)
         */
        @Function(name = "split", arity = 2)
        public static Object split(Realm realm, Object thisValue, Object string, Object limit) {
            RegExpObject rx = thisRegExpValue(realm, thisValue);
            String s = ToFlatString(realm, string);
            Scriptable a = ArrayCreate(realm, 0);
            int lengthA = 0;
            long lim = Type.isUndefined(limit) ? 0xFFFFFFFFL : ToUint32(realm, limit);
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
                a.defineOwnProperty("0", new PropertyDescriptor(s, true, true, true));
                return a;
            }
            // Note: omitted index q in the following code
            int lastStart = -1;
            while (matcher.find()) {
                int e = matcher.end();
                if (e != p) {
                    String t = s.substring(p, lastStart = matcher.start());
                    a.defineOwnProperty(ToString(lengthA), new PropertyDescriptor(t, true, true,
                            true));
                    lengthA += 1;
                    if (lengthA == lim) {
                        return a;
                    }
                    p = e;
                    Iterator<Object> iterator = newGroupIterator(rx, matcher);
                    while (iterator.hasNext()) {
                        Object cap = iterator.next();
                        a.defineOwnProperty(ToString(lengthA), new PropertyDescriptor(cap, true,
                                true, true));
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
            a.defineOwnProperty(ToString(lengthA), new PropertyDescriptor(t, true, true, true));
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
        public static Object compile(Realm realm, Object thisValue, Object pattern, Object flags) {
            RegExpObject r = thisRegExpValue(realm, thisValue);
            String p = Type.isUndefined(pattern) ? "" : ToFlatString(realm, pattern);
            String f = Type.isUndefined(flags) ? "" : ToFlatString(realm, flags);
            return RegExpInitialize(realm, r, p, f);
        }
    }

    /**
     * Runtime Semantics: RegExpExec Abstract Operation
     */
    public static Object RegExpExec(Realm realm, RegExpObject r, CharSequence s) {
        assert r.isInitialised();
        Matcher m = getMatcherOrNull(realm, r, s);
        if (m == null) {
            return NULL;
        }
        return toMatchResult(realm, r, s, m);
    }

    /**
     * Runtime Semantics: RegExpExec Abstract Operation (1)
     */
    public static Matcher getMatcherOrNull(Realm realm, RegExpObject r, CharSequence s) {
        assert r.isInitialised();
        Pattern matcher = r.getRegExpMatcher();
        int length = s.length();
        Object lastIndex = Get(r, "lastIndex");
        double i = ToInteger(realm, lastIndex);
        boolean global = ToBoolean(Get(r, "global"));
        if (!global) {
            i = 0;
        }
        if (i < 0 || i > length) {
            Put(realm, r, "lastIndex", 0, true);
            return null;
        }
        Matcher m = matcher.matcher(s);
        boolean matchSucceeded = m.find((int) i);
        if (!matchSucceeded) {
            Put(realm, r, "lastIndex", 0, true);
            return null;
        }
        int e = m.end();
        if (global) {
            Put(realm, r, "lastIndex", e, true);
        }
        return m;
    }

    /**
     * Runtime Semantics: RegExpExec Abstract Operation (2)
     */
    public static Scriptable toMatchResult(Realm realm, RegExpObject r, CharSequence s,
            MatchResult m) {
        assert r.isInitialised();
        int matchIndex = m.start();
        int e = m.end();
        int n = m.groupCount();

        Scriptable array = ArrayCreate(realm, 0);
        array.defineOwnProperty("index", new PropertyDescriptor(matchIndex, true, true, true));
        array.defineOwnProperty("input", new PropertyDescriptor(s, true, true, true));
        array.defineOwnProperty("length", new PropertyDescriptor(n + 1));

        CharSequence matchedSubstr = s.subSequence(matchIndex, e);
        array.defineOwnProperty("0", new PropertyDescriptor(matchedSubstr, true, true, true));
        Iterator<Object> iterator = newGroupIterator(r, m);
        for (int i = 1; iterator.hasNext(); ++i) {
            Object capture = iterator.next();
            array.defineOwnProperty(ToString(i), new PropertyDescriptor(capture, true, true, true));
        }
        return array;
    }

    static Object[] groups(RegExpObject r, MatchResult m) {
        assert r.isInitialised();
        Iterator<Object> iterator = newGroupIterator(r, m);
        int c = m.groupCount();
        Object[] groups = new Object[c + 1];
        groups[0] = m.group();
        for (int i = 1; iterator.hasNext(); ++i) {
            groups[i] = iterator.next();
        }
        return groups;
    }

    static Iterator<Object> newGroupIterator(RegExpObject r, MatchResult m) {
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
