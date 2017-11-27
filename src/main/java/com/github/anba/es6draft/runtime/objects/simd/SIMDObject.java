/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.simd;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>SIMD</h1>
 * <h2>SIMD objects</h2>
 * <ul>
 * <li>Properties of SIMD Instances
 * </ul>
 */
public final class SIMDObject extends OrdinaryObject {
    /** [[SIMDWrapperData]] */
    private final SIMDValue data;

    /**
     * Constructs a new SIMD object.
     * 
     * @param realm
     *            the realm object
     * @param data
     *            the SIMD data
     * @param prototype
     *            the prototype object
     */
    public SIMDObject(Realm realm, SIMDValue data, ScriptObject prototype) {
        super(realm, prototype);
        this.data = data;
    }

    /**
     * [[SIMDWrapperData]]
     * 
     * @return the wrapped SIMD value
     */
    public SIMDValue getData() {
        return data;
    }

    /**
     * Creates a new SIMD object.
     * 
     * @param cx
     *            the execution context
     * @param value
     *            the SIMD value
     * @return the new SIMD object
     */
    public static SIMDObject SIMDCreate(ExecutionContext cx, SIMDValue value) {
        return new SIMDObject(cx.getRealm(), value, cx.getIntrinsic(getProtoKey(value.getType())));
    }

    /**
     * Creates a new SIMD object.
     * 
     * @param cx
     *            the execution context
     * @param value
     *            the SIMD value
     * @param prototype
     *            the prototype object
     * @return the new SIMD object
     */
    public static SIMDObject SIMDCreate(ExecutionContext cx, SIMDValue value, ScriptObject prototype) {
        return new SIMDObject(cx.getRealm(), value, prototype);
    }

    private static Intrinsics getProtoKey(SIMDType type) {
        switch (type) {
        case Float64x2:
            return Intrinsics.SIMD_Float64x2Prototype;
        case Float32x4:
            return Intrinsics.SIMD_Float32x4Prototype;
        case Int32x4:
            return Intrinsics.SIMD_Int32x4Prototype;
        case Int16x8:
            return Intrinsics.SIMD_Int16x8Prototype;
        case Int8x16:
            return Intrinsics.SIMD_Int8x16Prototype;
        case Uint32x4:
            return Intrinsics.SIMD_Uint32x4Prototype;
        case Uint16x8:
            return Intrinsics.SIMD_Uint16x8Prototype;
        case Uint8x16:
            return Intrinsics.SIMD_Uint8x16Prototype;
        case Bool64x2:
            return Intrinsics.SIMD_Bool64x2Prototype;
        case Bool32x4:
            return Intrinsics.SIMD_Bool32x4Prototype;
        case Bool16x8:
            return Intrinsics.SIMD_Bool16x8Prototype;
        case Bool8x16:
            return Intrinsics.SIMD_Bool8x16Prototype;
        default:
            throw new AssertionError();
        }
    }

    @Override
    public String toString() {
        return String.format("%s, data=%s", super.toString(), data);
    }
}
