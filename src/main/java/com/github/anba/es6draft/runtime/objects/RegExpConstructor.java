/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.OrdinaryCreateFromConstructor;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.internal.Errors.throwSyntaxError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.internal.Strings.isLineTerminator;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;

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
import com.github.anba.es6draft.runtime.types.Property;
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
        Object pattern = args.length > 0 ? args[0] : UNDEFINED;
        Object flags = args.length > 1 ? args[1] : UNDEFINED;

        // TODO: custom extension, not in draft spec!
        if (thisValue instanceof RegExpObject) {
            RegExpObject rx = TestNotInitialisedOrThrow(realm(), (RegExpObject) thisValue);
            return RegExpCreate(realm(), rx, pattern, flags);
        }

        /* step 1 */
        if (Type.isObject(pattern)
                && Type.objectValue(pattern).getBuiltinBrand() == BuiltinBrand.BuiltinRegExp
                && Type.isUndefined(flags)) {
            return pattern;
        }
        /* step 2 */
        return RegExpCreate(realm(), pattern, flags);
    }

    /**
     * 15.10.4.1 new RegExp(pattern, flags)
     */
    @Override
    public Object construct(Object... args) {
        Object pattern = args.length > 0 ? args[0] : UNDEFINED;
        Object flags = args.length > 1 ? args[1] : UNDEFINED;
        /* step 1 */
        return RegExpCreate(realm(), pattern, flags);
    }

    public static RegExpObject TestNotInitialisedOrThrow(Realm realm, RegExpObject regexp) {
        if (regexp.isInitialised()) {
            throwTypeError(realm, Messages.Key.RegExpAlreadyInitialised);
        }
        return regexp;
    }

    public static RegExpObject TestInitialisedOrThrow(Realm realm, RegExpObject regexp) {
        if (!regexp.isInitialised()) {
            throwTypeError(realm, Messages.Key.RegExpNotInitialised);
        }
        return regexp;
    }

    public static RegExpObject RegExpCreate(Realm realm, Object pattern, Object flags) {
        return RegExpCreate(realm, null, pattern, flags);
    }

    static RegExpObject RegExpCreate(Realm realm, RegExpObject obj, Object pattern, Object flags) {
        String p, f;
        if (Type.isObject(pattern)
                && Type.objectValue(pattern).getBuiltinBrand() == BuiltinBrand.BuiltinRegExp) {
            assert pattern instanceof RegExpObject;
            RegExpObject regexp = TestInitialisedOrThrow(realm, (RegExpObject) pattern);
            if (!Type.isUndefined(flags)) {
                throw throwTypeError(realm, Messages.Key.NotUndefined);
            }
            // FIXME: spec bug (font type for `pattern`) (Bug 1149)
            p = regexp.getPattern();
            f = regexp.getFlags();
        } else {
            p = (Type.isUndefined(pattern) ? "" : ToFlatString(realm, pattern));
            f = (Type.isUndefined(flags) ? "" : ToFlatString(realm, flags));
        }

        // flags :: g | i | m
        boolean global = false, ignoreCase = false, multiline = false;
        for (int i = 0, len = f.length(); i < len; ++i) {
            switch (f.charAt(i)) {
            case 'g':
                if (global) {
                    throw throwSyntaxError(realm, Messages.Key.DuplicateRegExpFlag, "global");
                }
                global = true;
                break;
            case 'i':
                if (ignoreCase) {
                    throw throwSyntaxError(realm, Messages.Key.DuplicateRegExpFlag, "ignoreCase");
                }
                ignoreCase = true;
                break;
            case 'm':
                if (multiline) {
                    throw throwSyntaxError(realm, Messages.Key.DuplicateRegExpFlag, "multiline");
                }
                multiline = true;
                break;
            default:
                throw throwSyntaxError(realm, Messages.Key.InvalidRegExpFlag,
                        String.valueOf(f.charAt(i)));
            }
        }

        int iflags = 0;
        if (ignoreCase) {
            iflags |= Pattern.CASE_INSENSITIVE;
        }
        if (multiline) {
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

        if (obj == null) {
            // standard ECMAScript5 code path
            obj = new RegExpObject(realm, p, f, match, negativeLAGroups);
            obj.setPrototype(realm.getIntrinsic(Intrinsics.RegExpPrototype));
            obj.defineOwnProperty("source", new PropertyDescriptor(s, false, false, false));
            obj.defineOwnProperty("global", new PropertyDescriptor(global, false, false, false));
            obj.defineOwnProperty("ignoreCase", new PropertyDescriptor(ignoreCase, false, false,
                    false));
            obj.defineOwnProperty("multiline", new PropertyDescriptor(multiline, false, false,
                    false));
            obj.defineOwnProperty("lastIndex", new PropertyDescriptor(0, true, false, false));
        } else {
            // TODO: new ECMAScript6 behaviour
            applyOrCreate(obj, "source", new PropertyDescriptor(s, false, false, false));
            applyOrCreate(obj, "global", new PropertyDescriptor(global, false, false, false));
            applyOrCreate(obj, "ignoreCase",
                    new PropertyDescriptor(ignoreCase, false, false, false));
            applyOrCreate(obj, "multiline", new PropertyDescriptor(multiline, false, false, false));
            applyOrCreate(obj, "lastIndex", new PropertyDescriptor(0, true, false, false));

            obj.initialise(p, f, match, negativeLAGroups);
        }

        return obj;
    }

    private static void applyOrCreate(RegExpObject obj, String propertyKey, PropertyDescriptor desc) {
        if (obj.hasOwnProperty(propertyKey)) {
            Property prop = obj.getOwnProperty(propertyKey);
            if (!prop.isDataDescriptor()) {
                // or raise an error?
                prop.toDataProperty();
            }
            if (desc.isWritable() && !prop.isWritable()) {
                // handle 'lastIndex' special case
                desc.setWritable(false);
            }
            prop.apply(desc);
        } else {
            obj.defineOwnProperty(propertyKey, desc);
        }
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
         * TODO: not yet in spec
         */
        @Function(name = "@@create", symbol = BuiltinSymbol.create, arity = 0)
        public static Object create(Realm realm, Object thisValue) {
            Scriptable obj = OrdinaryCreateFromConstructor(realm, thisValue,
                    Intrinsics.RegExpPrototype);
            return obj;
        }
    }
}
