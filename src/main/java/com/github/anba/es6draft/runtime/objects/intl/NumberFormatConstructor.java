/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateArrayFromList;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsExtensible;
import static com.github.anba.es6draft.runtime.AbstractOperations.OrdinaryCreateFromConstructor;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToObject;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.CanonicalizeLocaleList;
import static com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations.getStringOption;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;

/**
 * <h1>11 NumberFormat Objects</h1>
 * <ul>
 * <li>11.1 The Intl.NumberFormat Constructor
 * <li>11.2 Properties of the Intl.NumberFormat Constructor
 * </ul>
 */
public class NumberFormatConstructor extends BuiltinFunction implements Constructor, Initialisable {
    /**
     * [[availableLocales]]
     */
    private List<Locale> availableLocales = asList(Locale.ENGLISH);

    /**
     * [[relevantExtensionKeys]]
     */
    private List<String> relevantExtensionKeys = asList("nu");

    public NumberFormatConstructor(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    @SafeVarargs
    private static <T> Set<T> set(T... elements) {
        return new HashSet<>(Arrays.asList(elements));
    }

    /**
     * 11.1.1.1 InitializeNumberFormat (numberFormat, locales, options)
     */
    public static void InitializeNumberFormat(ExecutionContext cx, ScriptObject obj,
            Object locales, Object opts) {
        if (!(obj instanceof NumberFormatObject)) {
            throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        NumberFormatObject numberFormat = (NumberFormatObject) obj;
        if (numberFormat.isInitializedIntlObject()) {
            throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        Set<String> requestedLocales = CanonicalizeLocaleList(cx, locales);
        ScriptObject options;
        if (Type.isUndefined(opts)) {
            options = ObjectCreate(cx, Intrinsics.ObjectPrototype);
        } else {
            options = ToObject(cx, opts);
        }
        String matcher = getStringOption(cx, options, "localeMatcher", set("lookup", "best fit"),
                "best fit");

    }

    /**
     * 11.1.2.1 Intl.NumberFormat.call (this [, locales [, options]])
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        Object locales = args.length > 0 ? args[0] : UNDEFINED;
        Object options = args.length > 1 ? args[1] : UNDEFINED;
        if (Type.isUndefined(thisValue) || thisValue == callerContext.getIntrinsic(Intrinsics.Intl)) {
            return construct(callerContext, args);
        }
        ScriptObject obj = ToObject(callerContext, thisValue);
        if (!IsExtensible(callerContext, obj)) {
            throwTypeError(callerContext, Messages.Key.NotExtensible);
        }
        InitializeNumberFormat(callerContext, obj, locales, options);
        return obj;
    }

    /**
     * 11.1.3.1 new Intl.NumberFormat ([locales [, options]])
     */
    @Override
    public Object construct(ExecutionContext callerContext, Object... args) {
        Object locales = args.length > 0 ? args[0] : UNDEFINED;
        Object options = args.length > 1 ? args[1] : UNDEFINED;
        NumberFormatObject obj = new NumberFormatObject(callerContext.getRealm());
        obj.setPrototype(callerContext,
                callerContext.getIntrinsic(Intrinsics.Intl_NumberFormatPrototype));
        InitializeNumberFormat(callerContext, obj, locales, options);
        return obj;
    }

    /**
     * 11.2 Properties of the Intl.NumberFormat Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final int length = 0;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final String name = "NumberFormat";

        /**
         * 11.2.1 Intl.NumberFormat.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.Intl_NumberFormatPrototype;

        /**
         * 11.2.2 Intl.NumberFormat.supportedLocalesOf (locales [, options])
         */
        @Function(name = "supportedLocalesOf", arity = 1)
        public static Object supportedLocalesOf(ExecutionContext cx, Object thisValue,
                Object locales, Object options) {
            return CreateArrayFromList(cx, emptyList());
        }

        /**
         * Extension: Make subclassable for ES6 classes
         */
        @Function(
                name = "@@create",
                symbol = BuiltinSymbol.create,
                arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue,
                    Intrinsics.Intl_NumberFormatPrototype, NumberFormatObjectAllocator.INSTANCE);
        }
    }

    private static class NumberFormatObjectAllocator implements ObjectAllocator<NumberFormatObject> {
        static final ObjectAllocator<NumberFormatObject> INSTANCE = new NumberFormatObjectAllocator();

        @Override
        public NumberFormatObject newInstance(Realm realm) {
            return new NumberFormatObject(realm);
        }
    }
}
