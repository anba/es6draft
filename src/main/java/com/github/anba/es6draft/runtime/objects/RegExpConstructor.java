/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.DefinePropertyOrThrow;
import static com.github.anba.es6draft.runtime.AbstractOperations.OrdinaryCreateFromConstructor;
import static com.github.anba.es6draft.runtime.AbstractOperations.Put;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.internal.Errors.throwSyntaxError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.internal.Strings.isLineTerminator;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.OrdinaryConstruct;

import java.util.BitSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.parser.RegExpParser;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinBrand;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.10 RegExp (Regular Expression) Objects</h2>
 * <ul>
 * <li>15.10.3 The RegExp Constructor Called as a Function
 * <li>15.10.4 The RegExp Constructor
 * <li>15.10.5 Properties of the RegExp Constructor
 * </ul>
 */
public class RegExpConstructor extends OrdinaryObject implements Scriptable, Callable, Constructor,
        Initialisable {
    public RegExpConstructor(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);
        AddRestrictedFunctionProperties(realm, this);
    }

    /**
     * [[BuiltinBrand]]
     */
    @Override
    public BuiltinBrand getBuiltinBrand() {
        return BuiltinBrand.BuiltinFunction;
    }

    @Override
    public String toSource() {
        return "function RegExp() { /* native code */ }";
    }

    /**
     * 15.10.3.1 RegExp(pattern, flags)
     */
    @Override
    public Object call(Object thisValue, Object... args) {
        Realm realm = realm();
        Object pattern = args.length > 0 ? args[0] : UNDEFINED;
        Object flags = args.length > 1 ? args[1] : UNDEFINED;

        RegExpObject obj;
        if (!Type.isObject(thisValue) || !(thisValue instanceof RegExpObject)
                || ((RegExpObject) thisValue).isInitialised()) {
            if (Type.isObject(pattern) && pattern instanceof RegExpObject
                    && Type.isUndefined(flags)) {
                return pattern;
            }
            obj = RegExpAllocate(realm, this);
        } else {
            obj = (RegExpObject) thisValue;
        }

        String p, f;
        if (Type.isObject(pattern) && pattern instanceof RegExpObject) {
            RegExpObject regexp = (RegExpObject) pattern;
            if (!regexp.isInitialised()) {
                throwTypeError(realm, Messages.Key.RegExpNotInitialised);
            }
            if (!Type.isUndefined(flags)) {
                throw throwTypeError(realm, Messages.Key.NotUndefined);
            }
            p = regexp.getOriginalSource();
            f = regexp.getOriginalFlags();
        } else {
            p = (Type.isUndefined(pattern) ? "" : ToFlatString(realm, pattern));
            f = (Type.isUndefined(flags) ? "" : ToFlatString(realm, flags));
        }

        return RegExpInitialize(realm, obj, p, f);
    }

    /**
     * 15.10.4.1 new RegExp(pattern, flags)
     */
    @Override
    public Object construct(Object... args) {
        return OrdinaryConstruct(realm(), this, args);
    }

    /**
     * Runtime Semantics: RegExpAlloc Abstract Operation
     */
    public static RegExpObject RegExpAllocate(Realm realm, Object constructor) {
        Scriptable obj = OrdinaryCreateFromConstructor(realm, constructor,
                Intrinsics.RegExpPrototype);
        DefinePropertyOrThrow(realm, obj, "lastIndex", new PropertyDescriptor(UNDEFINED, true,
                false, false));
        assert obj instanceof RegExpObject;
        return (RegExpObject) obj;
    }

    /**
     * Runtime Semantics: RegExpInitialize Abstract Operation
     */
    public static RegExpObject RegExpInitialize(Realm realm, RegExpObject obj, String p, String f) {
        // flags :: g | i | m | u | y
        final int global = 0b00001, ignoreCase = 0b00010, multiline = 0b00100, unicode = 0b01000, sticky = 0b10000;
        int flags = 0b00000;
        for (int i = 0, len = f.length(); i < len; ++i) {
            char c = f.charAt(i);
            int flag = (c == 'g' ? global : c == 'i' ? ignoreCase : c == 'm' ? multiline
                    : c == 'u' ? unicode : c == 'y' ? sticky : -1);
            if (flag != -1 && (flags & flag) == 0) {
                flags |= flag;
            } else {
                switch (flag) {
                case global:
                    throw throwSyntaxError(realm, Messages.Key.DuplicateRegExpFlag, "global");
                case ignoreCase:
                    throw throwSyntaxError(realm, Messages.Key.DuplicateRegExpFlag, "ignoreCase");
                case multiline:
                    throw throwSyntaxError(realm, Messages.Key.DuplicateRegExpFlag, "multiline");
                case unicode:
                    throw throwSyntaxError(realm, Messages.Key.DuplicateRegExpFlag, "unicode");
                case sticky:
                    throw throwSyntaxError(realm, Messages.Key.DuplicateRegExpFlag, "sticky");
                default:
                    throw throwSyntaxError(realm, Messages.Key.InvalidRegExpFlag, String.valueOf(c));
                }
            }
        }

        int iflags = 0;
        if ((flags & ignoreCase) != 0) {
            iflags |= Pattern.CASE_INSENSITIVE;
        }
        if ((flags & multiline) != 0) {
            iflags |= Pattern.MULTILINE;
        }

        Pattern match;
        BitSet negativeLAGroups;
        try {
            RegExpParser parser = new RegExpParser(p);
            String regexp = parser.toPattern();
            // System.out.printf("pattern = '%s'\n", regexp);
            match = Pattern.compile(regexp, iflags);
            negativeLAGroups = parser.negativeLookaheadGroups();
        } catch (ParserException | PatternSyntaxException e) {
            throw throwSyntaxError(realm, Messages.Key.InvalidRegExpPattern, e.getMessage());
        }

        obj.initialise(p, f, match, negativeLAGroups);

        // FIXME: spec bug (result from function not used)
        EscapeRegExpPattern(realm, p, f);

        Put(realm, obj, "lastIndex", 0, true);

        return obj;
    }

    /**
     * Runtime Semantics: RegExpCreate Abstract Operation
     */
    public static RegExpObject RegExpCreate(Realm realm, String pattern, String flags) {
        RegExpObject obj = RegExpAllocate(realm, realm.getIntrinsic(Intrinsics.RegExp));
        return RegExpInitialize(realm, obj, pattern, flags);
    }

    /**
     * Runtime Semantics: EscapeRegExpPattern Abstract Operation
     */
    public static String EscapeRegExpPattern(Realm realm, String p, String f) {
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

    public static RegExpObject TestInitialisedOrThrow(Realm realm, RegExpObject regexp) {
        if (!regexp.isInitialised()) {
            throwTypeError(realm, Messages.Key.RegExpNotInitialised);
        }
        return regexp;
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

        /**
         * 15.10.5.1 RegExp.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.RegExpPrototype;

        /**
         * 15.9.4.5 RegExp[ @@create ] ( )
         */
        @Function(
                name = "@@create",
                symbol = BuiltinSymbol.create,
                arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static Object create(Realm realm, Object thisValue) {
            return RegExpAllocate(realm, thisValue);
        }
    }
}
