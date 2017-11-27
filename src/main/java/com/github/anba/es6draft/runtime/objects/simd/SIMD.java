/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.simd;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.CreateByteDataBlock;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.IsDetachedBuffer;
import static com.github.anba.es6draft.runtime.objects.simd.SIMDType.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.*;
import java.util.stream.Collectors;

import com.github.anba.es6draft.runtime.AbstractOperations;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.CompatibilityExtension;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.binary.TypedArrayObject;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * The %SIMD% intrinsic object.
 */
public final class SIMD extends OrdinaryObject implements Initializable {
    private static final boolean FLUSH_DENORMAL = false;

    public SIMD(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
        createProperties(realm, this, AdditionalProperties.class);
    }

    /**
     * {@link BiPredicate} specialized for {@code double} arguments.
     */
    @FunctionalInterface
    public interface DoubleBiPredicate {
        boolean test(double left, double right);
    }

    /**
     * {@link BiPredicate} specialized for {@code int} arguments.
     */
    @FunctionalInterface
    public interface IntBiPredicate {
        boolean test(int left, int right);
    }

    /**
     * {@link UnaryOperator} specialized for {@code boolean} arguments.
     */
    @FunctionalInterface
    public interface BooleanUnaryOperator {
        boolean applyAsBoolean(boolean operand);
    }

    /**
     * {@link BinaryOperator} specialized for {@code boolean} arguments.
     */
    @FunctionalInterface
    public interface BooleanBinaryOperator {
        boolean applyAsBoolean(boolean left, boolean right);
    }

    /**
     * {@link Function} specialized for {@code boolean} return type.
     */
    @FunctionalInterface
    public interface ToBooleanFunction<T> {
        boolean applyAsBoolean(T value);
    }

    /**
     * ToString ( argument )
     * 
     * @param value
     *            the argument value
     * @return the string result
     */
    public static String ToString(SIMDValue value) {
        return value.toString();
    }

    /**
     * SameValue(x, y)
     * 
     * @param x
     *            the first operand
     * @param y
     *            the second operand
     * @return {@code true} if both operands have the same value
     */
    public static boolean SameValue(SIMDValue x, SIMDValue y) {
        if (x.getType() != y.getType()) {
            return false;
        }
        switch (x.getType()) {
        case Float64x2:
        case Float32x4:
            return compareSIMDFloat(x, y, AbstractOperations::SameValue);
        case Int32x4:
        case Int16x8:
        case Int8x16:
        case Uint32x4:
        case Uint16x8:
        case Uint8x16:
            return Arrays.equals(x.asInt(), y.asInt());
        case Bool64x2:
        case Bool32x4:
        case Bool16x8:
        case Bool8x16:
            return Arrays.equals(x.asBoolean(), y.asBoolean());
        default:
            throw new AssertionError();
        }
    }

    /**
     * SameValueZero(x, y)
     * 
     * @param x
     *            the first operand
     * @param y
     *            the second operand
     * @return {@code true} if both operands have the same value
     */
    public static boolean SameValueZero(SIMDValue x, SIMDValue y) {
        if (x.getType() != y.getType()) {
            return false;
        }
        switch (x.getType()) {
        case Float64x2:
        case Float32x4:
            return compareSIMDFloat(x, y, AbstractOperations::SameValueZero);
        case Int32x4:
        case Int16x8:
        case Int8x16:
        case Uint32x4:
        case Uint16x8:
        case Uint8x16:
            return Arrays.equals(x.asInt(), y.asInt());
        case Bool64x2:
        case Bool32x4:
        case Bool16x8:
        case Bool8x16:
            return Arrays.equals(x.asBoolean(), y.asBoolean());
        default:
            throw new AssertionError();
        }
    }

    /**
     * Strict Equality Comparison
     * 
     * @param x
     *            the first operand
     * @param y
     *            the second operand
     * @return the comparison result
     */
    public static boolean StrictEquality(SIMDValue x, SIMDValue y) {
        if (x.getType() != y.getType()) {
            return false;
        }
        switch (x.getType()) {
        case Float64x2:
        case Float32x4:
            return compareSIMDFloat(x, y, (a, b) -> a == b);
        case Int32x4:
        case Int16x8:
        case Int8x16:
        case Uint32x4:
        case Uint16x8:
        case Uint8x16:
            return Arrays.equals(x.asInt(), y.asInt());
        case Bool64x2:
        case Bool32x4:
        case Bool16x8:
        case Bool8x16:
            return Arrays.equals(x.asBoolean(), y.asBoolean());
        default:
            throw new AssertionError();
        }
    }

    private static boolean compareSIMDFloat(SIMDValue x, SIMDValue y, DoubleBiPredicate compare) {
        double[] a = x.asDouble(), b = y.asDouble();
        for (int i = 0; i < a.length; ++i) {
            if (!compare.test(a[i], b[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDCreate( descriptor, vectorElements )
     * 
     * @param cx
     *            the execution context
     * @param descriptor
     *            the SIMD type descriptor
     * @param vectorElements
     *            the SIMD vector elements
     * @param cast
     *            the cast function
     * @return the new SIMD value
     */
    public static SIMDValue SIMDCreateFloat(ExecutionContext cx, SIMDType descriptor, Object[] vectorElements,
            ToDoubleBiFunction<ExecutionContext, Object> cast) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        assert vectorElements.length == descriptor.getVectorLength();
        /* step 4 */
        double[] list = new double[descriptor.getVectorLength()];
        /* step 5 */
        for (int i = 0, len = descriptor.getVectorLength(); i < len; ++i) {
            list[i] = cast.applyAsDouble(cx, vectorElements[i]);
        }
        /* step 6 */
        return new SIMDValue(descriptor, list);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDCreate( descriptor, vectorElements )
     * 
     * @param cx
     *            the execution context
     * @param descriptor
     *            the SIMD type descriptor
     * @param vectorElements
     *            the SIMD vector elements
     * @param cast
     *            the cast function
     * @return the new SIMD value
     */
    public static SIMDValue SIMDCreateInt(ExecutionContext cx, SIMDType descriptor, Object[] vectorElements,
            ToIntBiFunction<ExecutionContext, Object> cast) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        assert vectorElements.length == descriptor.getVectorLength();
        /* step 4 */
        int[] list = new int[descriptor.getVectorLength()];
        /* step 5 */
        for (int i = 0, len = descriptor.getVectorLength(); i < len; ++i) {
            list[i] = cast.applyAsInt(cx, vectorElements[i]);
        }
        /* step 6 */
        return new SIMDValue(descriptor, list);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDCreate( descriptor, vectorElements )
     * 
     * @param cx
     *            the execution context
     * @param descriptor
     *            the SIMD type descriptor
     * @param vectorElements
     *            the SIMD vector elements
     * @param cast
     *            the cast function
     * @return the new SIMD value
     */
    public static SIMDValue SIMDCreateBool(ExecutionContext cx, SIMDType descriptor, Object[] vectorElements,
            ToBooleanFunction<Object> cast) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        assert vectorElements.length == descriptor.getVectorLength();
        /* step 4 */
        boolean[] list = new boolean[descriptor.getVectorLength()];
        /* step 5 */
        for (int i = 0, len = descriptor.getVectorLength(); i < len; ++i) {
            list[i] = cast.applyAsBoolean(vectorElements[i]);
        }
        /* step 6 */
        return new SIMDValue(descriptor, list);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDCreate( descriptor, vectorElements )
     * 
     * @param descriptor
     *            the SIMD type descriptor
     * @param vectorElements
     *            the SIMD vector elements
     * @return the new SIMD value
     */
    public static SIMDValue SIMDCreate(SIMDType descriptor, double[] vectorElements) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        assert vectorElements.length == descriptor.getVectorLength();
        /* steps 4-5 (not applicable) */
        /* step 6 */
        return new SIMDValue(descriptor, vectorElements);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDCreate( descriptor, vectorElements )
     * 
     * @param descriptor
     *            the SIMD type descriptor
     * @param vectorElements
     *            the SIMD vector elements
     * @return the new SIMD value
     */
    public static SIMDValue SIMDCreate(SIMDType descriptor, int[] vectorElements) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        assert vectorElements.length == descriptor.getVectorLength();
        /* steps 4-5 (not applicable) */
        /* step 6 */
        return new SIMDValue(descriptor, vectorElements);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDCreate( descriptor, vectorElements )
     * 
     * @param descriptor
     *            the SIMD type descriptor
     * @param vectorElements
     *            the SIMD vector elements
     * @return the new SIMD value
     */
    public static SIMDValue SIMDCreate(SIMDType descriptor, boolean[] vectorElements) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        assert vectorElements.length == descriptor.getVectorLength();
        /* steps 4-5 (not applicable) */
        /* step 6 */
        return new SIMDValue(descriptor, vectorElements);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDToLane( max, lane )
     * 
     * @param cx
     *            the execution context
     * @param max
     *            the maximum lane index
     * @param lane
     *            the lane index
     * @return the SIMD lane index
     */
    public static int SIMDToLane(ExecutionContext cx, int max, Object lane) {
        /* steps 1-2 */
        double index = ToNumber(cx, lane);
        /* step 3 */
        // FIXME: spec issue - SameValueZero not required or useful here, change to normal == comparison? (check NaN!)
        // FIXME: spec issue - ToLength expected semantics here, ToInteger probably more correct...?!
        if (!AbstractOperations.SameValueZero(index, ToLength(index)) || index < 0 || index >= max) {
            throw newRangeError(cx, Messages.Key.SIMDInvalidLane);
        }
        /* step 4 */
        return (int) index;
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDExtractLane( value, lane )
     * 
     * @param value
     *            the SIMD value
     * @param lane
     *            the lane index
     * @return the lane value
     */
    public static double SIMDExtractLaneFloat(SIMDValue value, int lane) {
        /* step 1 (not applicable) */
        /* steps 2-3 */
        assert 0 <= lane && lane < value.getType().getVectorLength();
        int index = lane;
        /* step 4 */
        return value.asDouble()[index];
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDExtractLane( value, lane )
     * 
     * @param value
     *            the SIMD value
     * @param lane
     *            the lane index
     * @return the lane value
     */
    public static int SIMDExtractLaneInt(SIMDValue value, int lane) {
        /* step 1 (not applicable) */
        /* steps 2-3 */
        assert 0 <= lane && lane < value.getType().getVectorLength();
        int index = lane;
        /* step 4 */
        return value.asInt()[index];
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDExtractLane( value, lane )
     * 
     * @param value
     *            the SIMD value
     * @param lane
     *            the lane index
     * @return the lane value
     */
    public static boolean SIMDExtractLaneBool(SIMDValue value, int lane) {
        /* step 1 (not applicable) */
        /* steps 2-3 */
        assert 0 <= lane && lane < value.getType().getVectorLength();
        int index = lane;
        /* step 4 */
        return value.asBoolean()[index];
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDReplaceLane( value, lane, replacement )
     * 
     * @param cx
     *            the execution context
     * @param value
     *            the SIMD value
     * @param lane
     *            the lane index
     * @param replacement
     *            the replacement value
     * @param cast
     *            the cast function
     * @return the new SIMD value
     */
    public static SIMDValue SIMDReplaceLaneFloat(ExecutionContext cx, SIMDValue value, Object lane, Object replacement,
            ToDoubleBiFunction<ExecutionContext, Object> cast) {
        /* step 1 (not applicable) */
        /* step 2 */
        SIMDType descriptor = value.getType();
        /* steps 3-4 */
        int index = SIMDToLane(cx, descriptor.getVectorLength(), lane);
        /* step 5 */
        double[] list = value.asDouble().clone();
        /* step 6 (inlined SIMDCreate - [[Cast]]) */
        list[index] = cast.applyAsDouble(cx, replacement);
        /* step 7 */
        return SIMDCreate(descriptor, list);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDReplaceLane( value, lane, replacement )
     * 
     * @param cx
     *            the execution context
     * @param value
     *            the SIMD value
     * @param lane
     *            the lane index
     * @param replacement
     *            the replacement value
     * @param cast
     *            the cast function
     * @return the new SIMD value
     */
    public static SIMDValue SIMDReplaceLaneInt(ExecutionContext cx, SIMDValue value, Object lane, Object replacement,
            ToIntBiFunction<ExecutionContext, Object> cast) {
        /* step 1 (not applicable) */
        /* step 2 */
        SIMDType descriptor = value.getType();
        /* steps 3-4 */
        int index = SIMDToLane(cx, descriptor.getVectorLength(), lane);
        /* step 5 */
        int[] list = value.asInt().clone();
        /* step 6 (inlined SIMDCreate - [[Cast]]) */
        list[index] = cast.applyAsInt(cx, replacement);
        /* step 7 */
        return SIMDCreate(descriptor, list);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDReplaceLane( value, lane, replacement )
     * 
     * @param cx
     *            the execution context
     * @param value
     *            the SIMD value
     * @param lane
     *            the lane index
     * @param replacement
     *            the replacement value
     * @param cast
     *            the cast function
     * @return the new SIMD value
     */
    public static SIMDValue SIMDReplaceLaneBool(ExecutionContext cx, SIMDValue value, Object lane, Object replacement,
            ToBooleanFunction<Object> cast) {
        /* step 1 (not applicable) */
        /* step 2 */
        SIMDType descriptor = value.getType();
        /* steps 3-4 */
        int index = SIMDToLane(cx, descriptor.getVectorLength(), lane);
        /* step 5 */
        boolean[] list = value.asBoolean().clone();
        /* step 6 (inlined SIMDCreate - [[Cast]]) */
        list[index] = cast.applyAsBoolean(replacement);
        /* step 7 */
        return SIMDCreate(descriptor, list);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * MaybeFlushDenormal( n, descriptor )
     * 
     * @param n
     *            the number value
     * @return the number value
     */
    public static double MaybeFlushDenormalFloat(double n) {
        /* steps 1-4 */
        if (FLUSH_DENORMAL) {
            if (isDenormalized((float) n)) {
                return n > 0 ? +0d : -0d;
            }
        }
        /* step 5/1 */
        return n;
    }

    private static boolean isDenormalized(float f) {
        return f != 0 && f > -Float.MIN_NORMAL && f < Float.MIN_NORMAL;
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * MaybeFlushDenormal( n, descriptor )
     * 
     * @param n
     *            the number value
     * @return the number value
     */
    public static double MaybeFlushDenormalDouble(double n) {
        /* steps 1-4 */
        if (FLUSH_DENORMAL) {
            if (isDenormalized(n)) {
                return n > 0 ? +0d : -0d;
            }
        }
        /* step 5/1 */
        return n;
    }

    private static boolean isDenormalized(double d) {
        return d != 0 && d > -Double.MIN_NORMAL && d < Double.MIN_NORMAL;
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDBinaryOp( a, b, op, outputDescriptor )
     * 
     * @param a
     *            the first operand
     * @param b
     *            the second operand
     * @param op
     *            the binary operator
     * @return the new SIMD value
     */
    public static SIMDValue SIMDBinaryOpFloat(SIMDValue a, SIMDValue b, DoubleBinaryOperator op) {
        /* step 1 */
        assert a.getType() == b.getType();
        assert a.getType() == SIMDType.Float32x4;
        /* step 2 */
        SIMDType descriptor = a.getType();
        /* step 3 */
        SIMDType outputDescriptor = a.getType();
        /* step 4 */
        double[] list = new double[descriptor.getVectorLength()];
        /* step 5 */
        for (int i = 0, len = descriptor.getVectorLength(); i < len; ++i) {
            /* step 5.a */
            double ax = MaybeFlushDenormalFloat(SIMDExtractLaneFloat(a, i));
            /* step 5.b */
            double bx = MaybeFlushDenormalFloat(SIMDExtractLaneFloat(b, i));
            /* steps 5.c-d */
            double res = op.applyAsDouble(ax, bx);
            /* step 5.e */
            res = MaybeFlushDenormalFloat(res);
            /* step 5.f */
            list[i] = res;
        }
        /* step 6 */
        return SIMDCreate(outputDescriptor, list);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDBinaryOp( a, b, op, outputDescriptor )
     * 
     * @param a
     *            the first operand
     * @param b
     *            the second operand
     * @param op
     *            the binary operator
     * @return the new SIMD value
     */
    public static SIMDValue SIMDBinaryOpDouble(SIMDValue a, SIMDValue b, DoubleBinaryOperator op) {
        /* step 1 */
        assert a.getType() == b.getType();
        assert a.getType() == SIMDType.Float64x2;
        /* step 2 */
        SIMDType descriptor = a.getType();
        /* step 3 */
        SIMDType outputDescriptor = a.getType();
        /* step 4 */
        double[] list = new double[descriptor.getVectorLength()];
        /* step 5 */
        for (int i = 0, len = descriptor.getVectorLength(); i < len; ++i) {
            /* step 5.a */
            double ax = MaybeFlushDenormalDouble(SIMDExtractLaneFloat(a, i));
            /* step 5.b */
            double bx = MaybeFlushDenormalDouble(SIMDExtractLaneFloat(b, i));
            /* steps 5.c-d */
            double res = op.applyAsDouble(ax, bx);
            /* step 5.e */
            res = MaybeFlushDenormalDouble(res);
            /* step 5.f */
            list[i] = res;
        }
        /* step 6 */
        return SIMDCreate(outputDescriptor, list);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDBinaryOp( a, b, op, outputDescriptor )
     * 
     * @param a
     *            the first operand
     * @param b
     *            the second operand
     * @param op
     *            the binary operator
     * @param outputDescriptor
     *            the output type descriptor, always boolean SIMD type
     * @return the new SIMD value
     */
    public static SIMDValue SIMDBinaryOpFloat(SIMDValue a, SIMDValue b, DoubleBiPredicate op,
            SIMDType outputDescriptor) {
        /* step 1 */
        assert a.getType() == b.getType();
        assert a.getType() == SIMDType.Float32x4 && outputDescriptor == SIMDType.Bool32x4;
        /* step 2 */
        SIMDType descriptor = a.getType();
        /* step 3 (not applicable) */
        /* step 4 */
        boolean[] list = new boolean[descriptor.getVectorLength()];
        /* step 5 */
        for (int i = 0, len = descriptor.getVectorLength(); i < len; ++i) {
            /* step 5.a */
            double ax = MaybeFlushDenormalFloat(SIMDExtractLaneFloat(a, i));
            /* step 5.b */
            double bx = MaybeFlushDenormalFloat(SIMDExtractLaneFloat(b, i));
            /* steps 5.c-d */
            boolean res = op.test(ax, bx);
            /* step 5.e (not applicable) */
            /* step 5.f */
            list[i] = res;
        }
        /* step 6 */
        return SIMDCreate(outputDescriptor, list);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDBinaryOp( a, b, op, outputDescriptor )
     * 
     * @param a
     *            the first operand
     * @param b
     *            the second operand
     * @param op
     *            the binary operator
     * @param outputDescriptor
     *            the output type descriptor, always boolean SIMD type
     * @return the new SIMD value
     */
    public static SIMDValue SIMDBinaryOpDouble(SIMDValue a, SIMDValue b, DoubleBiPredicate op,
            SIMDType outputDescriptor) {
        /* step 1 */
        assert a.getType() == b.getType();
        assert a.getType() == SIMDType.Float64x2 && outputDescriptor == SIMDType.Bool64x2;
        /* step 2 */
        SIMDType descriptor = a.getType();
        /* step 3 (not applicable) */
        /* step 4 */
        boolean[] list = new boolean[outputDescriptor.getVectorLength()];
        /* step 5 */
        for (int i = 0, len = descriptor.getVectorLength(); i < len; ++i) {
            /* step 5.a */
            double ax = MaybeFlushDenormalDouble(SIMDExtractLaneFloat(a, i));
            /* step 5.b */
            double bx = MaybeFlushDenormalDouble(SIMDExtractLaneFloat(b, i));
            /* steps 5.c-d */
            boolean res = op.test(ax, bx);
            /* step 5.e (not applicable) */
            /* step 5.f */
            list[i] = res;
        }
        /* step 6 */
        return SIMDCreate(outputDescriptor, list);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDBinaryOp( a, b, op, outputDescriptor )
     * 
     * @param a
     *            the first operand
     * @param b
     *            the second operand
     * @param op
     *            the binary operator
     * @return the new SIMD value
     */
    public static SIMDValue SIMDBinaryOpInt(SIMDValue a, SIMDValue b, IntBinaryOperator op) {
        /* step 1 */
        assert a.getType() == b.getType();
        assert a.getType().isInteger();
        /* step 2 */
        SIMDType descriptor = a.getType();
        /* step 3 */
        SIMDType outputDescriptor = a.getType();
        /* step 4 */
        int[] list = new int[descriptor.getVectorLength()];
        /* step 5 */
        for (int i = 0, len = descriptor.getVectorLength(); i < len; ++i) {
            /* step 5.a */
            int ax = SIMDExtractLaneInt(a, i);
            /* step 5.b */
            int bx = SIMDExtractLaneInt(b, i);
            /* steps 5.c-d */
            int res = op.applyAsInt(ax, bx);
            /* step 5.e (not applicable) */
            /* step 5.f */
            list[i] = res;
        }
        /* step 6 */
        return SIMDCreate(outputDescriptor, list);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDBinaryOp( a, b, op, outputDescriptor )
     * 
     * @param a
     *            the first operand
     * @param b
     *            the second operand
     * @param op
     *            the binary operator
     * @param outputDescriptor
     *            the output type descriptor, always boolean SIMD type
     * @return the new SIMD value
     */
    public static SIMDValue SIMDBinaryOpInt(SIMDValue a, SIMDValue b, IntBiPredicate op, SIMDType outputDescriptor) {
        /* step 1 */
        assert a.getType() == b.getType();
        assert a.getType().getVectorLength() == outputDescriptor.getVectorLength();
        assert a.getType().isInteger() && outputDescriptor.isBoolean();
        /* step 2 */
        SIMDType descriptor = a.getType();
        /* step 3 (not applicable) */
        /* step 4 */
        boolean[] list = new boolean[descriptor.getVectorLength()];
        /* step 5 */
        for (int i = 0, len = descriptor.getVectorLength(); i < len; ++i) {
            /* step 5.a */
            int ax = SIMDExtractLaneInt(a, i);
            /* step 5.b */
            int bx = SIMDExtractLaneInt(b, i);
            /* steps 5.c-d */
            boolean res = op.test(ax, bx);
            /* step 5.e (not applicable) */
            /* step 5.f */
            list[i] = res;
        }
        /* step 6 */
        return SIMDCreate(outputDescriptor, list);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDBinaryOp( a, b, op, outputDescriptor )
     * 
     * @param a
     *            the first operand
     * @param b
     *            the second operand
     * @param op
     *            the binary operator
     * @return the new SIMD value
     */
    public static SIMDValue SIMDBinaryOpBool(SIMDValue a, SIMDValue b, BooleanBinaryOperator op) {
        /* step 1 */
        assert a.getType() == b.getType();
        assert a.getType().isBoolean();
        /* step 2 */
        SIMDType descriptor = a.getType();
        /* step 3 */
        SIMDType outputDescriptor = a.getType();
        /* step 4 */
        boolean[] list = new boolean[descriptor.getVectorLength()];
        /* step 5 */
        for (int i = 0, len = descriptor.getVectorLength(); i < len; ++i) {
            /* step 5.a */
            boolean ax = SIMDExtractLaneBool(a, i);
            /* step 5.b */
            boolean bx = SIMDExtractLaneBool(b, i);
            /* steps 5.c-d */
            boolean res = op.applyAsBoolean(ax, bx);
            /* step 5.e (not applicable) */
            /* step 5.f */
            list[i] = res;
        }
        /* step 6 */
        return SIMDCreate(outputDescriptor, list);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDUnaryOp( a, op [ , flushDenormal ] )
     * 
     * @param a
     *            the operand value
     * @param op
     *            the unary operator
     * @return the new SIMD value
     */
    public static SIMDValue SIMDUnaryOpFloat(SIMDValue a, DoubleUnaryOperator op) {
        return SIMDUnaryOpFloat(a, op, true);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDUnaryOp( a, op [ , flushDenormal ] )
     * 
     * @param a
     *            the operand value
     * @param op
     *            the unary operator
     * @param flushDenormal
     *            if {@code true} denormals are flushed to zero
     * @return the new SIMD value
     */
    public static SIMDValue SIMDUnaryOpFloat(SIMDValue a, DoubleUnaryOperator op, boolean flushDenormal) {
        /* step 1 */
        SIMDType descriptor = a.getType();
        assert descriptor == SIMDType.Float32x4;
        /* step 2 (not applicable) */
        /* step 3 */
        double[] list = new double[descriptor.getVectorLength()];
        /* step 4 (FIXME: spec bug) */
        /* step 5 */
        for (int i = 0, len = descriptor.getVectorLength(); i < len; ++i) {
            /* step 5.a */
            double ax = SIMDExtractLaneFloat(a, i);
            /* step 5.b */
            if (flushDenormal) {
                ax = MaybeFlushDenormalFloat(ax);
            }
            /* steps 5.c-d */
            double res = op.applyAsDouble(ax);
            /* step 5.e */
            if (flushDenormal) {
                res = MaybeFlushDenormalFloat(res);
            }
            /* step 5.f */
            list[i] = res;
        }
        /* step 6 */
        return SIMDCreate(descriptor, list);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDUnaryOp( a, op [ , flushDenormal ] )
     * 
     * @param a
     *            the operand value
     * @param op
     *            the unary operator
     * @return the new SIMD value
     */
    public static SIMDValue SIMDUnaryOpDouble(SIMDValue a, DoubleUnaryOperator op) {
        return SIMDUnaryOpDouble(a, op, true);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDUnaryOp( a, op [ , flushDenormal ] )
     * 
     * @param a
     *            the operand value
     * @param op
     *            the unary operator
     * @param flushDenormal
     *            if {@code true} denormals are flushed to zero
     * @return the new SIMD value
     */
    public static SIMDValue SIMDUnaryOpDouble(SIMDValue a, DoubleUnaryOperator op, boolean flushDenormal) {
        /* step 1 */
        SIMDType descriptor = a.getType();
        assert descriptor == SIMDType.Float64x2;
        /* step 2 (not applicable) */
        /* step 3 */
        double[] list = new double[descriptor.getVectorLength()];
        /* step 4 (FIXME: spec bug) */
        /* step 5 */
        for (int i = 0, len = descriptor.getVectorLength(); i < len; ++i) {
            /* step 5.a */
            double ax = SIMDExtractLaneFloat(a, i);
            /* step 5.b */
            if (flushDenormal) {
                ax = MaybeFlushDenormalDouble(ax);
            }
            /* steps 5.c-d */
            double res = op.applyAsDouble(ax);
            /* step 5.e */
            if (flushDenormal) {
                res = MaybeFlushDenormalDouble(res);
            }
            /* step 5.f */
            list[i] = res;
        }
        /* step 6 */
        return SIMDCreate(descriptor, list);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDUnaryOp( a, op [ , flushDenormal ] )
     * 
     * @param a
     *            the operand value
     * @param op
     *            the unary operator
     * @return the new SIMD value
     */
    public static SIMDValue SIMDUnaryOpInt(SIMDValue a, IntUnaryOperator op) {
        /* step 1 */
        SIMDType descriptor = a.getType();
        assert descriptor.isInteger();
        /* step 2 (not applicable) */
        /* step 3 */
        int[] list = new int[descriptor.getVectorLength()];
        /* step 4 (FIXME: spec bug) */
        /* step 5 */
        for (int i = 0, len = descriptor.getVectorLength(); i < len; ++i) {
            /* step 5.a */
            int ax = SIMDExtractLaneInt(a, i);
            /* step 5.b (not applicable) */
            /* steps 5.c-d */
            int res = op.applyAsInt(ax);
            /* step 5.e (not applicable) */
            /* step 5.f */
            list[i] = res;
        }
        /* step 6 */
        return SIMDCreate(descriptor, list);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDUnaryOp( a, op [ , flushDenormal ] )
     * 
     * @param a
     *            the operand value
     * @param op
     *            the unary operator
     * @return the new SIMD value
     */
    public static SIMDValue SIMDUnaryOpBool(SIMDValue a, BooleanUnaryOperator op) {
        /* step 1 */
        SIMDType descriptor = a.getType();
        assert descriptor.isBoolean();
        /* step 2 (not applicable) */
        /* step 3 */
        boolean[] list = new boolean[descriptor.getVectorLength()];
        /* step 4 (FIXME: spec bug) */
        /* step 5 */
        for (int i = 0, len = descriptor.getVectorLength(); i < len; ++i) {
            /* step 5.a */
            boolean ax = SIMDExtractLaneBool(a, i);
            /* step 5.b (not applicable) */
            /* steps 5.c-d */
            boolean res = op.applyAsBoolean(ax);
            /* step 5.e (not applicable) */
            /* step 5.f */
            list[i] = res;
        }
        /* step 6 */
        return SIMDCreate(descriptor, list);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDScalarOp( a, scalar, op )
     * 
     * @param a
     *            the operand value
     * @param scalar
     *            the scalar value
     * @param op
     *            the binary operator
     * @return the new SIMD value
     */
    public static SIMDValue SIMDScalarOp(SIMDValue a, int scalar, IntBinaryOperator op) {
        /* step 1 */
        SIMDType descriptor = a.getType();
        assert descriptor.isInteger();
        /* step 2 */
        // FIXME: spec issue - instead assert descriptor is integer type!
        assert !descriptor.isFloatingPoint();
        /* step 3 */
        int[] list = new int[descriptor.getVectorLength()];
        /* step 4 (FIXME: spec bug) */
        /* step 5 */
        for (int i = 0, len = descriptor.getVectorLength(); i < len; ++i) {
            /* step 5.a */
            int ax = SIMDExtractLaneInt(a, i);
            /* steps 5.b-c */
            int res = op.applyAsInt(ax, scalar);
            /* step 5.d */
            list[i] = res;
        }
        /* step 6 */
        return SIMDCreate(descriptor, list);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDLoad( dataBlock, descriptor, byteOffset [, length] )
     * 
     * @param dataBlock
     *            the data block
     * @param descriptor
     *            the SIMD type descriptor
     * @param byteOffset
     *            the byte offset
     * @return the new SIMD value
     */
    public static SIMDValue SIMDLoad(ByteBuffer dataBlock, SIMDType descriptor, long byteOffset) {
        return SIMDLoad(dataBlock, descriptor, byteOffset, descriptor.getVectorLength());
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDLoad( dataBlock, descriptor, byteOffset [, length] )
     * 
     * @param dataBlock
     *            the data block
     * @param descriptor
     *            the SIMD type descriptor
     * @param byteOffset
     *            the byte offset
     * @param length
     *            the read length
     * @return the new SIMD value
     */
    public static SIMDValue SIMDLoad(ByteBuffer dataBlock, SIMDType descriptor, long byteOffset, int length) {
        assert !descriptor.isBoolean() : "Cannot deserialize boolean";
        /* step 1 (not applicable) */
        /* step 2 */
        assert 0 < length && length <= descriptor.getVectorLength();
        /* step 3 */
        assert 0 <= byteOffset && byteOffset <= (dataBlock.capacity() - descriptor.getElementSize() * length);
        /* steps 4-5 */
        Object list;
        switch (descriptor) {
        case Float64x2:
            list = SIMDLoadFloat64x2(dataBlock, (int) byteOffset, length);
            break;
        case Float32x4:
            list = SIMDLoadFloat32x4(dataBlock, (int) byteOffset, length);
            break;
        case Int32x4:
            list = SIMDLoadInt32x4(dataBlock, (int) byteOffset, length);
            break;
        case Int16x8:
            list = SIMDLoadInt16x8(dataBlock, (int) byteOffset, length);
            break;
        case Int8x16:
            list = SIMDLoadInt8x16(dataBlock, (int) byteOffset, length);
            break;
        case Uint32x4:
            list = SIMDLoadUint32x4(dataBlock, (int) byteOffset, length);
            break;
        case Uint16x8:
            list = SIMDLoadUint16x8(dataBlock, (int) byteOffset, length);
            break;
        case Uint8x16:
            list = SIMDLoadUint8x16(dataBlock, (int) byteOffset, length);
            break;
        case Bool64x2:
        case Bool32x4:
        case Bool16x8:
        case Bool8x16:
        default:
            throw new AssertionError();
        }
        /* step 6 */
        return new SIMDValue(descriptor, list);
    }

    private static double[] SIMDLoadFloat64x2(ByteBuffer dataBlock, int byteOffset, int length) {
        double[] list = new double[SIMDType.Float64x2.getVectorLength()];
        for (int i = 0; i < length; ++i) {
            int offset = byteOffset + i * SIMDType.Float64x2.getElementSize();
            list[i] = DeserializeFloat64(dataBlock, offset);
        }
        return list;
    }

    private static double[] SIMDLoadFloat32x4(ByteBuffer dataBlock, int byteOffset, int length) {
        double[] list = new double[SIMDType.Float32x4.getVectorLength()];
        for (int i = 0; i < length; ++i) {
            int offset = byteOffset + i * SIMDType.Float32x4.getElementSize();
            list[i] = DeserializeFloat32(dataBlock, offset);
        }
        return list;
    }

    private static int[] SIMDLoadInt32x4(ByteBuffer dataBlock, int byteOffset, int length) {
        int[] list = new int[SIMDType.Int32x4.getVectorLength()];
        for (int i = 0; i < length; ++i) {
            int offset = byteOffset + i * SIMDType.Int32x4.getElementSize();
            list[i] = DeserializeInt32(dataBlock, offset);
        }
        return list;
    }

    private static int[] SIMDLoadInt16x8(ByteBuffer dataBlock, int byteOffset, int length) {
        int[] list = new int[SIMDType.Int16x8.getVectorLength()];
        for (int i = 0; i < length; ++i) {
            int offset = byteOffset + i * SIMDType.Int16x8.getElementSize();
            list[i] = DeserializeInt16(dataBlock, offset);
        }
        return list;
    }

    private static int[] SIMDLoadInt8x16(ByteBuffer dataBlock, int byteOffset, int length) {
        int[] list = new int[SIMDType.Int8x16.getVectorLength()];
        for (int i = 0; i < length; ++i) {
            int offset = byteOffset + i * SIMDType.Int8x16.getElementSize();
            list[i] = DeserializeInt8(dataBlock, offset);
        }
        return list;
    }

    private static int[] SIMDLoadUint32x4(ByteBuffer dataBlock, int byteOffset, int length) {
        int[] list = new int[SIMDType.Uint32x4.getVectorLength()];
        for (int i = 0; i < length; ++i) {
            int offset = byteOffset + i * SIMDType.Uint32x4.getElementSize();
            list[i] = DeserializeUint32(dataBlock, offset);
        }
        return list;
    }

    private static int[] SIMDLoadUint16x8(ByteBuffer dataBlock, int byteOffset, int length) {
        int[] list = new int[SIMDType.Uint16x8.getVectorLength()];
        for (int i = 0; i < length; ++i) {
            int offset = byteOffset + i * SIMDType.Uint16x8.getElementSize();
            list[i] = DeserializeUint16(dataBlock, offset);
        }
        return list;
    }

    private static int[] SIMDLoadUint8x16(ByteBuffer dataBlock, int byteOffset, int length) {
        int[] list = new int[SIMDType.Uint8x16.getVectorLength()];
        for (int i = 0; i < length; ++i) {
            int offset = byteOffset + i * SIMDType.Uint8x16.getElementSize();
            list[i] = DeserializeUint8(dataBlock, offset);
        }
        return list;
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDLoadFromTypedArray( tarray, index, descriptor [, length] )
     * 
     * @param cx
     *            the execution context
     * @param tarray
     *            the typed array
     * @param index
     *            the read index
     * @param descriptor
     *            the SIMD type descriptor
     * @param method
     *            the method name
     * @return the new SIMD value
     */
    public static SIMDValue SIMDLoadFromTypedArray(ExecutionContext cx, Object tarray, Object index,
            SIMDType descriptor, String method) {
        return SIMDLoadFromTypedArray(cx, tarray, index, descriptor, descriptor.getVectorLength(), method);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDLoadFromTypedArray( tarray, index, descriptor [, length] )
     * 
     * @param cx
     *            the execution context
     * @param tarray
     *            the typed array
     * @param index
     *            the read index
     * @param descriptor
     *            the SIMD type descriptor
     * @param length
     *            the read length
     * @param method
     *            the method name
     * @return the new SIMD value
     */
    public static SIMDValue SIMDLoadFromTypedArray(ExecutionContext cx, Object tarray, Object index,
            SIMDType descriptor, int length, String method) {
        /* step 1 */
        // FIXME: spec bug - missing type check Type(tarray) = Object
        if (!(tarray instanceof TypedArrayObject)) {
            throw newTypeError(cx, Messages.Key.SIMDInvalidObject, descriptor.name() + method,
                    Type.of(tarray).toString());
        }
        TypedArrayObject typedArray = (TypedArrayObject) tarray;
        /* step 2 */
        assert 0 < length && length <= descriptor.getVectorLength();
        // FIXME: spec bug - missing ToNumber call
        // FIXME: ToNumber(index) comparing against ToLength(ToNumber(index)) does not match any ES2015 pattern.
        // FIXME: spec issue - range checking does not match Mozilla SIMD.
        double numIndex = ToNumber(cx, index);
        /* step 3 */
        if (IsDetachedBuffer(typedArray.getBuffer())) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* step 4 */
        // FIXME: spec issue - allow shared array buffers?
        ByteBuffer block = typedArray.getBuffer().getData();
        /* step 5 */
        if (numIndex != ToLength(numIndex)) {
            throw newTypeError(cx, Messages.Key.InvalidByteOffset);
        }
        /* steps 6-7 */
        // FIXME: spec issue - should use typedArray.[[TypedArrayName]] and retrieve element size from table 49.
        // FIXME: spec issue - rename elementLength to elementSize to match ES2015.
        long byteIndex = typedArray.getElementType().toBytes((long) numIndex);
        /* step 8 */
        if (byteIndex < 0 || byteIndex + (descriptor.getElementSize() * length) > typedArray.getByteLength()) {
            throw newRangeError(cx, Messages.Key.InvalidByteOffset);
        }
        /* step 9 */
        return SIMDLoad(block, descriptor, byteIndex, length);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDStore( dataBlock, descriptor, byteOffset, n [, length] )
     * 
     * @param dataBlock
     *            the data block
     * @param descriptor
     *            the SIMD type descriptor
     * @param byteOffset
     *            the byte offset
     * @param n
     *            the SIMD value
     */
    public static void SIMDStore(ByteBuffer dataBlock, SIMDType descriptor, long byteOffset, SIMDValue n) {
        SIMDStore(dataBlock, descriptor, byteOffset, n, descriptor.getVectorLength());
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDStore( dataBlock, descriptor, byteOffset, n [, length] )
     * 
     * @param dataBlock
     *            the data block
     * @param descriptor
     *            the SIMD type descriptor
     * @param byteOffset
     *            the byte offset
     * @param n
     *            the SIMD value
     * @param length
     *            the write length
     */
    public static void SIMDStore(ByteBuffer dataBlock, SIMDType descriptor, long byteOffset, SIMDValue n, int length) {
        assert !descriptor.isBoolean() : "Cannot serialize boolean";
        assert descriptor == n.getType();
        /* step 1 (not applicable) */
        /* step 2 */
        assert 0 < length && length <= descriptor.getVectorLength();
        /* step 3 */
        assert 0 <= byteOffset && byteOffset <= (dataBlock.capacity() - descriptor.getElementSize() * length);
        /* step 4 */
        switch (descriptor) {
        case Float64x2:
            SIMDStoreFloat64x2(dataBlock, (int) byteOffset, n, length);
            break;
        case Float32x4:
            SIMDStoreFloat32x4(dataBlock, (int) byteOffset, n, length);
            break;
        case Int32x4:
            SIMDStoreInt32x4(dataBlock, (int) byteOffset, n, length);
            break;
        case Int16x8:
            SIMDStoreInt16x8(dataBlock, (int) byteOffset, n, length);
            break;
        case Int8x16:
            SIMDStoreInt8x16(dataBlock, (int) byteOffset, n, length);
            break;
        case Uint32x4:
            SIMDStoreUint32x4(dataBlock, (int) byteOffset, n, length);
            break;
        case Uint16x8:
            SIMDStoreUint16x8(dataBlock, (int) byteOffset, n, length);
            break;
        case Uint8x16:
            SIMDStoreUint8x16(dataBlock, (int) byteOffset, n, length);
            break;
        case Bool64x2:
        case Bool32x4:
        case Bool16x8:
        case Bool8x16:
        default:
            throw new AssertionError();
        }
    }

    private static void SIMDStoreFloat64x2(ByteBuffer dataBlock, int byteOffset, SIMDValue n, int length) {
        for (int i = 0; i < length; ++i) {
            int offset = byteOffset + i * SIMDType.Float64x2.getElementSize();
            double value = n.asDouble()[i];
            SerializeFloat64(dataBlock, offset, value);
        }
    }

    private static void SIMDStoreFloat32x4(ByteBuffer dataBlock, int byteOffset, SIMDValue n, int length) {
        for (int i = 0; i < length; ++i) {
            int offset = byteOffset + i * SIMDType.Float32x4.getElementSize();
            double value = n.asDouble()[i];
            SerializeFloat32(dataBlock, offset, value);
        }
    }

    private static void SIMDStoreInt32x4(ByteBuffer dataBlock, int byteOffset, SIMDValue n, int length) {
        for (int i = 0; i < length; ++i) {
            int offset = byteOffset + i * SIMDType.Int32x4.getElementSize();
            int value = n.asInt()[i];
            SerializeInt32(dataBlock, offset, value);
        }
    }

    private static void SIMDStoreInt16x8(ByteBuffer dataBlock, int byteOffset, SIMDValue n, int length) {
        for (int i = 0; i < length; ++i) {
            int offset = byteOffset + i * SIMDType.Int16x8.getElementSize();
            int value = n.asInt()[i];
            SerializeInt16(dataBlock, offset, value);
        }
    }

    private static void SIMDStoreInt8x16(ByteBuffer dataBlock, int byteOffset, SIMDValue n, int length) {
        for (int i = 0; i < length; ++i) {
            int offset = byteOffset + i * SIMDType.Int8x16.getElementSize();
            int value = n.asInt()[i];
            SerializeInt8(dataBlock, offset, value);
        }
    }

    private static void SIMDStoreUint32x4(ByteBuffer dataBlock, int byteOffset, SIMDValue n, int length) {
        for (int i = 0; i < length; ++i) {
            int offset = byteOffset + i * SIMDType.Uint32x4.getElementSize();
            int value = n.asInt()[i];
            SerializeUint32(dataBlock, offset, value);
        }
    }

    private static void SIMDStoreUint16x8(ByteBuffer dataBlock, int byteOffset, SIMDValue n, int length) {
        for (int i = 0; i < length; ++i) {
            int offset = byteOffset + i * SIMDType.Uint16x8.getElementSize();
            int value = n.asInt()[i];
            SerializeUint16(dataBlock, offset, value);
        }
    }

    private static void SIMDStoreUint8x16(ByteBuffer dataBlock, int byteOffset, SIMDValue n, int length) {
        for (int i = 0; i < length; ++i) {
            int offset = byteOffset + i * SIMDType.Uint8x16.getElementSize();
            int value = n.asInt()[i];
            SerializeUint8(dataBlock, offset, value);
        }
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDStoreInTypedArray( tarray, index, descriptor, n [, length] )
     * 
     * @param cx
     *            the execution context
     * @param tarray
     *            the typed array
     * @param index
     *            the write index
     * @param descriptor
     *            the SIMD type descriptor
     * @param n
     *            the SIMD value
     * @param method
     *            the method name
     * @return the input SIMD value
     */
    public static SIMDValue SIMDStoreInTypedArray(ExecutionContext cx, Object tarray, Object index, SIMDType descriptor,
            SIMDValue n, String method) {
        return SIMDStoreInTypedArray(cx, tarray, index, descriptor, n, descriptor.getVectorLength(), method);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDStoreInTypedArray( tarray, index, descriptor, n [, length] )
     * 
     * @param cx
     *            the execution context
     * @param tarray
     *            the typed array
     * @param index
     *            the write index
     * @param descriptor
     *            the SIMD type descriptor
     * @param n
     *            the SIMD value
     * @param length
     *            the write length
     * @param method
     *            the method name
     * @return the input SIMD value
     */
    public static SIMDValue SIMDStoreInTypedArray(ExecutionContext cx, Object tarray, Object index, SIMDType descriptor,
            SIMDValue n, int length, String method) {
        /* step 1 */
        // FIXME: spec bug - incorrect variable name in spec
        if (n.getType() != descriptor) {
            throw newTypeError(cx, Messages.Key.SIMDInvalidType);
        }
        /* step 4 */
        // FIXME: spec bug - missing type check Type(tarray) = Object
        // FIXME: spec bug - type check at wrong position
        if (!(tarray instanceof TypedArrayObject)) {
            throw newTypeError(cx, Messages.Key.SIMDInvalidObject, descriptor.name() + method,
                    Type.of(tarray).toString());
        }
        TypedArrayObject typedArray = (TypedArrayObject) tarray;
        /* step 2 */
        assert 0 < length && length <= descriptor.getVectorLength();
        // FIXME: spec bug - missing ToNumber call
        // FIXME: ToNumber(index) comparing against ToLength(ToNumber(index)) does not match any ES2015 pattern.
        double numIndex = ToNumber(cx, index);
        /* step 3 */
        if (IsDetachedBuffer(typedArray.getBuffer())) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* step 5 */
        // FIXME: spec issue - allow shared array buffers?
        ByteBuffer block = typedArray.getBuffer().getData();
        /* step 6 */
        if (numIndex != ToLength(numIndex)) {
            throw newTypeError(cx, Messages.Key.InvalidByteOffset);
        }
        /* steps 7-8 */
        // FIXME: spec issue - should use typedArray.[[TypedArrayName]] and retrieve element size from table 49.
        // FIXME: spec issue - rename elementLength to elementSize to match ES2015.
        long byteIndex = typedArray.getElementType().toBytes((long) numIndex);
        /* step 9 */
        if (byteIndex < 0 || byteIndex + (descriptor.getElementSize() * length) > typedArray.getByteLength()) {
            throw newRangeError(cx, Messages.Key.InvalidByteOffset);
        }
        /* step 10 */
        // FIXME: spec bug - wrong variable name `simd` -> `n`
        SIMDStore(block, descriptor, byteIndex, n, length);
        /* step 11 */
        return n;
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDReinterpretCast( value, newDescriptor )
     * 
     * @param cx
     *            the execution context
     * @param value
     *            the SIMD value
     * @param newDescriptor
     *            the new SIMD type descriptor
     * @return the new SIMD value
     */
    public static SIMDValue SIMDReinterpretCast(ExecutionContext cx, SIMDValue value, SIMDType newDescriptor) {
        /* step 1 */
        assert value.getType().getVectorLength() * value.getType().getElementSize() == newDescriptor.getVectorLength()
                * newDescriptor.getElementSize();
        /* step 2 */
        int bytes = newDescriptor.getVectorLength() * newDescriptor.getElementSize();
        /* steps 3-4 */
        ByteBuffer block = CreateByteDataBlock(cx, bytes);
        /* step 5 */
        // FIXME: spec bug - invalid parameters for SIMDStore
        SIMDStore(block, value.getType(), 0, value);
        /* step 6 */
        return SIMDLoad(block, newDescriptor, 0);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDBoolType( descriptor )
     * 
     * @param descriptor
     *            the SIMD type descriptor
     * @return the boolean SIMD type descriptor
     */
    public static SIMDType SIMDBoolType(SIMDType descriptor) {
        /* step 1 */
        // FIXME: spec bug - either test for == 16 or add multiplicator (*8).
        assert descriptor.getVectorLength() * descriptor.getElementSize() * 8 == 128;
        /* step 2 */
        int length = descriptor.getVectorLength();
        if (length == 2) {
            return SIMDType.Bool64x2;
        }
        /* step 3 */
        if (length == 4) {
            return SIMDType.Bool32x4;
        }
        /* step 4 */
        if (length == 8) {
            return SIMDType.Bool16x8;
        }
        /* step 5 */
        assert length == 16;
        /* step 6 */
        return SIMDType.Bool8x16;
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDRelationalOp( a, b, op )
     * 
     * @param a
     *            the first operand
     * @param b
     *            the second operand
     * @param op
     *            the binary operator
     * @return the new SIMD value
     */
    public static SIMDValue SIMDRelationalOpFloat(SIMDValue a, SIMDValue b, DoubleBiPredicate op) {
        /* step 1 */
        SIMDType outputDescriptor = SIMDBoolType(a.getType());
        /* step 2 */
        return SIMDBinaryOpFloat(a, b, op, outputDescriptor);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDRelationalOp( a, b, op )
     * 
     * @param a
     *            the first operand
     * @param b
     *            the second operand
     * @param op
     *            the binary operator
     * @return the new SIMD value
     */
    public static SIMDValue SIMDRelationalOpDouble(SIMDValue a, SIMDValue b, DoubleBiPredicate op) {
        /* step 1 */
        SIMDType outputDescriptor = SIMDBoolType(a.getType());
        /* step 2 */
        return SIMDBinaryOpDouble(a, b, op, outputDescriptor);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * SIMDRelationalOp( a, b, op )
     * 
     * @param a
     *            the first operand
     * @param b
     *            the second operand
     * @param op
     *            the binary operator
     * @return the new SIMD value
     */
    public static SIMDValue SIMDRelationalOpInt(SIMDValue a, SIMDValue b, IntBiPredicate op) {
        /* step 1 */
        SIMDType outputDescriptor = SIMDBoolType(a.getType());
        /* step 2 */
        return SIMDBinaryOpInt(a, b, op, outputDescriptor);
    }

    /**
     * Converts the input value to a 32-bit floating point number.
     * 
     * @param cx
     *            the execution context
     * @param v
     *            the input value
     * @return the 32-bit floating point number
     */
    public static float ToFloat32(ExecutionContext cx, Object v) {
        return (float) ToNumber(cx, v);
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * ArrayJoin( array, separator )
     * 
     * @param cx
     *            the execution context
     * @param array
     *            the array object
     * @param separator
     *            the separator string
     * @return the result string
     */
    public static String ArrayJoin(ExecutionContext cx, Object array, Object separator) {
        /* steps 1-2 */
        ScriptObject o = ToObject(cx, array);
        /* steps 3-4 */
        long len = ToLength(cx, Get(cx, o, "length"));
        /* step 5 */
        if (Type.isUndefined(separator)) {
            separator = "";
        }
        /* steps 6-7 */
        String sep = ToFlatString(cx, separator);
        /* step 8 */
        if (len == 0) {
            return "";
        }
        StringBuilder r = new StringBuilder();
        /* step 9 */
        Object element0 = Get(cx, o, 0);
        /* steps 10-11 */
        if (!Type.isUndefinedOrNull(element0)) {
            r.append(AbstractOperations.ToString(cx, element0));
        }
        /* steps 12-13 */
        for (int k = 1; k < len; ++k) {
            /* step 13.a */
            r.append(sep);
            /* step 13.b */
            Object element = Get(cx, o, k);
            /* steps 13.c-e */
            if (!Type.isUndefinedOrNull(element)) {
                r.append(AbstractOperations.ToString(cx, element));
            }
        }
        /* step 14 */
        return r.toString();
    }

    /**
     * Internal algorithms on SIMD types
     * <p>
     * ArrayJoin( array, separator )
     * 
     * @param array
     *            the string array
     * @param separator
     *            the string separator
     * @return the result string
     */
    static String ArrayJoin(CharSequence[] array, String separator) {
        /* steps 1-14 */
        return Arrays.stream(array).collect(Collectors.joining(separator));
    }

    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * SIMD.Float32x4
         */
        @Value(name = "Float32x4")
        public static final Intrinsics Float32x4 = Intrinsics.SIMD_Float32x4;

        /**
         * SIMD.Int32x4
         */
        @Value(name = "Int32x4")
        public static final Intrinsics Int32x4 = Intrinsics.SIMD_Int32x4;

        /**
         * SIMD.Int16x8
         */
        @Value(name = "Int16x8")
        public static final Intrinsics Int16x8 = Intrinsics.SIMD_Int16x8;

        /**
         * SIMD.Int32x4
         */
        @Value(name = "Int8x16")
        public static final Intrinsics Int8x16 = Intrinsics.SIMD_Int8x16;

        /**
         * SIMD.Uint32x4
         */
        @Value(name = "Uint32x4")
        public static final Intrinsics Uint32x4 = Intrinsics.SIMD_Uint32x4;

        /**
         * SIMD.Uint16x8
         */
        @Value(name = "Uint16x8")
        public static final Intrinsics Uint16x8 = Intrinsics.SIMD_Uint16x8;

        /**
         * SIMD.Uint32x4
         */
        @Value(name = "Uint8x16")
        public static final Intrinsics Uint8x16 = Intrinsics.SIMD_Uint8x16;

        /**
         * SIMD.Bool32x4
         */
        @Value(name = "Bool32x4")
        public static final Intrinsics Bool32x4 = Intrinsics.SIMD_Bool32x4;

        /**
         * SIMD.Bool16x8
         */
        @Value(name = "Bool16x8")
        public static final Intrinsics Bool16x8 = Intrinsics.SIMD_Bool16x8;

        /**
         * SIMD.Bool32x4
         */
        @Value(name = "Bool8x16")
        public static final Intrinsics Bool8x16 = Intrinsics.SIMD_Bool8x16;
    }

    /**
     * Extension: SIMD.Float64x2 and SIMD.Bool64x2
     */
    @CompatibilityExtension(CompatibilityOption.SIMD_Phase2)
    public enum AdditionalProperties {
        ;

        /**
         * SIMD.Float64x2
         */
        @Value(name = "Float64x2")
        public static final Intrinsics Float64x2 = Intrinsics.SIMD_Float64x2;

        /**
         * SIMD.Bool64x2
         */
        @Value(name = "Bool64x2")
        public static final Intrinsics Bool64x2 = Intrinsics.SIMD_Bool64x2;
    }
}
