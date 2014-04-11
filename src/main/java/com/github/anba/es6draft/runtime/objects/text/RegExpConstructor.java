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
import static com.github.anba.es6draft.runtime.internal.Strings.isLineTerminator;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.regexp.RegExpMatcher;
import com.github.anba.es6draft.regexp.RegExpParser;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.*;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.CompatibilityExtension;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
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
public final class RegExpConstructor extends BuiltinConstructor implements Initialisable {
    public RegExpConstructor(Realm realm) {
        super(realm, "RegExp");
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
        createProperties(cx, this, RegExpStatics.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    @Override
    public RegExpConstructor clone(ExecutionContext cx) {
        RegExpConstructor f = new RegExpConstructor(getRealm());
        f.setPrototype(getPrototype());
        f.addRestrictedFunctionProperties(cx);
        return f;
    }

    /**
     * 21.2.3.1 RegExp(pattern, flags)
     */
    @Override
    public RegExpObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object pattern = args.length > 0 ? args[0] : UNDEFINED;
        Object flags = args.length > 1 ? args[1] : UNDEFINED;

        /* steps 1-2 (omitted) */
        RegExpObject obj;
        if (!(thisValue instanceof RegExpObject) || ((RegExpObject) thisValue).isInitialised()) {
            /* step 3 */
            if (pattern instanceof RegExpObject && Type.isUndefined(flags)) {
                return (RegExpObject) pattern;
            }
            obj = RegExpAlloc(calleeContext, this);
        } else {
            /* step 4 */
            obj = (RegExpObject) thisValue;
        }

        Object p, f;
        if (pattern instanceof RegExpObject) {
            /* step 4 */
            RegExpObject regexp = (RegExpObject) pattern;
            if (!regexp.isInitialised()) {
                throw newTypeError(calleeContext, Messages.Key.UninitialisedObject);
            }
            if (!Type.isUndefined(flags)) {
                throw newTypeError(calleeContext, Messages.Key.NotUndefined);
            }
            p = regexp.getOriginalSource();
            f = regexp.getOriginalFlags();
        } else {
            /* step 5 */
            p = pattern;
            f = flags;
        }

        /* step 6 */
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
    public static RegExpObject RegExpAlloc(ExecutionContext cx, Object constructor) {
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
        if (getRegExp(cx).isDefaultMultiline() && f.indexOf('m') == -1) {
            f = f + 'm';
        }

        /* steps 7-8 */
        RegExpMatcher matcher;
        try {
            matcher = RegExpParser.parse(p, f, "<regexp>", 1, 1);
        } catch (ParserException e) {
            throw e.toScriptException(cx);
        }
        /* steps 9-11 */
        obj.initialise(p, f, matcher);
        /* steps 12-13 */
        Put(cx, obj, "lastIndex", 0, true);
        /* step 14 */
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
        RegExpObject obj = RegExpAlloc(cx, cx.getIntrinsic(Intrinsics.RegExp));
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
            } else if (isLineTerminator(c)) {
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
         * 21.2.4.2 RegExp[ @@create ] ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the new RegExp object
         */
        @Function(name = "[Symbol.create]", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return RegExpAlloc(cx, thisValue);
        }
    }

    /*
     * RegExp statics extensions below this point
     */

    private static RegExpConstructor getRegExp(ExecutionContext cx) {
        return (RegExpConstructor) cx.getIntrinsic(Intrinsics.RegExp);
    }

    private static final MatchResult EMPTY_MATCH_RESULT;
    static {
        Matcher m = Pattern.compile("").matcher("");
        m.matches();
        EMPTY_MATCH_RESULT = m;
    }

    private boolean defaultMultiline = false;
    private RegExpObject lastRegExpObject;
    private CharSequence lastInput = "";
    private MatchResult lastMatchResult = EMPTY_MATCH_RESULT;

    public boolean isDefaultMultiline() {
        return defaultMultiline;
    }

    public void setDefaultMultiline(boolean defaultMultiline) {
        this.defaultMultiline = defaultMultiline;
    }

    public RegExpObject getLastRegExpObject() {
        return lastRegExpObject;
    }

    public CharSequence getLastInput() {
        return lastInput;
    }

    public MatchResult getLastMatchResult() {
        return lastMatchResult;
    }

    public static void storeLastMatchResult(ExecutionContext cx, RegExpObject rx,
            CharSequence input, MatchResult matchResult) {
        RegExpConstructor re = getRegExp(cx);
        re.lastRegExpObject = rx;
        re.lastInput = input;
        re.lastMatchResult = matchResult;
    }

    @CompatibilityExtension(CompatibilityOption.RegExpStatics)
    public enum RegExpStatics {
        ;

        private static String group(RegExpConstructor re, int group) {
            assert group > 0;
            if (group > re.getLastMatchResult().groupCount()) {
                return "";
            }
            String[] groups = RegExpPrototype.groups(re.getLastRegExpObject(),
                    re.getLastMatchResult());
            return (groups[group] != null ? groups[group] : "");
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
            return getRegExp(cx).isDefaultMultiline();
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
            getRegExp(cx).setDefaultMultiline(ToBoolean(value));
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
            return getRegExp(cx).getLastInput();
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
            return getRegExp(cx).getLastMatchResult().group();
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
            RegExpConstructor re = getRegExp(cx);
            int groups = re.getLastMatchResult().groupCount();
            return (groups > 0 ? group(re, groups) : "");
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
            RegExpConstructor re = getRegExp(cx);
            return re.getLastInput().toString().substring(0, re.getLastMatchResult().start());
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
            RegExpConstructor re = getRegExp(cx);
            return re.getLastInput().toString().substring(re.getLastMatchResult().end());
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
            return group(getRegExp(cx), 1);
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
            return group(getRegExp(cx), 2);
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
            return group(getRegExp(cx), 3);
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
            return group(getRegExp(cx), 4);
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
            return group(getRegExp(cx), 5);
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
            return group(getRegExp(cx), 6);
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
            return group(getRegExp(cx), 7);
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
            return group(getRegExp(cx), 8);
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
            return group(getRegExp(cx), 9);
        }
    }
}
