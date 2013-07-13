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
import static com.github.anba.es6draft.runtime.internal.Strings.isLineTerminator;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.OrdinaryConstruct;

import java.util.BitSet;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.parser.RegExpParser;
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
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.10 RegExp (Regular Expression) Objects</h2>
 * <ul>
 * <li>15.10.3 The RegExp Constructor Called as a Function
 * <li>15.10.4 The RegExp Constructor
 * <li>15.10.5 Properties of the RegExp Constructor
 * </ul>
 */
public class RegExpConstructor extends BuiltinFunction implements Constructor, Initialisable {
    public RegExpConstructor(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        createProperties(this, cx, RegExpStatics.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    /**
     * 15.10.3.1 RegExp(pattern, flags)
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object pattern = args.length > 0 ? args[0] : UNDEFINED;
        Object flags = args.length > 1 ? args[1] : UNDEFINED;

        RegExpObject obj;
        if (!Type.isObject(thisValue) || !(thisValue instanceof RegExpObject)
                || ((RegExpObject) thisValue).isInitialised()) {
            if (Type.isObject(pattern) && pattern instanceof RegExpObject
                    && Type.isUndefined(flags)) {
                return pattern;
            }
            obj = RegExpAllocate(calleeContext, this);
        } else {
            obj = (RegExpObject) thisValue;
        }

        Object p, f;
        if (Type.isObject(pattern) && pattern instanceof RegExpObject) {
            RegExpObject regexp = (RegExpObject) pattern;
            if (!regexp.isInitialised()) {
                throwTypeError(calleeContext, Messages.Key.RegExpNotInitialised);
            }
            if (!Type.isUndefined(flags)) {
                throw throwTypeError(calleeContext, Messages.Key.NotUndefined);
            }
            p = regexp.getOriginalSource();
            f = regexp.getOriginalFlags();
        } else {
            p = pattern;
            f = flags;
        }

        return RegExpInitialize(calleeContext, obj, p, f);
    }

    /**
     * 15.10.4.1 new RegExp(pattern, flags)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return OrdinaryConstruct(callerContext, this, args);
    }

    private static class RegExpObjectAllocator implements ObjectAllocator<RegExpObject> {
        static final ObjectAllocator<RegExpObject> INSTANCE = new RegExpObjectAllocator();

        @Override
        public RegExpObject newInstance(Realm realm) {
            return new RegExpObject(realm);
        }
    }

    /**
     * Runtime Semantics: RegExpAlloc Abstract Operation
     */
    public static RegExpObject RegExpAllocate(ExecutionContext cx, Object constructor) {
        RegExpObject obj = OrdinaryCreateFromConstructor(cx, constructor,
                Intrinsics.RegExpPrototype, RegExpObjectAllocator.INSTANCE);
        DefinePropertyOrThrow(cx, obj, "lastIndex", new PropertyDescriptor(UNDEFINED, true, false,
                false));
        return obj;
    }

    /**
     * Runtime Semantics: RegExpInitialize Abstract Operation
     */
    public static RegExpObject RegExpInitialize(ExecutionContext cx, RegExpObject obj,
            Object pattern, Object flags) {
        String p = (Type.isUndefined(pattern) ? "" : ToFlatString(cx, pattern));
        String f = (Type.isUndefined(flags) ? "" : ToFlatString(cx, flags));

        if (getRegExp(cx).isDefaultMultiline() && f.indexOf('m') == -1) {
            f = f + 'm';
        }

        Pattern match;
        BitSet negativeLAGroups;
        try {
            RegExpParser parser = RegExpParser.parse(p, f, -1, -1);
            match = parser.getPattern();
            negativeLAGroups = parser.getNegativeLookaheadGroups();
            // System.out.printf("pattern = '%s'\n", match.pattern());
        } catch (ParserException e) {
            throw e.toScriptException(cx);
        }

        obj.initialise(p, f, match, negativeLAGroups);

        Put(cx, obj, "lastIndex", 0, true);

        return obj;
    }

    /**
     * Runtime Semantics: RegExpCreate Abstract Operation
     */
    public static RegExpObject RegExpCreate(ExecutionContext cx, Object pattern, Object flags) {
        RegExpObject obj = RegExpAllocate(cx, cx.getIntrinsic(Intrinsics.RegExp));
        return RegExpInitialize(cx, obj, pattern, flags);
    }

    /**
     * Runtime Semantics: EscapeRegExpPattern Abstract Operation
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
     * 15.10.5 Properties of the RegExp Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final int length = 2;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final String name = "RegExp";

        /**
         * 15.10.5.1 RegExp.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.RegExpPrototype;

        /**
         * 15.9.4.5 RegExp[ @@create ] ( )
         */
        @Function(name = "@@create", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return RegExpAllocate(cx, thisValue);
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
         */
        @Accessor(name = "$*", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = false))
        public static Object get_$multiline(ExecutionContext cx, Object thisValue) {
            return get_multiline(cx, thisValue);
        }

        /**
         * Extension: RegExp.$*
         */
        @Accessor(name = "$*", type = Accessor.Type.Setter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = false))
        public static Object set_$multiline(ExecutionContext cx, Object thisValue, Object value) {
            return set_multiline(cx, thisValue, value);
        }

        /**
         * Extension: RegExp.multiline
         */
        @Accessor(name = "multiline", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = false))
        public static Object get_multiline(ExecutionContext cx, Object thisValue) {
            return getRegExp(cx).isDefaultMultiline();
        }

        /**
         * Extension: RegExp.multiline
         */
        @Accessor(name = "multiline", type = Accessor.Type.Setter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = false))
        public static Object set_multiline(ExecutionContext cx, Object thisValue, Object value) {
            getRegExp(cx).setDefaultMultiline(ToBoolean(value));
            return UNDEFINED;
        }

        /**
         * Extension: RegExp.$_
         */
        @Accessor(name = "$_", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = false))
        public static Object $input(ExecutionContext cx, Object thisValue) {
            return input(cx, thisValue);
        }

        /**
         * Extension: RegExp.input
         */
        @Accessor(name = "input", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = false))
        public static Object input(ExecutionContext cx, Object thisValue) {
            return getRegExp(cx).getLastInput();
        }

        /**
         * Extension: RegExp.$&
         */
        @Accessor(name = "$&", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = false))
        public static Object $lastMatch(ExecutionContext cx, Object thisValue) {
            return lastMatch(cx, thisValue);
        }

        /**
         * Extension: RegExp.lastMatch
         */
        @Accessor(name = "lastMatch", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = false))
        public static Object lastMatch(ExecutionContext cx, Object thisValue) {
            return getRegExp(cx).getLastMatchResult().group();
        }

        /**
         * Extension: RegExp.$+
         */
        @Accessor(name = "$+", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = false))
        public static Object $lastParen(ExecutionContext cx, Object thisValue) {
            return lastParen(cx, thisValue);
        }

        /**
         * Extension: RegExp.lastParen
         */
        @Accessor(name = "lastParen", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = false))
        public static Object lastParen(ExecutionContext cx, Object thisValue) {
            RegExpConstructor re = getRegExp(cx);
            int groups = re.getLastMatchResult().groupCount();
            return (groups > 0 ? group(re, groups) : "");
        }

        /**
         * Extension: RegExp.$`
         */
        @Accessor(name = "$`", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = false))
        public static Object $leftContext(ExecutionContext cx, Object thisValue) {
            return leftContext(cx, thisValue);
        }

        /**
         * Extension: RegExp.leftContext
         */
        @Accessor(name = "leftContext", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = false))
        public static Object leftContext(ExecutionContext cx, Object thisValue) {
            RegExpConstructor re = getRegExp(cx);
            return re.getLastInput().toString().substring(0, re.getLastMatchResult().start());
        }

        /**
         * Extension: RegExp.$'
         */
        @Accessor(name = "$'", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = false, configurable = false))
        public static Object $rightContext(ExecutionContext cx, Object thisValue) {
            return rightContext(cx, thisValue);
        }

        /**
         * Extension: RegExp.rightContext
         */
        @Accessor(name = "rightContext", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = false))
        public static Object rightContext(ExecutionContext cx, Object thisValue) {
            RegExpConstructor re = getRegExp(cx);
            return re.getLastInput().toString().substring(re.getLastMatchResult().end());
        }

        /**
         * Extension: RegExp.$1
         */
        @Accessor(name = "$1", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = false))
        public static Object $1(ExecutionContext cx, Object thisValue) {
            return group(getRegExp(cx), 1);
        }

        /**
         * Extension: RegExp.$2
         */
        @Accessor(name = "$2", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = false))
        public static Object $2(ExecutionContext cx, Object thisValue) {
            return group(getRegExp(cx), 2);
        }

        /**
         * Extension: RegExp.$3
         */
        @Accessor(name = "$3", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = false))
        public static Object $3(ExecutionContext cx, Object thisValue) {
            return group(getRegExp(cx), 3);
        }

        /**
         * Extension: RegExp.$4
         */
        @Accessor(name = "$4", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = false))
        public static Object $4(ExecutionContext cx, Object thisValue) {
            return group(getRegExp(cx), 4);
        }

        /**
         * Extension: RegExp.$5
         */
        @Accessor(name = "$5", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = false))
        public static Object $5(ExecutionContext cx, Object thisValue) {
            return group(getRegExp(cx), 5);
        }

        /**
         * Extension: RegExp.$6
         */
        @Accessor(name = "$6", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = false))
        public static Object $6(ExecutionContext cx, Object thisValue) {
            return group(getRegExp(cx), 6);
        }

        /**
         * Extension: RegExp.$7
         */
        @Accessor(name = "$7", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = false))
        public static Object $7(ExecutionContext cx, Object thisValue) {
            return group(getRegExp(cx), 7);
        }

        /**
         * Extension: RegExp.$8
         */
        @Accessor(name = "$8", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = false))
        public static Object $8(ExecutionContext cx, Object thisValue) {
            return group(getRegExp(cx), 8);
        }

        /**
         * Extension: RegExp.$9
         */
        @Accessor(name = "$9", type = Accessor.Type.Getter, attributes = @Attributes(
                writable = false, enumerable = true, configurable = false))
        public static Object $9(ExecutionContext cx, Object thisValue) {
            return group(getRegExp(cx), 9);
        }
    }
}
