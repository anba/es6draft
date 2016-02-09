/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.simd;

import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.simd.SIMD.*;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.Arrays;

import com.github.anba.es6draft.runtime.AbstractOperations;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;

/**
 * <h1>SIMD</h1>
 * <h2>SIMD objects</h2>
 * <ul>
 * <li>The SIMD.Bool8x16 Constructor
 * <li>Properties of the SIMD.Bool8x16 Constructor
 * </ul>
 */
public final class Bool8x16Constructor extends BuiltinConstructor implements Initializable {
    private static final SIMDType SIMD_TYPE = SIMDType.Bool8x16;
    @SuppressWarnings("unused")
    private static final int ELEMENT_SIZE = SIMDType.Bool8x16.getElementSize();
    private static final int VECTOR_LENGTH = SIMDType.Bool8x16.getVectorLength();

    public Bool8x16Constructor(Realm realm) {
        super(realm, SIMD_TYPE.name(), VECTOR_LENGTH);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object[] fields = new Object[VECTOR_LENGTH];
        for (int i = 0; i < VECTOR_LENGTH; ++i) {
            fields[i] = i < args.length ? args[i] : UNDEFINED;
        }
        return SIMDCreateBool(calleeContext, SIMD_TYPE, fields, AbstractOperations::ToBoolean);
    }

    @Override
    public ScriptObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
        throw newTypeError(calleeContext(), Messages.Key.SIMDCreate, SIMD_TYPE.name());
    }

    @Override
    protected BuiltinFunction clone() {
        return new Bool8x16Constructor(getRealm());
    }

    /**
     * Properties of the SIMD.Bool8x16 Constructor
     */
    public enum Properties {
        ;

        private static SIMDValue simdValue(ExecutionContext cx, Object value, SIMDType type) {
            if (!(value instanceof SIMDValue)) {
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            if (((SIMDValue) value).getType() != type) {
                throw newTypeError(cx, Messages.Key.SIMDInvalidType);
            }
            return (SIMDValue) value;
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true) )
        public static final int length = VECTOR_LENGTH;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true) )
        public static final String name = SIMD_TYPE.name();

        // FIXME: spec bug - missing definition
        @Value(name = "prototype",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false) )
        public static final Intrinsics prototype = Intrinsics.SIMD_Bool8x16Prototype;

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
            return SIMDCreateBool(cx, SIMD_TYPE, list, AbstractOperations::ToBoolean);
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
            return simdValue(cx, a, SIMD_TYPE);
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
            SIMDValue sa = simdValue(cx, a, SIMD_TYPE);
            SIMDValue sb = simdValue(cx, b, SIMD_TYPE);
            /* steps 2-3 */
            SIMDValue result = SIMDBinaryOpBool(sa, sb, (x, y) -> x && y);
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
            SIMDValue sa = simdValue(cx, a, SIMD_TYPE);
            SIMDValue sb = simdValue(cx, b, SIMD_TYPE);
            /* steps 2-3 */
            SIMDValue result = SIMDBinaryOpBool(sa, sb, (x, y) -> x ^ y);
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
            SIMDValue sa = simdValue(cx, a, SIMD_TYPE);
            SIMDValue sb = simdValue(cx, b, SIMD_TYPE);
            /* steps 2-3 */
            SIMDValue result = SIMDBinaryOpBool(sa, sb, (x, y) -> x | y);
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
            SIMDValue sa = simdValue(cx, a, SIMD_TYPE);
            /* steps 2-3 */
            // FIXME: spec bug - incorrect definition, should be using `!` instead of `~`
            SIMDValue result = SIMDUnaryOpBool(sa, x -> !x);
            /* step 4 */
            return result;
        }

        /**
         * SIMDConstructor.anyTrue(a)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param a
         *            the value
         * @return the new SIMD value
         */
        @Function(name = "anyTrue", arity = 1)
        public static Object anyTrue(ExecutionContext cx, Object thisValue, Object a) {
            /* step 1 */
            SIMDValue sa = simdValue(cx, a, SIMD_TYPE);
            /* step 2 */
            for (int i = 0; i < VECTOR_LENGTH; ++i) {
                /* step 2.a */
                if (sa.asBoolean()[i]) {
                    return true;
                }
            }
            /* step 3 */
            return false;
        }

        /**
         * SIMDConstructor.allTrue(a)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param a
         *            the value
         * @return the new SIMD value
         */
        @Function(name = "allTrue", arity = 1)
        public static Object allTrue(ExecutionContext cx, Object thisValue, Object a) {
            /* step 1 */
            SIMDValue sa = simdValue(cx, a, SIMD_TYPE);
            /* step 2 */
            for (int i = 0; i < VECTOR_LENGTH; ++i) {
                /* step 2.a */
                if (!sa.asBoolean()[i]) {
                    return false;
                }
            }
            /* step 3 */
            return true;
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
            SIMDValue sv = simdValue(cx, simd, SIMD_TYPE);
            /* step 2 */
            return SIMDExtractLaneBool(sv, SIMDToLane(cx, VECTOR_LENGTH, lane));
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
            SIMDValue sv = simdValue(cx, simd, SIMD_TYPE);
            /* step 2 */
            return SIMDReplaceLaneBool(cx, sv, lane, value, AbstractOperations::ToBoolean);
        }
    }
}
