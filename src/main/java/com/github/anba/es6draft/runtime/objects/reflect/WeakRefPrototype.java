/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.reflect;

import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>WeakRef Objects</h1>
 * <ul>
 * <li>Properties of the WeakRef Prototype Object
 * </ul>
 */
public final class WeakRefPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new WeakRef prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public WeakRefPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * Properties of the WeakRef Prototype Object
     */
    public enum Properties {
        ;

        /**
         * Abstract Operation: thisWeakRefObject(value)
         * 
         * @param cx
         *            the execution context
         * @param value
         *            the argument value
         * @param method
         *            the method
         * @return the WeakRef object
         */
        private static WeakRefObject thisWeakRefObject(ExecutionContext cx, Object value, String method) {
            if (value instanceof WeakRefObject) {
                return (WeakRefObject) value;
            }
            throw newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(value).toString());
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * %WeakRefPrototype%.get( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the target object or {@code undefined}
         */
        @Function(name = "get", arity = 0)
        public static Object get(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            WeakRefObject weakRef = thisWeakRefObject(cx, thisValue, "%WeakRefPrototype%.get");
            /* steps 4-7 */
            ScriptObject target = weakRef.getTarget(cx);
            return target != null ? target : UNDEFINED;
        }

        /**
         * %WeakRefPrototype%.clear( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the {@code undefined} value
         */
        @Function(name = "clear", arity = 0)
        public static Object clear(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            WeakRefObject weakRef = thisWeakRefObject(cx, thisValue, "%WeakRefPrototype%.clear");
            /* steps 4-6 */
            weakRef.clear();
            /* step 7 */
            return UNDEFINED;
        }

        /**
         * %WeakRefPrototype% [ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "WeakRef";
    }
}
