/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.text;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsRegExp;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.Collections;
import java.util.Set;

import com.github.anba.es6draft.parser.Characters;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.regexp.MatcherResult;
import com.github.anba.es6draft.regexp.RegExpMatcher;
import com.github.anba.es6draft.regexp.RegExpParser;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.StrBuilder;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;

/**
 * <h1>21 Text Processing</h1><br>
 * <h2>21.2 RegExp (Regular Expression) Objects</h2>
 * <ul>
 * <li>21.2.3 The RegExp Constructor
 * <li>21.2.4 Properties of the RegExp Constructor
 * </ul>
 */
public final class RegExpConstructor extends BuiltinConstructor implements Initializable {
    /**
     * Constructs a new RegExp constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public RegExpConstructor(Realm realm) {
        super(realm, "RegExp", 2);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
        if (realm.getRuntimeContext().isEnabled(CompatibilityOption.RegExpStatics)
                || realm.getRuntimeContext().isEnabled(CompatibilityOption.LegacyRegExp))
            createProperties(realm, this, RegExpStatics.class);
    }

    /**
     * 21.2.3.1 RegExp(pattern, flags)
     */
    @Override
    public ScriptObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object pattern = argument(args, 0);
        Object flags = argument(args, 1);

        /* step 1 */
        boolean patternIsRegExp = IsRegExp(calleeContext, pattern);
        /* step 2 (not applicable) */
        /* step 3 */
        if (patternIsRegExp && Type.isUndefined(flags)) {
            ScriptObject patternObject = Type.objectValue(pattern);
            Object patternConstructor = Get(calleeContext, patternObject, "constructor");
            if (this == patternConstructor) { // SameValue
                return patternObject;
            }
        }
        /* steps 4-8 */
        return RegExpCreate(calleeContext, this, pattern, flags, patternIsRegExp);
    }

    /**
     * 21.2.3.1 RegExp ( pattern, flags )
     */
    @Override
    public RegExpObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object pattern = argument(args, 0);
        Object flags = argument(args, 1);

        /* step 1 */
        boolean patternIsRegExp = IsRegExp(calleeContext, pattern);
        /* step 2 (omitted) */
        /* step 3 (not applicable) */
        /* steps 4-8 */
        return RegExpCreate(calleeContext, newTarget, pattern, flags, patternIsRegExp);
    }

    private static RegExpObject RegExpCreate(ExecutionContext cx, Constructor newTarget, Object pattern, Object flags,
            boolean patternIsRegExp) {
        /* steps 4-6 */
        Object p, f;
        if (pattern instanceof RegExpObject) {
            /* step 4 */
            RegExpObject regexp = (RegExpObject) pattern;
            /* step 4.a */
            p = regexp.getOriginalSource();
            /* steps 4.b-c */
            f = Type.isUndefined(flags) ? regexp.getOriginalFlags() : flags;
        } else if (patternIsRegExp) {
            /* step 5 */
            ScriptObject patternObject = Type.objectValue(pattern);
            /* step 5.a */
            p = Get(cx, patternObject, "source");
            /* steps 5.b-c */
            f = Type.isUndefined(flags) ? Get(cx, patternObject, "flags") : flags;
        } else {
            /* step 6 */
            p = pattern;
            f = flags;
        }
        /* step 7 */
        RegExpObject obj = RegExpAlloc(cx, newTarget);
        /* step 8 */
        return RegExpInitialize(cx, obj, p, f);
    }

    /**
     * 21.2.3.2 Abstract Operations for the RegExp Constructor<br>
     * 21.2.3.2.1 Runtime Semantics: RegExpAlloc ( newTarget )
     * 
     * @param cx
     *            the execution context
     * @param newTarget
     *            the constructor function
     * @return the new regular expression object
     */
    public static RegExpObject RegExpAlloc(ExecutionContext cx, Constructor newTarget) {
        /* steps 1-3 */
        return new RegExpObject(cx.getRealm(), newTarget == cx.getIntrinsic(Intrinsics.RegExp),
                GetPrototypeFromConstructor(cx, newTarget, Intrinsics.RegExpPrototype));
    }

    private static RegExpObject RegExpAlloc(ExecutionContext cx) {
        /* steps 1-3 */
        return new RegExpObject(cx.getRealm(), true, cx.getIntrinsic(Intrinsics.RegExpPrototype));
    }

    /**
     * 21.2.3.2 Abstract Operations for the RegExp Constructor<br>
     * 21.2.3.2.2 Runtime Semantics: RegExpInitialize ( obj, pattern, flags )
     * 
     * @param cx
     *            the execution context
     * @param obj
     *            the RegExp object
     * @param pattern
     *            the regular expression pattern
     * @param flags
     *            the regular expression flags
     * @return the RegExp object
     */
    public static RegExpObject RegExpInitialize(ExecutionContext cx, RegExpObject obj, Object pattern, Object flags) {
        /* steps 1-2 */
        String p = Type.isUndefined(pattern) ? "" : ToFlatString(cx, pattern);
        /* steps 3-4 */
        String f = Type.isUndefined(flags) ? "" : ToFlatString(cx, flags);
        /* steps 5-13 */
        return RegExpInitialize(cx, obj, p, f);
    }

    /**
     * 21.2.3.2 Abstract Operations for the RegExp Constructor<br>
     * 21.2.3.2.2 Runtime Semantics: RegExpInitialize ( obj, pattern, flags )
     * 
     * @param cx
     *            the execution context
     * @param obj
     *            the RegExp object
     * @param pattern
     *            the regular expression pattern
     * @param flags
     *            the regular expression flags
     * @param defaultMultiline
     *            {@code true} to create multiline-mode pattern
     * @return the RegExp object
     */
    private static RegExpObject RegExpInitialize(ExecutionContext cx, RegExpObject obj, String p, String f) {
        /* steps 1-4 (not applicable) */
        /* steps 5-8 */
        RegExpMatcher matcher;
        try {
            matcher = RegExpParser.parse(cx.getRuntimeContext(), p, f, "<regexp>", 1, 1);
        } catch (ParserException e) {
            throw e.toScriptException(cx);
        }
        /* steps 9-11 */
        obj.initialize(p, f, matcher);
        /* step 12 */
        RegExpSetLastIndex(cx, obj, 0);
        /* step 13 */
        return obj;
    }

    private static void RegExpSetLastIndex(ExecutionContext cx, RegExpObject obj, int lastIndex) {
        if (!obj.getLastIndex().isWritable()) {
            throw newTypeError(cx, Messages.Key.PropertyNotModifiable, "lastIndex");
        }
        obj.getLastIndex().setValue(lastIndex);
    }

    /**
     * 21.2.3.2 Abstract Operations for the RegExp Constructor<br>
     * 21.2.3.2.3 Runtime Semantics: RegExpCreate ( P, F )
     * 
     * @param cx
     *            the execution context
     * @param pattern
     *            the regular expression pattern
     * @param flags
     *            the regular expression flags
     * @return the new RegExp object
     */
    public static RegExpObject RegExpCreate(ExecutionContext cx, Object pattern, Object flags) {
        /* step 1 */
        RegExpObject obj = RegExpAlloc(cx);
        /* step 2 */
        return RegExpInitialize(cx, obj, pattern, flags);
    }

    /**
     * 21.2.3.2 Abstract Operations for the RegExp Constructor<br>
     * 21.2.3.2.3 Runtime Semantics: RegExpCreate ( P, F )
     * 
     * @param cx
     *            the execution context
     * @param pattern
     *            the regular expression pattern
     * @param flags
     *            the regular expression flags
     * @return the new RegExp object
     */
    public static RegExpObject RegExpCreate(ExecutionContext cx, String pattern, String flags) {
        /* step 1 */
        RegExpObject obj = RegExpAlloc(cx);
        /* step 2 */
        return RegExpInitialize(cx, obj, pattern, flags);
    }

    /**
     * 21.2.3.2 Abstract Operations for the RegExp Constructor<br>
     * 21.2.3.2.4 Runtime Semantics: EscapeRegExpPattern ( P, F )
     * 
     * @param cx
     *            the execution context
     * @param p
     *            the regular expression pattern
     * @param f
     *            the regular expression flags
     * @return the escaped regular expression pattern
     */
    public static String EscapeRegExpPattern(ExecutionContext cx, String p, String f) {
        if (p.isEmpty()) {
            return "(?:)";
        }
        boolean inClass = false;
        StrBuilder sb = new StrBuilder(cx, p.length());
        for (int i = 0, len = p.length(); i < len; ++i) {
            char c = p.charAt(i);
            if (c == '/' && !inClass) {
                sb.append("\\/");
            } else if (c == '[') {
                inClass = true;
                sb.append(c);
            } else if (c == ']' && inClass) {
                inClass = false;
                sb.append(c);
            } else if (c == '\\') {
                assert i + 1 < len;
                switch (c = p.charAt(++i)) {
                case 0x0A:
                    sb.append("\\n");
                    break;
                case 0x0D:
                    sb.append("\\r");
                    break;
                case 0x2028:
                    sb.append("\\u2028");
                    break;
                case 0x2029:
                    sb.append("\\u2029");
                    break;
                default:
                    sb.append('\\').append(c);
                    break;
                }
            } else if (Characters.isLineTerminator(c)) {
                switch (c) {
                case 0x0A:
                    sb.append("\\n");
                    break;
                case 0x0D:
                    sb.append("\\r");
                    break;
                case 0x2028:
                    sb.append("\\u2028");
                    break;
                case 0x2029:
                    sb.append("\\u2029");
                    break;
                default:
                    throw new AssertionError();
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 21.2.4 Properties of the RegExp Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 2;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = "RegExp";

        /**
         * 21.2.4.1 RegExp.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.RegExpPrototype;

        /**
         * 21.2.4.2 get RegExp [ @@species ]
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the species object
         */
        @Accessor(name = "get [Symbol.species]", symbol = BuiltinSymbol.species, type = Accessor.Type.Getter)
        public static Object species(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            return thisValue;
        }
    }

    /*
     * RegExp statics extensions below this point
     */

    private static final class EmptyMatcherResult implements MatcherResult {
        static final EmptyMatcherResult EMPTY = new EmptyMatcherResult();
        static final EmptyMatcherResult INVALID = new EmptyMatcherResult();

        @Override
        public int start() {
            return 0;
        }

        @Override
        public int start(int group) {
            return 0;
        }

        @Override
        public int end() {
            return 0;
        }

        @Override
        public int end(int group) {
            return 0;
        }

        @Override
        public String group() {
            return "";
        }

        @Override
        public String group(int group) {
            return "";
        }

        @Override
        public int groupCount() {
            return 0;
        }

        @Override
        public CharSequence getInput() {
            return "";
        }

        @Override
        public Set<String> groups() {
            return Collections.emptySet();
        }

        @Override
        public String group(String name) {
            return "";
        }
    }

    private static final class RegExpStaticsHolder {
        private CharSequence lastInput = "";
        private MatcherResult lastMatchResult = EmptyMatcherResult.EMPTY;

        CharSequence getLastInput() {
            return lastInput;
        }

        void setLastInput(CharSequence lastInput) {
            this.lastInput = lastInput;
        }

        MatcherResult getLastMatchResult() {
            return lastMatchResult;
        }

        void storeLastMatchResult(CharSequence input, MatcherResult matchResult) {
            this.lastInput = input;
            this.lastMatchResult = matchResult;
        }

        void invalidateLastMatchResult() {
            this.lastInput = "";
            this.lastMatchResult = EmptyMatcherResult.INVALID;
        }
    }

    private final RegExpStaticsHolder regExpStatics = new RegExpStaticsHolder();

    private static RegExpStaticsHolder getRegExpStatics(ExecutionContext cx) {
        return ((RegExpConstructor) cx.getIntrinsic(Intrinsics.RegExp)).regExpStatics;
    }

    private static RegExpStaticsHolder getRegExpStatics(ExecutionContext cx, Object thisValue, String method) {
        RegExpStaticsHolder statics = getRegExpStatics(cx);
        if (cx.getRuntimeContext().isEnabled(CompatibilityOption.LegacyRegExp)) {
            if (thisValue != cx.getIntrinsic(Intrinsics.RegExp)) {
                throw newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(thisValue).toString());
            }
            if (statics.getLastMatchResult() == EmptyMatcherResult.INVALID) {
                throw newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(thisValue).toString());
            }
        }
        return statics;
    }

    static void storeLastMatchResult(ExecutionContext cx, CharSequence input, MatcherResult matchResult) {
        getRegExpStatics(cx).storeLastMatchResult(input, matchResult);
    }

    static void invalidateLastMatchResult(ExecutionContext cx) {
        getRegExpStatics(cx).invalidateLastMatchResult();
    }

    public enum RegExpStatics {
        ;

        private static String group(RegExpStaticsHolder statics, int groupIndex) {
            assert groupIndex >= 0;
            MatcherResult lastMatch = statics.getLastMatchResult();
            int groupCount = lastMatch.groupCount();
            if (groupIndex == 0 || groupIndex > groupCount) {
                return "";
            }
            String group = lastMatch.group(groupIndex);
            return group != null ? group : "";
        }

        /**
         * Extension: RegExp.$_
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the RegExp.input value
         */
        @Accessor(name = "$_", type = Accessor.Type.Getter,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object get_$input(ExecutionContext cx, Object thisValue) {
            return get_input(cx, thisValue);
        }

        /**
         * Extension: RegExp.$_
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param lastInput
         *            the lastInput string
         * @return the RegExp.input value
         */
        @Accessor(name = "$_", type = Accessor.Type.Setter,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object set_$input(ExecutionContext cx, Object thisValue, Object lastInput) {
            return set_input(cx, thisValue, lastInput);
        }

        /**
         * Extension: RegExp.input
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the RegExp.input value
         */
        @Accessor(name = "input", type = Accessor.Type.Getter,
                attributes = @Attributes(writable = false, enumerable = true, configurable = true))
        public static Object get_input(ExecutionContext cx, Object thisValue) {
            return getRegExpStatics(cx, thisValue, "RegExp.input").getLastInput();
        }

        /**
         * Extension: RegExp.input
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param lastInput
         *            the lastInput string
         * @return the RegExp.input value
         */
        @Accessor(name = "input", type = Accessor.Type.Setter,
                attributes = @Attributes(writable = false, enumerable = true, configurable = true))
        public static Object set_input(ExecutionContext cx, Object thisValue, Object lastInput) {
            getRegExpStatics(cx, thisValue, "RegExp.input").setLastInput(ToString(cx, lastInput));
            return UNDEFINED;
        }

        /**
         * Extension: RegExp.$&amp;
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the RegExp.lastMatch value
         */
        @Accessor(name = "$&", type = Accessor.Type.Getter,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object $lastMatch(ExecutionContext cx, Object thisValue) {
            return lastMatch(cx, thisValue);
        }

        /**
         * Extension: RegExp.lastMatch
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the RegExp.lastMatch value
         */
        @Accessor(name = "lastMatch", type = Accessor.Type.Getter,
                attributes = @Attributes(writable = false, enumerable = true, configurable = true))
        public static Object lastMatch(ExecutionContext cx, Object thisValue) {
            return getRegExpStatics(cx, thisValue, "RegExp.lastMatch").getLastMatchResult().group();
        }

        /**
         * Extension: RegExp.$+
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the RegExp.lastParen value
         */
        @Accessor(name = "$+", type = Accessor.Type.Getter,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object $lastParen(ExecutionContext cx, Object thisValue) {
            return lastParen(cx, thisValue);
        }

        /**
         * Extension: RegExp.lastParen
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the RegExp.lastParen value
         */
        @Accessor(name = "lastParen", type = Accessor.Type.Getter,
                attributes = @Attributes(writable = false, enumerable = true, configurable = true))
        public static Object lastParen(ExecutionContext cx, Object thisValue) {
            RegExpStaticsHolder statics = getRegExpStatics(cx, thisValue, "RegExp.lastParen");
            return group(statics, statics.getLastMatchResult().groupCount());
        }

        /**
         * Extension: RegExp.$`
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the RegExp.leftContext value
         */
        @Accessor(name = "$`", type = Accessor.Type.Getter,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object $leftContext(ExecutionContext cx, Object thisValue) {
            return leftContext(cx, thisValue);
        }

        /**
         * Extension: RegExp.leftContext
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the RegExp.leftContext value
         */
        @Accessor(name = "leftContext", type = Accessor.Type.Getter,
                attributes = @Attributes(writable = false, enumerable = true, configurable = true))
        public static Object leftContext(ExecutionContext cx, Object thisValue) {
            RegExpStaticsHolder statics = getRegExpStatics(cx, thisValue, "RegExp.leftContext");
            MatcherResult matchResult = statics.getLastMatchResult();
            return matchResult.getInput().toString().substring(0, matchResult.start());
        }

        /**
         * Extension: RegExp.$'
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the RegExp.rightContext value
         */
        @Accessor(name = "$'", type = Accessor.Type.Getter,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object $rightContext(ExecutionContext cx, Object thisValue) {
            return rightContext(cx, thisValue);
        }

        /**
         * Extension: RegExp.rightContext
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the RegExp.rightContext value
         */
        @Accessor(name = "rightContext", type = Accessor.Type.Getter,
                attributes = @Attributes(writable = false, enumerable = true, configurable = true))
        public static Object rightContext(ExecutionContext cx, Object thisValue) {
            RegExpStaticsHolder statics = getRegExpStatics(cx, thisValue, "RegExp.rightContext");
            MatcherResult matchResult = statics.getLastMatchResult();
            return matchResult.getInput().toString().substring(matchResult.end());
        }

        /**
         * Extension: RegExp.$1
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the first matched group
         */
        @Accessor(name = "$1", type = Accessor.Type.Getter,
                attributes = @Attributes(writable = false, enumerable = true, configurable = true))
        public static Object $1(ExecutionContext cx, Object thisValue) {
            return group(getRegExpStatics(cx, thisValue, "RegExp.$1"), 1);
        }

        /**
         * Extension: RegExp.$2
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the second matched group
         */
        @Accessor(name = "$2", type = Accessor.Type.Getter,
                attributes = @Attributes(writable = false, enumerable = true, configurable = true))
        public static Object $2(ExecutionContext cx, Object thisValue) {
            return group(getRegExpStatics(cx, thisValue, "RegExp.$2"), 2);
        }

        /**
         * Extension: RegExp.$3
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the third matched group
         */
        @Accessor(name = "$3", type = Accessor.Type.Getter,
                attributes = @Attributes(writable = false, enumerable = true, configurable = true))
        public static Object $3(ExecutionContext cx, Object thisValue) {
            return group(getRegExpStatics(cx, thisValue, "RegExp.$3"), 3);
        }

        /**
         * Extension: RegExp.$4
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the fourth matched group
         */
        @Accessor(name = "$4", type = Accessor.Type.Getter,
                attributes = @Attributes(writable = false, enumerable = true, configurable = true))
        public static Object $4(ExecutionContext cx, Object thisValue) {
            return group(getRegExpStatics(cx, thisValue, "RegExp.$4"), 4);
        }

        /**
         * Extension: RegExp.$5
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the fifth matched group
         */
        @Accessor(name = "$5", type = Accessor.Type.Getter,
                attributes = @Attributes(writable = false, enumerable = true, configurable = true))
        public static Object $5(ExecutionContext cx, Object thisValue) {
            return group(getRegExpStatics(cx, thisValue, "RegExp.$5"), 5);
        }

        /**
         * Extension: RegExp.$6
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the sixth matched group
         */
        @Accessor(name = "$6", type = Accessor.Type.Getter,
                attributes = @Attributes(writable = false, enumerable = true, configurable = true))
        public static Object $6(ExecutionContext cx, Object thisValue) {
            return group(getRegExpStatics(cx, thisValue, "RegExp.$6"), 6);
        }

        /**
         * Extension: RegExp.$7
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the seventh matched group
         */
        @Accessor(name = "$7", type = Accessor.Type.Getter,
                attributes = @Attributes(writable = false, enumerable = true, configurable = true))
        public static Object $7(ExecutionContext cx, Object thisValue) {
            return group(getRegExpStatics(cx, thisValue, "RegExp.$7"), 7);
        }

        /**
         * Extension: RegExp.$8
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the eighth matched group
         */
        @Accessor(name = "$8", type = Accessor.Type.Getter,
                attributes = @Attributes(writable = false, enumerable = true, configurable = true))
        public static Object $8(ExecutionContext cx, Object thisValue) {
            return group(getRegExpStatics(cx, thisValue, "RegExp.$8"), 8);
        }

        /**
         * Extension: RegExp.$9
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the nineth matched group
         */
        @Accessor(name = "$9", type = Accessor.Type.Getter,
                attributes = @Attributes(writable = false, enumerable = true, configurable = true))
        public static Object $9(ExecutionContext cx, Object thisValue) {
            return group(getRegExpStatics(cx, thisValue, "RegExp.$9"), 9);
        }
    }
}
