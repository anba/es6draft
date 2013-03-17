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
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Locale;

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
 * <h1>12 DateTimeFormat Objects</h1>
 * <ul>
 * <li>12.1 The Intl.DateTimeFormat Constructor
 * <li>12.2 Properties of the Intl.DateTimeFormat Constructor
 * </ul>
 */
public class DateTimeFormatConstructor extends BuiltinFunction implements Constructor,
        Initialisable {
    /**
     * [[availableLocales]]
     */
    private List<Locale> availableLocales = asList(Locale.ENGLISH);

    /**
     * [[relevantExtensionKeys]]
     */
    private List<String> relevantExtensionKeys = asList("ca", "nu");

    public DateTimeFormatConstructor(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);
        AddRestrictedFunctionProperties(realm, this);
    }

    @Override
    public String toSource() {
        return "function DateTimeFormat() { /* native code */ }";
    }

    /**
     * 12.1.1.1 InitializeDateTimeFormat (dateTimeFormat, locales, options)
     */
    public static void InitializeDateTimeFormat(ScriptObject dateTimeFormat, Object locales,
            Object options) {

    }

    /**
     * 12.1.2.1 Intl.DateTimeFormat.call (this [, locales [, options]])
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
        InitializeDateTimeFormat(obj, locales, options);
        return obj;
    }

    /**
     * 12.1.3.1 new Intl.DateTimeFormat ([locales [, options]])
     */
    @Override
    public Object construct(Object... args) {
        Object locales = args.length > 0 ? args[0] : UNDEFINED;
        Object options = args.length > 1 ? args[1] : UNDEFINED;
        DateTimeFormatObject obj = new DateTimeFormatObject(realm());
        obj.setPrototype(realm().getIntrinsic(Intrinsics.Intl_DateTimeFormatPrototype));
        InitializeDateTimeFormat(obj, locales, options);
        return obj;
    }

    /**
     * 12.2 Properties of the Intl.DateTimeFormat Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final int length = 0;

        /**
         * 12.2.1 Intl.DateTimeFormat.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.Intl_DateTimeFormatPrototype;

        /**
         * 12.2.2 Intl.DateTimeFormat.supportedLocalesOf (locales [, options])
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
                    Intrinsics.Intl_DateTimeFormatPrototype, DateTimeFormatObjectAllocator.INSTANCE);
        }
    }

    private static class DateTimeFormatObjectAllocator implements
            ObjectAllocator<DateTimeFormatObject> {
        static final ObjectAllocator<DateTimeFormatObject> INSTANCE = new DateTimeFormatObjectAllocator();

        @Override
        public DateTimeFormatObject newInstance(Realm realm) {
            return new DateTimeFormatObject(realm);
        }
    }
}
