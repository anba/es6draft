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
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.anba.es6draft.parser.Characters;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.regexp.RegExpMatcher;
import com.github.anba.es6draft.regexp.RegExpParser;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.CompatibilityExtension;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Creatable;
import com.github.anba.es6draft.runtime.types.CreateAction;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
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
public final class RegExpConstructor extends BuiltinConstructor implements Initializable,
        Creatable<RegExpObject> {
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
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
        createProperties(cx, this, RegExpStatics.class);
    }

    @Override
    public RegExpConstructor clone() {
        return new RegExpConstructor(getRealm());
    }

    /**
     * 21.2.3.1 RegExp(pattern, flags)
     */
    @Override
    public ScriptObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object pattern = argument(args, 0);
        Object flags = argument(args, 1);

        /* steps 1-2 (omitted) */
        /* steps 3-4 */
        @SuppressWarnings("unused")
        boolean thisValueIsRegExp = IsRegExp(calleeContext, thisValue);
        /* steps 5-6 */
        boolean patternIsRegExp = IsRegExp(calleeContext, pattern);
        /* step 7 */
        RegExpObject obj;
        if (!(thisValue instanceof RegExpObject) || ((RegExpObject) thisValue).isInitialized()) {
            if (patternIsRegExp && Type.isUndefined(flags)) {
                ScriptObject patternObject = Type.objectValue(pattern);
                Object patternConstructor = Get(calleeContext, patternObject, "constructor");
                if (SameValue(this, patternConstructor)) {
                    return patternObject;
                }
            }
            obj = RegExpAlloc(calleeContext, this);
        } else {
            obj = (RegExpObject) thisValue;
        }

        /* steps 8-10 */
        Object p, f;
        if (pattern instanceof RegExpObject) {
            /* step 8 */
            RegExpObject regexp = (RegExpObject) pattern;
            /* step 8.a */
            if (!regexp.isInitialized()) {
                throw newTypeError(calleeContext, Messages.Key.UninitializedObject);
            }
            /* step 8.b */
            p = regexp.getOriginalSource();
            /* steps 8.c-8.d */
            if (Type.isUndefined(flags)) {
                f = regexp.getOriginalFlags();
            } else {
                f = flags;
            }
        } else if (patternIsRegExp) {
            /* step 9 */
            ScriptObject patternObject = Type.objectValue(pattern);
            p = Get(calleeContext, patternObject, "source");
            f = Get(calleeContext, patternObject, "flags");
        } else {
            /* step 10 */
            p = pattern;
            f = flags;
        }

        /* step 11 */
        return RegExpInitialize(calleeContext, obj, p, f);
    }

    /**
     * 21.2.3.2 new RegExp(...argumentsList)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return Construct(callerContext, this, args);
    }

    private static final class RegExpObjectAllocator implements ObjectAllocator<RegExpObject> {
        static final ObjectAllocator<RegExpObject> INSTANCE = new RegExpObjectAllocator();

        @Override
        public RegExpObject newInstance(Realm realm) {
            return new RegExpObject(realm);
        }
    }

    private static final class RegExpCreate implements CreateAction<RegExpObject> {
        static final CreateAction<RegExpObject> INSTANCE = new RegExpCreate();

        @Override
        public RegExpObject create(ExecutionContext cx, Constructor constructor, Object... args) {
            return RegExpAlloc(cx, constructor);
        }
    }

    @Override
    public CreateAction<RegExpObject> createAction() {
        return RegExpCreate.INSTANCE;
    }

    /**
     * 21.2.3.3 Abstract Operations for the RegExp Constructor<br>
     * 21.2.3.3.1 Runtime Semantics: RegExpAlloc Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param constructor
     *            the constructor function
     * @return the new regular expression object
     */
    public static RegExpObject RegExpAlloc(ExecutionContext cx, Constructor constructor) {
        /* step 1 */
        RegExpObject obj = OrdinaryCreateFromConstructor(cx, constructor,
                Intrinsics.RegExpPrototype, RegExpObjectAllocator.INSTANCE);
        /* steps 2-3 */
        DefinePropertyOrThrow(cx, obj, "lastIndex", new PropertyDescriptor(UNDEFINED, true, false,
                false));
        /* step 4 */
        return obj;
    }

    /**
     * 21.2.3.3 Abstract Operations for the RegExp Constructor<br>
     * 21.2.3.3.2 Runtime Semantics: RegExpInitialize Abstract Operation
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
    public static RegExpObject RegExpInitialize(ExecutionContext cx, RegExpObject obj,
            Object pattern, Object flags) {
        /* steps 1-3 */
        String p = Type.isUndefined(pattern) ? "" : ToFlatString(cx, pattern);
        /* steps 4-6 */
        String f = Type.isUndefined(flags) ? "" : ToFlatString(cx, flags);

        /* RegExp statics extension */
        if (getRegExpStatics(cx).isDefaultMultiline() && f.indexOf('m') == -1) {
            f = f + 'm';
        }

        /* steps 7-10 */
        RegExpMatcher matcher;
        try {
            matcher = RegExpParser.parse(p, f, "<regexp>", 1, 1,
                    cx.getRealm().isEnabled(CompatibilityOption.WebRegularExpressions));
        } catch (ParserException e) {
            throw e.toScriptException(cx);
        }
        /* steps 11-12, 14 */
        obj.initialize(p, f, matcher);
        /* step 13 */
        // FIXME: spec bug - invalid test for initialization, cf. RegExp.prototype.compile
        /* steps 15-16 */
        Put(cx, obj, "lastIndex", 0, true);
        /* step 17 */
        return obj;
    }

    /**
     * 21.2.3.3 Abstract Operations for the RegExp Constructor<br>
     * 21.2.3.3.3 Runtime Semantics: RegExpCreate Abstract Operation
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
        /* steps 1-2 */
        RegExpObject obj = RegExpAlloc(cx, (Constructor) cx.getIntrinsic(Intrinsics.RegExp));
        /* step 3 */
        return RegExpInitialize(cx, obj, pattern, flags);
    }

    /**
     * 21.2.3.3 Abstract Operations for the RegExp Constructor<br>
     * 21.2.3.3.4 Runtime Semantics: EscapeRegExpPattern Abstract Operation
     * 
     * @param p
     *            the regular expression pattern
     * @param f
     *            the regular expression flags
     * @return the escaped regular expression pattern
     */
    public static String EscapeRegExpPattern(String p, String f) {
        StringBuilder sb = new StringBuilder(p.length());
        for (int i = 0, len = p.length(); i < len; ++i) {
            char c = p.charAt(i);
            if (c == '/') {
                sb.append("\\/");
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
                    assert false : "unknown line terminator";
                }
            } else {
                sb.append(c);
            }
        }

        String s = sb.toString();
        if (s.isEmpty()) {
            // web reality vs. spec compliance o_O
            // s = "(?:)";
        }

        return s;
    }

    /**
     * 21.2.4 Properties of the RegExp Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 2;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = "RegExp";

        /**
         * 21.2.4.1 RegExp.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
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
        @Accessor(name = "get [Symbol.species]", symbol = BuiltinSymbol.species,
                type = Accessor.Type.Getter)
        public static Object species(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            return thisValue;
        }
    }

    /*
     * RegExp statics extensions below this point
     */

    private static final class RegExpStaticsHolder {
        private static final MatchResult EMPTY_MATCH_RESULT;
        static {
            Matcher m = Pattern.compile("").matcher("");
            m.matches();
            EMPTY_MATCH_RESULT = m;
        }

        private boolean defaultMultiline = false;
        private CharSequence lastInput = "";
        private MatchResult lastMatchResult = EMPTY_MATCH_RESULT;

        boolean isDefaultMultiline() {
            return defaultMultiline;
        }

        void setDefaultMultiline(boolean defaultMultiline) {
            this.defaultMultiline = defaultMultiline;
        }

        CharSequence getLastInput() {
            return lastInput;
        }

        MatchResult getLastMatchResult() {
            return lastMatchResult;
        }

        void storeLastMatchResult(CharSequence input, MatchResult matchResult) {
            this.lastInput = input;
            this.lastMatchResult = matchResult;
        }
    }

    private RegExpStaticsHolder regExpStatics;

    private static RegExpStaticsHolder getRegExpStatics(ExecutionContext cx) {
        RegExpConstructor re = (RegExpConstructor) cx.getIntrinsic(Intrinsics.RegExp);
        RegExpStaticsHolder statics = re.regExpStatics;
        if (statics == null) {
            re.regExpStatics = statics = new RegExpStaticsHolder();
        }
        return statics;
    }

    static void storeLastMatchResult(ExecutionContext cx, CharSequence input,
            MatchResult matchResult) {
        getRegExpStatics(cx).storeLastMatchResult(input, matchResult);
    }

    @CompatibilityExtension(CompatibilityOption.RegExpStatics)
    public enum RegExpStatics {
        ;

        private static String group(RegExpStaticsHolder statics, int groupIndex) {
            assert groupIndex >= 0;
            if (groupIndex == 0 || groupIndex > statics.getLastMatchResult().groupCount()) {
                return "";
            }
            String[] groups = RegExpPrototype.groups(statics.getLastMatchResult());
            String group = groups[groupIndex - 1];
            return group != null ? group : "";
        }

        /**
         * Extension: RegExp.$*
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the RegExp.multiline value
         */
        @Accessor(name = "$*", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = true))
        public static Object get_$multiline(ExecutionContext cx, Object thisValue) {
            return get_multiline(cx, thisValue);
        }

        /**
         * Extension: RegExp.$*
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the new multiline value
         * @return the undefined value
         */
        @Accessor(name = "$*", type = Accessor.Type.Setter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = true))
        public static Object set_$multiline(ExecutionContext cx, Object thisValue, Object value) {
            return set_multiline(cx, thisValue, value);
        }

        /**
         * Extension: RegExp.multiline
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the RegExp.multiline value
         */
        @Accessor(name = "multiline", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = true))
        public static Object get_multiline(ExecutionContext cx, Object thisValue) {
            return getRegExpStatics(cx).isDefaultMultiline();
        }

        /**
         * Extension: RegExp.multiline
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the new multiline value
         * @return the undefined value
         */
        @Accessor(name = "multiline", type = Accessor.Type.Setter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = true))
        public static Object set_multiline(ExecutionContext cx, Object thisValue, Object value) {
            getRegExpStatics(cx).setDefaultMultiline(ToBoolean(value));
            return UNDEFINED;
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
        @Accessor(name = "$_", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = true))
        public static Object $input(ExecutionContext cx, Object thisValue) {
            return input(cx, thisValue);
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
        @Accessor(name = "input", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = true))
        public static Object input(ExecutionContext cx, Object thisValue) {
            return getRegExpStatics(cx).getLastInput();
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
        @Accessor(name = "$&", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = true))
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
        @Accessor(name = "lastMatch", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = true))
        public static Object lastMatch(ExecutionContext cx, Object thisValue) {
            return getRegExpStatics(cx).getLastMatchResult().group();
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
        @Accessor(name = "$+", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = true))
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
        @Accessor(name = "lastParen", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = true))
        public static Object lastParen(ExecutionContext cx, Object thisValue) {
            RegExpStaticsHolder statics = getRegExpStatics(cx);
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
        @Accessor(name = "$`", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = true))
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
        @Accessor(name = "leftContext", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = true))
        public static Object leftContext(ExecutionContext cx, Object thisValue) {
            RegExpStaticsHolder statics = getRegExpStatics(cx);
            return statics.getLastInput().toString()
                    .substring(0, statics.getLastMatchResult().start());
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
        @Accessor(name = "$'", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = true))
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
        @Accessor(name = "rightContext", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = true))
        public static Object rightContext(ExecutionContext cx, Object thisValue) {
            RegExpStaticsHolder statics = getRegExpStatics(cx);
            return statics.getLastInput().toString().substring(statics.getLastMatchResult().end());
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
        @Accessor(name = "$1", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = true))
        public static Object $1(ExecutionContext cx, Object thisValue) {
            return group(getRegExpStatics(cx), 1);
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
        @Accessor(name = "$2", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = true))
        public static Object $2(ExecutionContext cx, Object thisValue) {
            return group(getRegExpStatics(cx), 2);
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
        @Accessor(name = "$3", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = true))
        public static Object $3(ExecutionContext cx, Object thisValue) {
            return group(getRegExpStatics(cx), 3);
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
        @Accessor(name = "$4", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = true))
        public static Object $4(ExecutionContext cx, Object thisValue) {
            return group(getRegExpStatics(cx), 4);
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
        @Accessor(name = "$5", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = true))
        public static Object $5(ExecutionContext cx, Object thisValue) {
            return group(getRegExpStatics(cx), 5);
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
        @Accessor(name = "$6", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = true))
        public static Object $6(ExecutionContext cx, Object thisValue) {
            return group(getRegExpStatics(cx), 6);
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
        @Accessor(name = "$7", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = true))
        public static Object $7(ExecutionContext cx, Object thisValue) {
            return group(getRegExpStatics(cx), 7);
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
        @Accessor(name = "$8", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = true))
        public static Object $8(ExecutionContext cx, Object thisValue) {
            return group(getRegExpStatics(cx), 8);
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
        @Accessor(name = "$9", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = true))
        public static Object $9(ExecutionContext cx, Object thisValue) {
            return group(getRegExpStatics(cx), 9);
        }
    }
}
