/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToNumber;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.builtins.BoundFunctionObject.BoundFunctionCreate;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.builtins.BoundFunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>PluralRules Objects</h1>
 * <ul>
 * <li>Properties of the Intl.PluralRules Prototype Object
 * </ul>
 */
public final class PluralRulesPrototype extends PluralRulesObject implements Initializable {
    /**
     * Constructs a new PluralRules prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public PluralRulesPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);

        // Initialize Intl.PluralRules.prototype's internal state.
        PluralRulesConstructor.InitializeDefaultPluralRules(realm, this);
    }

    /**
     * Properties of the Intl.PluralRules Prototype Object
     */
    public enum Properties {
        ;

        private static PluralRulesObject thisPluralRulesObject(ExecutionContext cx, Object object) {
            if (object instanceof PluralRulesObject) {
                return (PluralRulesObject) object;
            }
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * Intl.PluralRules.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Intl_PluralRules;

        /**
         * Intl.PluralRules.prototype[@@toStringTag]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true) )
        public static final String toStringTag = "Object";

        /**
         * get Intl.PluralRules.prototype.select
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the bound compare function
         */
        @Accessor(name = "select", type = Accessor.Type.Getter)
        public static Object select(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            PluralRulesObject pluralRules = thisPluralRulesObject(cx, thisValue);
            /* step 2 */
            if (pluralRules.getBoundResolve() == null) {
                /* step 2.a */
                SelectFunction f = new SelectFunction(cx.getRealm());
                /* step 2.b (not applicable) */
                /* step 2.c */
                BoundFunctionObject bf = BoundFunctionCreate(cx, f, thisValue);
                // FIXME: spec bug - missing define for .length
                bf.infallibleDefineOwnProperty("length", new Property(1, false, false, true));
                // FIXME: spec issue - set .name property?
                /* step 2.d */
                pluralRules.setBoundResolve(bf);
            }
            /* step 3 */
            return pluralRules.getBoundResolve();
        }

        /**
         * Intl.PluralRules.prototype.resolvedOptions ()
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the resolved options object
         */
        @Function(name = "resolvedOptions", arity = 0)
        public static Object resolvedOptions(ExecutionContext cx, Object thisValue) {
            PluralRulesObject pluralRules = thisPluralRulesObject(cx, thisValue);
            OrdinaryObject object = ObjectCreate(cx, Intrinsics.ObjectPrototype);
            CreateDataProperty(cx, object, "locale", pluralRules.getLocale());
            CreateDataProperty(cx, object, "type", pluralRules.getType());
            // TODO: spec issue - undefined pluralCategories property?
            return object;
        }
    }

    /**
     * ResolvePlural (pluralRules, x)
     * 
     * @param pluralRules
     *            the pluralRules object
     * @param x
     *            the number value
     * @return the locale specific plural form
     */
    public static String ResolvePlural(PluralRulesObject pluralRules, double x) {
        return pluralRules.getPluralRules().select(x);
    }

    /**
     * Plural Rules Functions
     */
    public static final class SelectFunction extends BuiltinFunction {
        public SelectFunction(Realm realm) {
            super(realm, "select", 1);
            createDefaultFunctionProperties();
        }

        private SelectFunction(Realm realm, Void ignore) {
            super(realm, "select", 1);
        }

        @Override
        public SelectFunction clone() {
            return new SelectFunction(getRealm(), null);
        }

        @Override
        public String call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            /* steps 1-2 */
            assert thisValue instanceof PluralRulesObject;
            PluralRulesObject pluralRules = (PluralRulesObject) thisValue;
            /* step 3 */
            Object value = argument(args, 0);
            /* step 4 */
            double x = ToNumber(calleeContext, value);
            /* step 5 */
            return ResolvePlural(pluralRules, x);
        }
    }
}
