/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.FunctionPrototype;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
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
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);

        // initialize Intl.Collator.prototype's internal state
        CollatorConstructor.InitializeCollator(cx, this, UNDEFINED, UNDEFINED);
    }

    /**
     * 10.3 Properties of the Intl.Collator Prototype Object
     */
    public enum Properties {
        ;

        private static CollatorObject thisCollatorValue(ExecutionContext cx, Object object) {
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
         * 10.3.2 Intl.Collator.prototype.compare
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the bound compare function
         */
        @Accessor(name = "compare", type = Accessor.Type.Getter)
        public static Object compare(ExecutionContext cx, Object thisValue) {
            CollatorObject collator = thisCollatorValue(cx, thisValue);
            if (collator.getBoundCompare() == null) {
                CompareFunction f = new CompareFunction(cx.getRealm());
                Callable bf = (Callable) FunctionPrototype.Properties.bind(cx, f, thisValue);
                collator.setBoundCompare(bf);
            }
            return collator.getBoundCompare();
        }

        /**
         * 10.3.3 Intl.Collator.prototype.resolvedOptions ()
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the resolved options object
         */
        @Function(name = "resolvedOptions", arity = 0)
        public static Object resolvedOptions(ExecutionContext cx, Object thisValue) {
            CollatorObject collator = thisCollatorValue(cx, thisValue);
            OrdinaryObject object = OrdinaryObject.ObjectCreate(cx, Intrinsics.ObjectPrototype);
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

    private static final class CompareFunction extends BuiltinFunction {
        public CompareFunction(Realm realm) {
            super(realm, "compare");
            createDefaultFunctionProperties("compare", 2);
        }

        private CompareFunction(Realm realm, Void ignore) {
            super(realm, "compare");
        }

        @Override
        public CompareFunction clone() {
            return new CompareFunction(getRealm(), null);
        }

        /**
         * [[Call]]
         */
        @Override
        public Integer call(ExecutionContext callerContext, Object thisValue, Object... args) {
            assert thisValue instanceof CollatorObject;
            ExecutionContext calleeContext = calleeContext();
            Object arg0 = args.length > 0 ? args[0] : UNDEFINED;
            Object arg1 = args.length > 1 ? args[1] : UNDEFINED;
            String x = ToFlatString(calleeContext, arg0);
            String y = ToFlatString(calleeContext, arg1);
            return CompareStrings(calleeContext, (CollatorObject) thisValue, x, y);
        }
    }
}
