/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.simd;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToInt32;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToInteger;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.simd.SIMD.*;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.Arrays;

import com.github.anba.es6draft.runtime.AbstractOperations;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.CompatibilityExtension;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;

/**
 * <h1>SIMD</h1>
 * <h2>SIMD objects</h2>
 * <ul>
 * <li>The SIMD.Int32x4 Constructor
 * <li>Properties of the SIMD.Int32x4 Constructor
 * </ul>
 */
public final class Int32x4Constructor extends BuiltinConstructor implements Initializable {
    private static final SIMDType SIMD_TYPE = SIMDType.Int32x4;
    private static final int ELEMENT_SIZE = SIMDType.Int32x4.getElementSize();
    private static final int VECTOR_LENGTH = SIMDType.Int32x4.getVectorLength();
    private static final int MIN_VALUE = Integer.MIN_VALUE;
    private static final int MAX_VALUE = Integer.MAX_VALUE;

    public Int32x4Constructor(Realm realm) {
        super(realm, SIMD_TYPE.name(), VECTOR_LENGTH);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
        createProperties(realm, this, AdditionalProperties.class);
    }

    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object[] fields = new Object[VECTOR_LENGTH];
        for (int i = 0; i < VECTOR_LENGTH; ++i) {
            fields[i] = i < args.length ? args[i] : UNDEFINED;
        }
        return SIMDCreateInt(calleeContext, SIMD_TYPE, fields, AbstractOperations::ToInt32);
    }

    @Override
    public ScriptObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
        throw newTypeError(calleeContext(), Messages.Key.SIMDCreate, SIMD_TYPE.name());
    }

    /**
     * Properties of the SIMD.Int32x4 Constructor
     */
    public enum Properties {
        ;

        private static SIMDValue anySimdValue(ExecutionContext cx, Object value, String method) {
            if (!(value instanceof SIMDValue)) {
                throw newTypeError(cx, Messages.Key.SIMDInvalidObject, SIMD_TYPE.name() + method,
                        Type.of(value).toString());
            }
            return (SIMDValue) value;
        }

        private static SIMDValue simdValue(ExecutionContext cx, Object value, SIMDType type, String method) {
            if (!(value instanceof SIMDValue)) {
                throw newTypeError(cx, Messages.Key.SIMDInvalidObject, SIMD_TYPE.name() + method,
                        Type.of(value).toString());
            }
            if (((SIMDValue) value).getType() != type) {
                throw newTypeError(cx, Messages.Key.SIMDInvalidType);
            }
            return (SIMDValue) value;
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = VECTOR_LENGTH;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = SIMD_TYPE.name();

        // FIXME: spec bug - missing definition
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.SIMD_Int32x4Prototype;

        /**
         * SIMDConstructor.splat(n)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param n
         *            the value
         * @return the new SIMD value
         */
        @Function(name = "splat", arity = 1)
        public static Object splat(ExecutionContext cx, Object thisValue, Object n) {
            /* step 1 */
            Object[] list = new Object[VECTOR_LENGTH];
            Arrays.fill(list, n);
            /* step 2 */
            return SIMDCreateInt(cx, SIMD_TYPE, list, AbstractOperations::ToInt32);
        }

        /**
         * SIMDConstructor.check(a)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param a
         *            the value
         * @return the SIMD value
         */
        @Function(name = "check", arity = 1)
        public static Object check(ExecutionContext cx, Object thisValue, Object a) {
            /* steps 1-2 */
            return simdValue(cx, a, SIMD_TYPE, ".check");
        }

        /**
         * SIMDConstructor.add(a, b)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param a
         *            the first value
         * @param b
         *            the second value
         * @return the new SIMD value
         */
        @Function(name = "add", arity = 2)
        public static Object add(ExecutionContext cx, Object thisValue, Object a, Object b) {
            /* step 1 */
            SIMDValue sa = simdValue(cx, a, SIMD_TYPE, ".add");
            SIMDValue sb = simdValue(cx, b, SIMD_TYPE, ".add");
            /* steps 2-3 */
            SIMDValue result = SIMDBinaryOpInt(sa, sb, (x, y) -> x + y);
            /* step 4 */
            return result;
        }

        /**
         * SIMDConstructor.sub(a, b)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param a
         *            the first value
         * @param b
         *            the second value
         * @return the new SIMD value
         */
        @Function(name = "sub", arity = 2)
        public static Object sub(ExecutionContext cx, Object thisValue, Object a, Object b) {
            /* step 1 */
            SIMDValue sa = simdValue(cx, a, SIMD_TYPE, ".sub");
            SIMDValue sb = simdValue(cx, b, SIMD_TYPE, ".sub");
            /* steps 2-3 */
            SIMDValue result = SIMDBinaryOpInt(sa, sb, (x, y) -> x - y);
            /* step 4 */
            return result;
        }

        /**
         * SIMDConstructor.mul(a, b)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param a
         *            the first value
         * @param b
         *            the second value
         * @return the new SIMD value
         */
        @Function(name = "mul", arity = 2)
        public static Object mul(ExecutionContext cx, Object thisValue, Object a, Object b) {
            /* step 1 */
            SIMDValue sa = simdValue(cx, a, SIMD_TYPE, ".mul");
            SIMDValue sb = simdValue(cx, b, SIMD_TYPE, ".mul");
            /* steps 2-4 */
            SIMDValue result = SIMDBinaryOpInt(sa, sb, (x, y) -> x * y);
            /* step 5 */
            return result;
        }

        /**
         * SIMDConstructor.max(a, b)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param a
         *            the first value
         * @param b
         *            the second value
         * @return the new SIMD value
         */
        // FIXME: spec bug - only spec'ed for floating point, but expected for floating + integer
        // https://github.com/tc39/ecmascript_simd/issues/293
        @Function(name = "max", arity = 2)
        public static Object max(ExecutionContext cx, Object thisValue, Object a, Object b) {
            /* step 1 */
            SIMDValue sa = simdValue(cx, a, SIMD_TYPE, ".max");
            SIMDValue sb = simdValue(cx, b, SIMD_TYPE, ".max");
            /* steps 2-3 */
            SIMDValue result = SIMDBinaryOpInt(sa, sb, Math::max);
            /* step 4 */
            return result;
        }

        /**
         * SIMDConstructor.min(a, b)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param a
         *            the first value
         * @param b
         *            the second value
         * @return the new SIMD value
         */
        // FIXME: spec bug - only spec'ed for floating point, but expected for floating + integer
        // https://github.com/tc39/ecmascript_simd/issues/293
        @Function(name = "min", arity = 2)
        public static Object min(ExecutionContext cx, Object thisValue, Object a, Object b) {
            /* step 1 */
            SIMDValue sa = simdValue(cx, a, SIMD_TYPE, ".min");
            SIMDValue sb = simdValue(cx, b, SIMD_TYPE, ".min");
            /* steps 2-3 */
            SIMDValue result = SIMDBinaryOpInt(sa, sb, Math::min);
            /* step 4 */
            return result;
        }

        /**
         * SIMDConstructor.neg(a)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param a
         *            the value
         * @return the new SIMD value
         */
        @Function(name = "neg", arity = 1)
        public static Object neg(ExecutionContext cx, Object thisValue, Object a) {
            /* step 1 */
            SIMDValue sa = simdValue(cx, a, SIMD_TYPE, ".neg");
            /* steps 2-3 */
            SIMDValue result = SIMDUnaryOpInt(sa, x -> -x);
            /* step 4 */
            return result;
        }

        /**
         * SIMDConstructor.and(a, b)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param a
         *            the first value
         * @param b
         *            the second value
         * @return the new SIMD value
         */
        @Function(name = "and", arity = 2)
        public static Object and(ExecutionContext cx, Object thisValue, Object a, Object b) {
            /* step 1 */
            SIMDValue sa = simdValue(cx, a, SIMD_TYPE, ".and");
            SIMDValue sb = simdValue(cx, b, SIMD_TYPE, ".and");
            /* steps 2-3 */
            SIMDValue result = SIMDBinaryOpInt(sa, sb, (x, y) -> x & y);
            /* step 4 */
            return result;
        }

        /**
         * SIMDConstructor.xor(a, b)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param a
         *            the first value
         * @param b
         *            the second value
         * @return the new SIMD value
         */
        @Function(name = "xor", arity = 2)
        public static Object xor(ExecutionContext cx, Object thisValue, Object a, Object b) {
            /* step 1 */
            SIMDValue sa = simdValue(cx, a, SIMD_TYPE, ".xor");
            SIMDValue sb = simdValue(cx, b, SIMD_TYPE, ".xor");
            /* steps 2-3 */
            SIMDValue result = SIMDBinaryOpInt(sa, sb, (x, y) -> x ^ y);
            /* step 4 */
            return result;
        }

        /**
         * SIMDConstructor.or(a, b)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param a
         *            the first value
         * @param b
         *            the second value
         * @return the new SIMD value
         */
        @Function(name = "or", arity = 2)
        public static Object or(ExecutionContext cx, Object thisValue, Object a, Object b) {
            /* step 1 */
            SIMDValue sa = simdValue(cx, a, SIMD_TYPE, ".or");
            SIMDValue sb = simdValue(cx, b, SIMD_TYPE, ".or");
            /* steps 2-3 */
            SIMDValue result = SIMDBinaryOpInt(sa, sb, (x, y) -> x | y);
            /* step 4 */
            return result;
        }

        /**
         * SIMDConstructor.not(a)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param a
         *            the value
         * @return the new SIMD value
         */
        @Function(name = "not", arity = 1)
        public static Object not(ExecutionContext cx, Object thisValue, Object a) {
            /* step 1 */
            SIMDValue sa = simdValue(cx, a, SIMD_TYPE, ".not");
            /* steps 2-3 */
            SIMDValue result = SIMDUnaryOpInt(sa, x -> ~x);
            /* step 4 */
            return result;
        }

        /**
         * SIMDConstructor.lessThan(a, b)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param a
         *            the first value
         * @param b
         *            the second value
         * @return the new SIMD value
         */
        @Function(name = "lessThan", arity = 2)
        public static Object lessThan(ExecutionContext cx, Object thisValue, Object a, Object b) {
            /* step 1 */
            SIMDValue sa = simdValue(cx, a, SIMD_TYPE, ".lessThan");
            SIMDValue sb = simdValue(cx, b, SIMD_TYPE, ".lessThan");
            /* steps 2-3 */
            SIMDValue result = SIMDRelationalOpInt(sa, sb, (x, y) -> x < y);
            /* step 4 */
            return result;
        }

        /**
         * SIMDConstructor.lessThanOrEqual(a, b)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param a
         *            the first value
         * @param b
         *            the second value
         * @return the new SIMD value
         */
        @Function(name = "lessThanOrEqual", arity = 2)
        public static Object lessThanOrEqual(ExecutionContext cx, Object thisValue, Object a, Object b) {
            /* step 1 */
            SIMDValue sa = simdValue(cx, a, SIMD_TYPE, ".lessThanOrEqual");
            SIMDValue sb = simdValue(cx, b, SIMD_TYPE, ".lessThanOrEqual");
            /* steps 2-3 */
            SIMDValue result = SIMDRelationalOpInt(sa, sb, (x, y) -> x <= y);
            /* step 4 */
            return result;
        }

        /**
         * SIMDConstructor.greaterThan(a, b)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param a
         *            the first value
         * @param b
         *            the second value
         * @return the new SIMD value
         */
        @Function(name = "greaterThan", arity = 2)
        public static Object greaterThan(ExecutionContext cx, Object thisValue, Object a, Object b) {
            /* step 1 */
            SIMDValue sa = simdValue(cx, a, SIMD_TYPE, ".greaterThan");
            SIMDValue sb = simdValue(cx, b, SIMD_TYPE, ".greaterThan");
            /* steps 2-3 */
            SIMDValue result = SIMDRelationalOpInt(sa, sb, (x, y) -> x > y);
            /* step 4 */
            return result;
        }

        /**
         * SIMDConstructor.greaterThanOrEqual(a, b)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param a
         *            the first value
         * @param b
         *            the second value
         * @return the new SIMD value
         */
        @Function(name = "greaterThanOrEqual", arity = 2)
        public static Object greaterThanOrEqual(ExecutionContext cx, Object thisValue, Object a, Object b) {
            /* step 1 */
            SIMDValue sa = simdValue(cx, a, SIMD_TYPE, ".greaterThanOrEqual");
            SIMDValue sb = simdValue(cx, b, SIMD_TYPE, ".greaterThanOrEqual");
            /* steps 2-3 */
            SIMDValue result = SIMDRelationalOpInt(sa, sb, (x, y) -> x >= y);
            /* step 4 */
            return result;
        }

        /**
         * SIMDConstructor.equal(a, b)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param a
         *            the first value
         * @param b
         *            the second value
         * @return the new SIMD value
         */
        @Function(name = "equal", arity = 2)
        public static Object equal(ExecutionContext cx, Object thisValue, Object a, Object b) {
            /* step 1 */
            SIMDValue sa = simdValue(cx, a, SIMD_TYPE, ".equal");
            SIMDValue sb = simdValue(cx, b, SIMD_TYPE, ".equal");
            /* steps 2-3 */
            SIMDValue result = SIMDRelationalOpInt(sa, sb, (x, y) -> x == y);
            /* step 4 */
            return result;
        }

        /**
         * SIMDConstructor.notEqual(a, b)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param a
         *            the first value
         * @param b
         *            the second value
         * @return the new SIMD value
         */
        @Function(name = "notEqual", arity = 2)
        public static Object notEqual(ExecutionContext cx, Object thisValue, Object a, Object b) {
            /* step 1 */
            SIMDValue sa = simdValue(cx, a, SIMD_TYPE, ".notEqual");
            SIMDValue sb = simdValue(cx, b, SIMD_TYPE, ".notEqual");
            /* steps 2-3 */
            SIMDValue result = SIMDRelationalOpInt(sa, sb, (x, y) -> x != y);
            /* step 4 */
            return result;
        }

        /**
         * SIMDConstructor.select( selector, a, b )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param selector
         *            the selector
         * @param a
         *            the first value
         * @param b
         *            the second value
         * @return the new SIMD value
         */
        @Function(name = "select", arity = 3)
        public static Object select(ExecutionContext cx, Object thisValue, Object selector, Object a, Object b) {
            /* step 1 */
            // FIXME: spec bug - missing type check for selector
            SIMDValue ss = anySimdValue(cx, selector, ".select");
            SIMDValue sa = simdValue(cx, a, SIMD_TYPE, ".select");
            SIMDValue sb = simdValue(cx, b, SIMD_TYPE, ".select");
            /* step 2 */
            SIMDType outputDescriptor = SIMDBoolType(SIMD_TYPE);
            /* step 3 */
            if (ss.getType() != outputDescriptor) {
                throw newTypeError(cx, Messages.Key.SIMDInvalidType);
            }
            /* step 4 */
            int[] list = new int[VECTOR_LENGTH];
            /* step 5 */
            for (int i = 0; i < VECTOR_LENGTH; ++i) {
                /* steps 5.a-b */
                if (ss.asBoolean()[i]) {
                    /* step 5.a */
                    list[i] = sa.asInt()[i];
                } else {
                    /* step 5.b */
                    list[i] = sb.asInt()[i];
                }
            }
            /* step 6 */
            return SIMDCreate(SIMD_TYPE, list);
        }

        /**
         * SIMDConstructor.shiftLeftByScalar( a, bits )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param a
         *            the first value
         * @param bits
         *            the shift amount
         * @return the new SIMD value
         */
        @Function(name = "shiftLeftByScalar", arity = 2)
        public static Object shiftLeftByScalar(ExecutionContext cx, Object thisValue, Object a, Object bits) {
            /* step 1 */
            SIMDValue sa = simdValue(cx, a, SIMD_TYPE, ".shiftLeftByScalar");
            /* step 2 */
            // FIXME: spec bug - Missing ReturnIfAbrupt
            int scalar = ToInt32(cx, bits);
            /* step 3 */
            int shiftCount = scalar & (ELEMENT_SIZE * 8 - 1);
            /* steps 4-5 */
            SIMDValue result = SIMDScalarOp(sa, shiftCount, (x, y) -> x << y);
            /* step 6 */
            return result;
        }

        /**
         * SIMDConstructor.shiftRightByScalar( a, bits )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param a
         *            the first value
         * @param bits
         *            the shift amount
         * @return the new SIMD value
         */
        @Function(name = "shiftRightByScalar", arity = 2)
        public static Object shiftRightByScalar(ExecutionContext cx, Object thisValue, Object a, Object bits) {
            /* step 1 */
            SIMDValue sa = simdValue(cx, a, SIMD_TYPE, ".shiftRightByScalar");
            /* step 2 */
            // FIXME: spec bug - Missing ReturnIfAbrupt
            int scalar = ToInt32(cx, bits);
            /* step 3 */
            int shiftCount = scalar & (ELEMENT_SIZE * 8 - 1);
            /* steps 4-5 */
            SIMDValue result = SIMDScalarOp(sa, shiftCount, (x, y) -> x >> y);
            /* step 6 */
            return result;
        }

        /**
         * SIMDConstructor.extractLane( simd, lane )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param simd
         *            the simd value
         * @param lane
         *            the lane number
         * @return the SIMD lane value
         */
        @Function(name = "extractLane", arity = 2)
        public static Object extractLane(ExecutionContext cx, Object thisValue, Object simd, Object lane) {
            /* step 1 */
            SIMDValue sv = simdValue(cx, simd, SIMD_TYPE, ".extractLane");
            /* step 2 */
            return SIMDExtractLaneInt(sv, SIMDToLane(cx, VECTOR_LENGTH, lane));
        }

        /**
         * SIMDConstructor.replaceLane( simd, lane, value )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param simd
         *            the simd value
         * @param lane
         *            the lane number
         * @param value
         *            the new lane value
         * @return the new SIMD value
         */
        @Function(name = "replaceLane", arity = 3)
        public static Object replaceLane(ExecutionContext cx, Object thisValue, Object simd, Object lane,
                Object value) {
            /* step 1 */
            SIMDValue sv = simdValue(cx, simd, SIMD_TYPE, ".replaceLane");
            /* step 2 */
            return SIMDReplaceLaneInt(cx, sv, lane, value, AbstractOperations::ToInt32);
        }

        /**
         * SIMDConstructor.store( tarray, index, simd )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param tarray
         *            the typed array
         * @param index
         *            the typed array index
         * @param simd
         *            the simd value
         * @return the input SIMD value
         */
        @Function(name = "store", arity = 3)
        public static Object store(ExecutionContext cx, Object thisValue, Object tarray, Object index, Object simd) {
            /* step 1 */
            SIMDValue sv = simdValue(cx, simd, SIMD_TYPE, ".store");
            /* step 2 */
            return SIMDStoreInTypedArray(cx, tarray, index, SIMD_TYPE, sv, ".store");
        }

        /**
         * SIMDConstructor.store1( tarray, index, simd )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param tarray
         *            the typed array
         * @param index
         *            the typed array index
         * @param simd
         *            the simd value
         * @return the input SIMD value
         */
        @Function(name = "store1", arity = 3)
        public static Object store1(ExecutionContext cx, Object thisValue, Object tarray, Object index, Object simd) {
            /* step 1 */
            SIMDValue sv = simdValue(cx, simd, SIMD_TYPE, ".store1");
            /* step 2 */
            return SIMDStoreInTypedArray(cx, tarray, index, SIMD_TYPE, sv, 1, ".store1");
        }

        /**
         * SIMDConstructor.store2( tarray, index, simd )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param tarray
         *            the typed array
         * @param index
         *            the typed array index
         * @param simd
         *            the simd value
         * @return the input SIMD value
         */
        @Function(name = "store2", arity = 3)
        public static Object store2(ExecutionContext cx, Object thisValue, Object tarray, Object index, Object simd) {
            /* step 1 */
            SIMDValue sv = simdValue(cx, simd, SIMD_TYPE, ".store2");
            /* step 2 */
            return SIMDStoreInTypedArray(cx, tarray, index, SIMD_TYPE, sv, 2, ".store2");
        }

        /**
         * SIMDConstructor.store3( tarray, index, simd )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param tarray
         *            the typed array
         * @param index
         *            the typed array index
         * @param simd
         *            the simd value
         * @return the input SIMD value
         */
        @Function(name = "store3", arity = 3)
        public static Object store3(ExecutionContext cx, Object thisValue, Object tarray, Object index, Object simd) {
            /* step 1 */
            SIMDValue sv = simdValue(cx, simd, SIMD_TYPE, ".store3");
            /* step 2 */
            return SIMDStoreInTypedArray(cx, tarray, index, SIMD_TYPE, sv, 3, ".store3");
        }

        /**
         * SIMDConstructor.load( tarray, index )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param tarray
         *            the typed array
         * @param index
         *            the typed array index
         * @return the new SIMD value
         */
        @Function(name = "load", arity = 2)
        public static Object load(ExecutionContext cx, Object thisValue, Object tarray, Object index) {
            /* step 1 */
            return SIMDLoadFromTypedArray(cx, tarray, index, SIMD_TYPE, ".load");
        }

        /**
         * SIMDConstructor.load1( tarray, index )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param tarray
         *            the typed array
         * @param index
         *            the typed array index
         * @return the new SIMD value
         */
        @Function(name = "load1", arity = 2)
        public static Object load1(ExecutionContext cx, Object thisValue, Object tarray, Object index) {
            /* step 1 */
            return SIMDLoadFromTypedArray(cx, tarray, index, SIMD_TYPE, 1, ".load1");
        }

        /**
         * SIMDConstructor.load2( tarray, index )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param tarray
         *            the typed array
         * @param index
         *            the typed array index
         * @return the new SIMD value
         */
        @Function(name = "load2", arity = 2)
        public static Object load2(ExecutionContext cx, Object thisValue, Object tarray, Object index) {
            /* step 1 */
            return SIMDLoadFromTypedArray(cx, tarray, index, SIMD_TYPE, 2, ".load2");
        }

        /**
         * SIMDConstructor.load3( tarray, index )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param tarray
         *            the typed array
         * @param index
         *            the typed array index
         * @return the new SIMD value
         */
        @Function(name = "load3", arity = 2)
        public static Object load3(ExecutionContext cx, Object thisValue, Object tarray, Object index) {
            /* step 1 */
            return SIMDLoadFromTypedArray(cx, tarray, index, SIMD_TYPE, 3, ".load3");
        }

        /**
         * SIMD Constructor.from TIMD Bits( value ), TIMD = Float32x4
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the value
         * @return the new SIMD value
         */
        @Function(name = "fromFloat32x4Bits", arity = 1)
        public static Object fromFloat32x4Bits(ExecutionContext cx, Object thisValue, Object value) {
            /* step 1 */
            SIMDValue simd = simdValue(cx, value, SIMDType.Float32x4, ".fromFloat32x4Bits");
            /* step 2 */
            return SIMDReinterpretCast(cx, simd, SIMD_TYPE);
        }

        /**
         * SIMD Constructor.from TIMD Bits( value ), TIMD = Int16x8
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the value
         * @return the new SIMD value
         */
        @Function(name = "fromInt16x8Bits", arity = 1)
        public static Object fromInt16x8Bits(ExecutionContext cx, Object thisValue, Object value) {
            /* step 1 */
            SIMDValue simd = simdValue(cx, value, SIMDType.Int16x8, ".fromInt16x8Bits");
            /* step 2 */
            return SIMDReinterpretCast(cx, simd, SIMD_TYPE);
        }

        /**
         * SIMD Constructor.from TIMD Bits( value ), TIMD = Int8x16
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the value
         * @return the new SIMD value
         */
        @Function(name = "fromInt8x16Bits", arity = 1)
        public static Object fromInt8x16Bits(ExecutionContext cx, Object thisValue, Object value) {
            /* step 1 */
            SIMDValue simd = simdValue(cx, value, SIMDType.Int8x16, ".fromInt8x16Bits");
            /* step 2 */
            return SIMDReinterpretCast(cx, simd, SIMD_TYPE);
        }

        /**
         * SIMD Constructor.from TIMD Bits( value ), TIMD = Uint32x4
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the value
         * @return the new SIMD value
         */
        @Function(name = "fromUint32x4Bits", arity = 1)
        public static Object fromUint32x4Bits(ExecutionContext cx, Object thisValue, Object value) {
            /* step 1 */
            SIMDValue simd = simdValue(cx, value, SIMDType.Uint32x4, ".fromUint32x4Bits");
            /* step 2 */
            return SIMDReinterpretCast(cx, simd, SIMD_TYPE);
        }

        /**
         * SIMD Constructor.from TIMD Bits( value ), TIMD = Uint16x8
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the value
         * @return the new SIMD value
         */
        @Function(name = "fromUint16x8Bits", arity = 1)
        public static Object fromUint16x8Bits(ExecutionContext cx, Object thisValue, Object value) {
            /* step 1 */
            SIMDValue simd = simdValue(cx, value, SIMDType.Uint16x8, ".fromUint16x8Bits");
            /* step 2 */
            return SIMDReinterpretCast(cx, simd, SIMD_TYPE);
        }

        /**
         * SIMD Constructor.from TIMD Bits( value ), TIMD = Uint8x16
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the value
         * @return the new SIMD value
         */
        @Function(name = "fromUint8x16Bits", arity = 1)
        public static Object fromUint8x16Bits(ExecutionContext cx, Object thisValue, Object value) {
            /* step 1 */
            SIMDValue simd = simdValue(cx, value, SIMDType.Uint8x16, ".fromUint8x16Bits");
            /* step 2 */
            return SIMDReinterpretCast(cx, simd, SIMD_TYPE);
        }

        /**
         * SIMD Constructor.from TIMD ( value ), TIMD = Float32x4
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the value
         * @return the new SIMD value
         */
        @Function(name = "fromFloat32x4", arity = 1)
        public static Object fromFloat32x4(ExecutionContext cx, Object thisValue, Object value) {
            /* step 1 */
            SIMDValue simd = simdValue(cx, value, SIMDType.Float32x4, ".fromFloat32x4");
            /* steps 2-3 */
            int[] list = new int[VECTOR_LENGTH];
            for (int i = 0; i < VECTOR_LENGTH; ++i) {
                double v = simd.asDouble()[i];
                if (v != v) {
                    throw newRangeError(cx, Messages.Key.SIMDOutOfRange);
                }
                double intElement = ToInteger(v);
                if (intElement < MIN_VALUE || intElement > MAX_VALUE) {
                    throw newRangeError(cx, Messages.Key.SIMDOutOfRange);
                }
                list[i] = (int) intElement;
            }
            /* step 4 */
            return SIMDCreate(SIMD_TYPE, list);
        }

        /**
         * SIMDConstructor.swizzle( a, ...lanes )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param a
         *            the value
         * @param lanes
         *            the simd lanes
         * @return the new SIMD value
         */
        @Function(name = "swizzle", arity = 1 + 4)
        public static Object swizzle(ExecutionContext cx, Object thisValue, Object a, Object... lanes) {
            /* step 1 */
            SIMDValue sa = simdValue(cx, a, SIMD_TYPE, ".swizzle");
            /* step 2 */
            int[] indices = new int[VECTOR_LENGTH];
            /* step 3 */
            // FIXME: spec bug - variable name and incorrect bounds
            for (int i = 0; i < VECTOR_LENGTH; ++i) {
                /* step 3.a */
                Object lane = i < lanes.length ? lanes[i] : 0;
                /* steps 3.b-c */
                int index = SIMDToLane(cx, VECTOR_LENGTH, lane);
                /* step 3.d */
                indices[i] = index;
            }
            /* step 4 */
            int[] fields = new int[VECTOR_LENGTH];
            /* step 5 */
            for (int i = 0; i < VECTOR_LENGTH; ++i) {
                fields[i] = SIMDExtractLaneInt(sa, indices[i]);
            }
            /* step 6 */
            return SIMDCreate(SIMD_TYPE, fields);
        }

        /**
         * SIMDConstructor.shuffle( a, b, ...lanes )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param a
         *            the first value
         * @param b
         *            the second value
         * @param lanes
         *            the simd lanes
         * @return the new SIMD value
         */
        // FIXME: spec bug - length spec'ed as 10 instead of 6
        @Function(name = "shuffle", arity = 2 + 4)
        public static Object shuffle(ExecutionContext cx, Object thisValue, Object a, Object b, Object... lanes) {
            /* step 1 */
            SIMDValue sa = simdValue(cx, a, SIMD_TYPE, ".shuffle");
            SIMDValue sb = simdValue(cx, b, SIMD_TYPE, ".shuffle");
            /* step 2 */
            int[] indices = new int[VECTOR_LENGTH];
            /* step 3 */
            // FIXME: spec bug - variable name and incorrect bounds
            for (int i = 0; i < VECTOR_LENGTH; ++i) {
                /* step 3.a */
                Object lane = i < lanes.length ? lanes[i] : 0;
                /* steps 3.b-c */
                int index = SIMDToLane(cx, VECTOR_LENGTH * 2, lane);
                /* step 3.d */
                indices[i] = index;
            }
            /* step 4 */
            int[] fields = new int[VECTOR_LENGTH];
            /* step 5 */
            for (int i = 0; i < VECTOR_LENGTH; ++i) {
                /* step 5.a */
                int idx = indices[i];
                /* steps 5.b-c */
                if (idx >= VECTOR_LENGTH) {
                    /* step 5.b */
                    fields[i] = SIMDExtractLaneInt(sb, idx - VECTOR_LENGTH);
                } else {
                    /* step 5.c */
                    fields[i] = SIMDExtractLaneInt(sa, idx);
                }
            }
            /* step 6 */
            return SIMDCreate(SIMD_TYPE, fields);
        }
    }

    /**
     * Extension: SIMD.Float64x2
     */
    @CompatibilityExtension(CompatibilityOption.SIMD_Phase2)
    public enum AdditionalProperties {
        ;

        private static SIMDValue simdValue(ExecutionContext cx, Object value, SIMDType type, String method) {
            if (!(value instanceof SIMDValue)) {
                throw newTypeError(cx, Messages.Key.SIMDInvalidObject, SIMD_TYPE.name() + method,
                        Type.of(value).toString());
            }
            if (((SIMDValue) value).getType() != type) {
                throw newTypeError(cx, Messages.Key.SIMDInvalidType);
            }
            return (SIMDValue) value;
        }

        /**
         * SIMDConstructor.selectBits( selector, a, b )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param selector
         *            the selector
         * @param a
         *            the first value
         * @param b
         *            the second value
         * @return the new SIMD value
         */
        @Function(name = "selectBits", arity = 3)
        public static Object selectBits(ExecutionContext cx, Object thisValue, Object selector, Object a, Object b) {
            /* step 1 */
            SIMDValue ss = simdValue(cx, selector, SIMD_TYPE, ".selectBits");
            SIMDValue sa = simdValue(cx, a, SIMD_TYPE, ".selectBits");
            SIMDValue sb = simdValue(cx, b, SIMD_TYPE, ".selectBits");
            /* step 2 */
            int[] list = new int[VECTOR_LENGTH];
            /* step 3 */
            for (int i = 0; i < VECTOR_LENGTH; ++i) {
                int mask = ss.asInt()[i];
                list[i] = (sa.asInt()[i] & mask) | (sb.asInt()[i] & ~mask);
            }
            /* step 4 */
            return SIMDCreate(SIMD_TYPE, list);
        }

        /**
         * SIMD Constructor.from TIMD Bits( value ), TIMD = Float64x2
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the value
         * @return the new SIMD value
         */
        @Function(name = "fromFloat64x2Bits", arity = 1)
        public static Object fromFloat64x2Bits(ExecutionContext cx, Object thisValue, Object value) {
            /* step 1 */
            SIMDValue simd = simdValue(cx, value, SIMDType.Float64x2, ".fromFloat64x2Bits");
            /* step 2 */
            return SIMDReinterpretCast(cx, simd, SIMD_TYPE);
        }

        /**
         * SIMD Constructor.from TIMD ( value ), TIMD = Float64x2
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the value
         * @return the new SIMD value
         */
        @Function(name = "fromFloat64x2", arity = 1)
        public static Object fromFloat64x2(ExecutionContext cx, Object thisValue, Object value) {
            /* step 1 */
            SIMDValue simd = simdValue(cx, value, SIMDType.Float64x2, ".fromFloat64x2");
            /* steps 2-3 */
            int[] list = new int[VECTOR_LENGTH];
            for (int i = 0; i < SIMDType.Float64x2.getVectorLength(); ++i) {
                double v = simd.asDouble()[i];
                if (v != v) {
                    throw newRangeError(cx, Messages.Key.SIMDOutOfRange);
                }
                double intElement = ToInteger(v);
                if (intElement < MIN_VALUE || intElement > MAX_VALUE) {
                    throw newRangeError(cx, Messages.Key.SIMDOutOfRange);
                }
                list[i] = (int) intElement;
            }
            /* step 4 */
            return SIMDCreate(SIMD_TYPE, list);
        }
    }
}
