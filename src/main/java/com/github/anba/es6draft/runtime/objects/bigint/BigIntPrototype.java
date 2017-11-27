/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.bigint;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToNumber;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import java.math.BigInteger;

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
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>BigInt</h1><br>
 * <h2>BigInt Objects</h2>
 * <ul>
 * <li>Properties of the BigInt Prototype Object
 * </ul>
 */
public final class BigIntPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new BigInt prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public BigIntPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * Properties of the BigInt Prototype Object
     */
    public enum Properties {
        ;

        /**
         * Abstract operation thisBigIntValue(value)
         * 
         * @param cx
         *            the execution context
         * @param value
         *            the value
         * @param method
         *            the method name
         * @return the BigInt value
         */
        private static BigInteger thisBigIntValue(ExecutionContext cx, Object value, String method) {
            if (Type.isBigInt(value)) {
                return Type.bigIntValue(value);
            }
            if (value instanceof BigIntObject) {
                return ((BigIntObject) value).getBigIntData();
            }
            throw newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(value).toString());
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * BigInt.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.BigInt;

        /**
         * BigInt.prototype.toLocaleString( [ reserved1 [ , reserved2 ] ])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param locales
         *            the optional locales array
         * @param options
         *            the optional options object
         * @return the locale string representation for this number
         */
        @Function(name = "toLocaleString", arity = 0)
        public static Object toLocaleString(ExecutionContext cx, Object thisValue, Object locales, Object options) {
            return toString(cx, thisValue, 10);
        }

        /**
         * BigInt.prototype.toString ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param radix
         *            the optional radix value
         * @return the string representation for this BigInt
         */
        @Function(name = "toString", arity = 0)
        public static Object toString(ExecutionContext cx, Object thisValue, Object radix) {
            /* step 1 */
            BigInteger x = thisBigIntValue(cx, thisValue, "BigInt.prototype.toString");
            /* steps 2-4 */
            int radixNumber = 10;
            if (!Type.isUndefined(radix)) {
                radixNumber = (int) ToNumber(cx, radix); // ToInteger
            }
            /* step 5 */
            if (radixNumber < 2 || radixNumber > 36) {
                throw newRangeError(cx, Messages.Key.InvalidRadix);
            }
            /* step 6 */
            if (radixNumber == 10) {
                return x.toString();
            }
            /* step 7 */
            return x.toString(radixNumber);
        }

        /**
         * BigInt.prototype.valueOf ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the BigInt value of this BigInt object
         */
        @Function(name = "valueOf", arity = 0)
        public static Object valueOf(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            return thisBigIntValue(cx, thisValue, "BigInt.prototype.valueOf");
        }

        /**
         * BigInt.prototype[@@toStringTag]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "BigInt";
    }
}
