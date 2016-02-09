/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.simd;

import static com.github.anba.es6draft.runtime.AbstractOperations.Invoke;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.simd.SIMD.ArrayJoin;

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
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>SIMD</h1>
 * <h2>SIMD objects</h2>
 * <ul>
 * <li>Properties of the SIMD.Uint32x4 Prototype Object
 * </ul>
 */
public final class Uint32x4Prototype extends OrdinaryObject implements Initializable {
    private static final SIMDType SIMD_TYPE = SIMDType.Uint32x4;
    private static final int VECTOR_LENGTH = SIMDType.Uint32x4.getVectorLength();

    public Uint32x4Prototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * Properties of the SIMD.Uint32x4 Prototype Object
     */
    public enum Properties {
        ;

        private static SIMDValue thisSIMDValue(ExecutionContext cx, Object obj) {
            if (obj instanceof SIMDValue) {
                return (SIMDValue) obj;
            }
            if (!(obj instanceof SIMDObject)) {
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            SIMDObject object = (SIMDObject) obj;
            if (object.getData().getType() != SIMD_TYPE) {
                throw newTypeError(cx, Messages.Key.SIMDInvalidType);
            }
            return object.getData();
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * SIMDConstructor.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.SIMD_Uint32x4;

        /**
         * SIMDConstructor.prototype.valueOf()
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the SIMD value
         */
        @Function(name = "valueOf", arity = 0)
        public static Object valueOf(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            return thisSIMDValue(cx, thisValue);
        }

        /**
         * SIMDConstructor.prototype.toLocaleString( [ reserved1 [, reserved2 ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param locales
         *            the optional locales array
         * @param options
         *            the optional options object
         * @return the locale specific string representation
         */
        @Function(name = "toLocaleString", arity = 0)
        public static Object toLocaleString(ExecutionContext cx, Object thisValue, Object locales, Object options) {
            /* steps 1-2 */
            SIMDValue value = thisSIMDValue(cx, thisValue);
            /* step 3 */
            // FIXME: spec issue - retrieve list separator from locale?
            String separator = cx.getRealm().getListSeparator();
            /* step 4 */
            CharSequence[] list = new CharSequence[VECTOR_LENGTH];
            /* step 5 */
            for (int i = 0; i < VECTOR_LENGTH; ++i) {
                double element = value.asInt()[i] & 0xffff_ffffL;
                /* steps 5.a-c */
                list[i] = ToString(cx, Invoke(cx, element, "toLocaleString", locales, options));
            }
            /* step 7 */
            String t = SIMD_TYPE.name();
            /* steps 6, 8 */
            // FIXME: spec - add assertion no abrupt completion possible
            String e = ArrayJoin(list, separator);
            /* step 9 */
            // FIXME: spec bug - "SIMD" not prepended.
            return String.format("SIMD.%s(%s)", t, e);
        }

        /**
         * SIMDConstructor.prototype.toString()
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the string representation
         */
        @Function(name = "toString", arity = 0)
        public static Object toString(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            SIMDValue value = thisSIMDValue(cx, thisValue);
            /* step 3 */
            return ToString(cx, value);
        }

        /**
         * SIMDConstructor.prototype [ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true) )
        public static final String toStringTag = "SIMD." + SIMD_TYPE.name();

        /**
         * SIMDConstructor.prototype [ @@toPrimitive ] ( hint )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param hint
         *            the ToPrimitive hint string
         * @return the primitive value for this SIMD object
         */
        @Function(name = "[Symbol.toPrimitive]", arity = 1, symbol = BuiltinSymbol.toPrimitive,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true) )
        public static Object toPrimitive(ExecutionContext cx, Object thisValue, Object hint) {
            /* step 1 (omitted) */
            /* step 2 */
            // FIXME: spec bug? - inconsistent simd type checks when compared to wrapper case.
            if (thisValue instanceof SIMDValue) {
                return thisValue;
            }
            /* steps 3-6 */
            return thisSIMDValue(cx, thisValue);
        }
    }
}
