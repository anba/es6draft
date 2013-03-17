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

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinBrand;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>11 NumberFormat Objects</h1>
 * <ul>
 * <li>11.1 The Intl.NumberFormat Constructor
 * <li>11.2 Properties of the Intl.NumberFormat Constructor
 * </ul>
 */
public class NumberFormatConstructor extends OrdinaryObject implements BuiltinFunction,
        Constructor, Initialisable {
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
        return "function NumberFormat() { /* native code */ }";
    }

    @SafeVarargs
    private static <T> Set<T> set(T... elements) {
        return new HashSet<>(Arrays.asList(elements));
    }

    /**
     * 11.1.1.1 InitializeNumberFormat (numberFormat, locales, options)
     */
    public static void InitializeNumberFormat(Realm realm, ScriptObject obj, Object locales,
            Object opts) {
        if (!(obj instanceof NumberFormatObject)) {
            throwTypeError(realm, Messages.Key.IncompatibleObject);
        }
        NumberFormatObject numberFormat = (NumberFormatObject) obj;
        if (numberFormat.isInitializedIntlObject()) {
            throwTypeError(realm, Messages.Key.IncompatibleObject);
        }
        Set<String> requestedLocales = CanonicalizeLocaleList(realm, locales);
        ScriptObject options;
        if (Type.isUndefined(opts)) {
            options = ObjectCreate(realm, Intrinsics.ObjectPrototype);
        } else {
            options = ToObject(realm, opts);
        }
        String matcher = getStringOption(realm, options, "localeMatcher",
                set("lookup", "best fit"), "best fit");

    }

    /**
     * 11.1.2.1 Intl.NumberFormat.call (this [, locales [, options]])
     */
    @Override
    public Object call(Object thisValue, Object... args) {
        Object locales = args.length > 0 ? args[0] : UNDEFINED;
        Object options = args.length > 1 ? args[1] : UNDEFINED;
        if (Type.isUndefined(thisValue) || thisValue == realm().getIntrinsic(Intrinsics.Intl)) {
            return construct(args);
        }
        ScriptObject obj = ToObject(realm(), thisValue);
        if (!IsExtensible(obj)) {
            throwTypeError(realm(), Messages.Key.NotExtensible);
        }
        InitializeNumberFormat(realm(), obj, locales, options);
        return obj;
    }

    /**
     * 11.1.3.1 new Intl.NumberFormat ([locales [, options]])
     */
    @Override
    public Object construct(Object... args) {
        Object locales = args.length > 0 ? args[0] : UNDEFINED;
        Object options = args.length > 1 ? args[1] : UNDEFINED;
        NumberFormatObject obj = new NumberFormatObject(realm());
        obj.setPrototype(realm().getIntrinsic(Intrinsics.Intl_NumberFormatPrototype));
        InitializeNumberFormat(realm(), obj, locales, options);
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
        public static Object supportedLocalesOf(Realm realm, Object thisValue, Object locales,
                Object options) {
            return CreateArrayFromList(realm, emptyList());
        }

        /**
         * Extension: Make subclassable for ES6 classes
         */
        @Function(name = "@@create", symbol = BuiltinSymbol.create, arity = 0)
        public static Object create(Realm realm, Object thisValue) {
            return OrdinaryCreateFromConstructor(realm, thisValue,
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
