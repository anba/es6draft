/**
 * Copyright (c) 2012-2015 André Bargull
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
import com.github.anba.es6draft.runtime.types.builtins.BoundFunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>10 Collator Objects</h1>
 * <ul>
 * <li>10.3 Properties of the Intl.Collator Prototype Object
 * </ul>
 */
public final class CollatorPrototype extends CollatorObject implements Initializable {
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

        // Initialize Intl.Collator.prototype's internal state.
        CollatorConstructor.InitializeDefaultCollator(realm, this);
    }

    /**
     * 10.3 Properties of the Intl.Collator Prototype Object
     */
    public enum Properties {
        ;

        private static CollatorObject thisCollatorObject(ExecutionContext cx, Object object) {
            if (object instanceof CollatorObject) {
                CollatorObject collator = (CollatorObject) object;
                if (collator.isInitializedCollator()) {
                    return collator;
                }
                throw newTypeError(cx, Messages.Key.UninitializedObject);
            }
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
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
            /* step 1 */
            CollatorObject collator = thisCollatorObject(cx, thisValue);
            /* step 2 */
            if (collator.getBoundCompare() == null) {
                /* step 2.a */
                CompareFunction f = new CompareFunction(cx.getRealm());
                /* step 2.b (not applicable) */
                /* step 2.c */
                BoundFunctionObject bf = BoundFunctionCreate(cx, f, thisValue);
                // FIXME: spec bug - missing define for .length
                bf.infallibleDefineOwnProperty("length", new Property(2, false, false, true));
                // FIXME: spec issue - set .name property?
                /* step 2.d */
                collator.setBoundCompare(bf);
            }
            /* step 3 */
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
            CollatorObject collator = thisCollatorObject(cx, thisValue);
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
     * @param cx
     *            the execution context
     * @param collator
     *            the collator object
     * @param x
     *            the first string
     * @param y
     *            the second string
     * @return the locale specific string comparison result
     */
    public static int CompareStrings(ExecutionContext cx, CollatorObject collator, String x,
            String y) {
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

        private CompareFunction(Realm realm, Void ignore) {
            super(realm, "compare", 2);
        }

        @Override
        public CompareFunction clone() {
            return new CompareFunction(getRealm(), null);
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
            /* steps 5-6 */
            String sx = ToFlatString(calleeContext, x);
            /* steps 7-8 */
            String sy = ToFlatString(calleeContext, y);
            /* step 9 */
            return CompareStrings(calleeContext, collator, sx, sy);
        }
    }
}
