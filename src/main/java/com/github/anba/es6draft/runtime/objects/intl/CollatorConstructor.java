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
 * <h1>10 Collator Objects</h1>
 * <ul>
 * <li>10.1 The Intl.Collator Constructor
 * <li>10.2 Properties of the Intl.Collator Constructor
 * </ul>
 */
public class CollatorConstructor extends BuiltinFunction implements Constructor, Initialisable {
    /**
     * [[availableLocales]]
     */
    private List<Locale> availableLocales = asList(Locale.ENGLISH);

    /**
     * [[relevantExtensionKeys]]
     */
    private List<String> relevantExtensionKeys = asList("co" /* , "kn, "kf" */);

    public CollatorConstructor(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    /**
     * 10.1.1.1 InitializeCollator (collator, locales, options)
     */
    public static void InitializeCollator(ExecutionContext cx, ScriptObject obj, Object locales,
            Object options) {
        if (!(obj instanceof CollatorObject)) {
            throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        CollatorObject collator = (CollatorObject) obj;
        if (collator.isInitialized()) {
            throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        collator.setInitialized(true);

    }

    /**
     * 10.1.2.1 Intl.Collator.call (this [, locales [, options]])
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
        InitializeCollator(callerContext, obj, locales, options);
        return obj;
    }

    /**
     * 10.1.3.1 new Intl.Collator ([locales [, options]])
     */
    @Override
    public Object construct(ExecutionContext callerContext, Object... args) {
        Object locales = args.length > 0 ? args[0] : UNDEFINED;
        Object options = args.length > 1 ? args[1] : UNDEFINED;
        CollatorObject obj = new CollatorObject(callerContext.getRealm());
        obj.setPrototype(callerContext,
                callerContext.getIntrinsic(Intrinsics.Intl_CollatorPrototype));
        InitializeCollator(callerContext, obj, locales, options);
        return obj;
    }

    /**
     * 10.2 Properties of the Intl.Collator Constructor
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
        public static final String name = "Collator";

        /**
         * 10.2.1 Intl.Collator.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.Intl_CollatorPrototype;

        /**
         * 10.2.2 Intl.Collator.supportedLocalesOf (locales [, options])
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
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.Intl_CollatorPrototype,
                    CollatorObjectAllocator.INSTANCE);
        }
    }

    private static class CollatorObjectAllocator implements ObjectAllocator<CollatorObject> {
        static final ObjectAllocator<CollatorObject> INSTANCE = new CollatorObjectAllocator();

        @Override
        public CollatorObject newInstance(Realm realm) {
            return new CollatorObject(realm);
        }
    }
}
