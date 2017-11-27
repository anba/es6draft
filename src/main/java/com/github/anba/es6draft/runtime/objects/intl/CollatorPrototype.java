/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
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
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BoundFunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>10 Collator Objects</h1>
 * <ul>
 * <li>10.3 Properties of the Intl.Collator Prototype Object
 * </ul>
 */
public final class CollatorPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new Collator prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public CollatorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * 10.3 Properties of the Intl.Collator Prototype Object
     */
    public enum Properties {
        ;

        private static CollatorObject thisCollatorObject(ExecutionContext cx, Object value, String method) {
            if (value instanceof CollatorObject) {
                return (CollatorObject) value;
            }
            throw newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(value).toString());
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 10.3.1 Intl.Collator.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Intl_Collator;

        /**
         * 10.3.2 Intl.Collator.prototype[@@toStringTag]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "Object";

        /**
         * 10.3.3 Intl.Collator.prototype.compare
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the bound compare function
         */
        @Accessor(name = "compare", type = Accessor.Type.Getter)
        public static Object compare(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            CollatorObject collator = thisCollatorObject(cx, thisValue, "Intl.Collator.prototype.compare");
            /* step 4 */
            if (collator.getBoundCompare() == null) {
                /* step 4.a */
                CompareFunction f = new CompareFunction(cx.getRealm());
                /* step 4.b */
                BoundFunctionObject bf = BoundFunctionCreate(cx, f, thisValue);
                /* step 4.c */
                bf.infallibleDefineOwnProperty("length", new Property(2, false, false, true));
                /* step 4.d */
                collator.setBoundCompare(bf);
            }
            /* step 5 */
            return collator.getBoundCompare();
        }

        /**
         * 10.3.5 Intl.Collator.prototype.resolvedOptions ()
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the resolved options object
         */
        @Function(name = "resolvedOptions", arity = 0)
        public static Object resolvedOptions(ExecutionContext cx, Object thisValue) {
            CollatorObject collator = thisCollatorObject(cx, thisValue, "Intl.Collator.prototype.resolvedOptions");
            OrdinaryObject object = ObjectCreate(cx, Intrinsics.ObjectPrototype);
            CreateDataProperty(cx, object, "locale", collator.getLocale());
            CreateDataProperty(cx, object, "usage", collator.getUsage());
            CreateDataProperty(cx, object, "sensitivity", collator.getSensitivity());
            CreateDataProperty(cx, object, "ignorePunctuation", collator.isIgnorePunctuation());
            CreateDataProperty(cx, object, "collation", collator.getCollation());
            CreateDataProperty(cx, object, "numeric", collator.isNumeric());
            CreateDataProperty(cx, object, "caseFirst", collator.getCaseFirst());
            return object;
        }
    }

    /**
     * Abstract Operation: CompareStrings
     * 
     * @param collator
     *            the collator object
     * @param x
     *            the first string
     * @param y
     *            the second string
     * @return the locale specific string comparison result
     */
    public static int CompareStrings(CollatorObject collator, String x, String y) {
        return collator.getCollator().compare(x, y);
    }

    /**
     * 10.3.4 Collator Compare Functions
     */
    public static final class CompareFunction extends BuiltinFunction {
        public CompareFunction(Realm realm) {
            super(realm, "compare", 2);
            createDefaultFunctionProperties();
        }

        @Override
        public Integer call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            /* steps 1-2 */
            assert thisValue instanceof CollatorObject;
            CollatorObject collator = (CollatorObject) thisValue;
            /* step 3 */
            Object x = argument(args, 0);
            /* step 4 */
            Object y = argument(args, 1);
            /* step 5 */
            String sx = ToFlatString(calleeContext, x);
            /* step 6 */
            String sy = ToFlatString(calleeContext, y);
            /* step 7 */
            return CompareStrings(collator, sx, sy);
        }
    }
}
