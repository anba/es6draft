/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types;

/**
 * <h1>6 ECMAScript Data Types and Values</h1><br>
 * <h2>6.1 ECMAScript Language Types</h2><br>
 * <h3>6.1.7 The Object Type</h3>
 * <ul>
 * <li>6.1.7.4 Well-Known Intrinsic Objects
 * </ul>
 */
public enum Intrinsics {/* @formatter:off */
    /** Intrinsic: <code>%Array%</code> */
    Array,
    /** Intrinsic: <code>%ArrayBuffer%</code> */
    ArrayBuffer,
    /** Intrinsic: <code>%ArrayBufferPrototype%</code> */
    ArrayBufferPrototype,
    /** Intrinsic: <code>%ArrayIteratorPrototype%</code> */
    ArrayIteratorPrototype,
    /** Intrinsic: <code>%ArrayPrototype%</code> */
    ArrayPrototype,
    /** Intrinsic: <code>%ArrayProto_values%</code> */
    ArrayProto_values,
    /** Intrinsic: <code>%Boolean%</code> */
    Boolean,
    /** Intrinsic: <code>%BooleanPrototype%</code> */
    BooleanPrototype,
    /** Intrinsic: <code>%DataView%</code> */
    DataView,
    /** Intrinsic: <code>%DataViewPrototype%</code> */
    DataViewPrototype,
    /** Intrinsic: <code>%Date%</code> */
    Date,
    /** Intrinsic: <code>%DatePrototype%</code> */
    DatePrototype,
    /** Intrinsic: <code>%decodeURI%</code> */
    decodeURI,
    /** Intrinsic: <code>%decodeURIComponent%</code> */
    decodeURIComponent,
    /** Intrinsic: <code>%encodeURI%</code> */
    encodeURI,
    /** Intrinsic: <code>%encodeURIComponent%</code> */
    encodeURIComponent,
    /** Intrinsic: <code>%Error%</code> */
    Error,
    /** Intrinsic: <code>%ErrorPrototype%</code> */
    ErrorPrototype,
    /** Intrinsic: <code>%escape%</code> */
    escape,
    /** Intrinsic: <code>%eval%</code> */
    eval,
    /** Intrinsic: <code>%EvalError%</code> */
    EvalError,
    /** Intrinsic: <code>%EvalErrorPrototype%</code> */
    EvalErrorPrototype,
    /** Intrinsic: <code>%Float32Array%</code> */
    Float32Array,
    /** Intrinsic: <code>%Float32ArrayPrototype%</code> */
    Float32ArrayPrototype,
    /** Intrinsic: <code>%Float64Array%</code> */
    Float64Array,
    /** Intrinsic: <code>%Float64ArrayPrototype%</code> */
    Float64ArrayPrototype,
    /** Intrinsic: <code>%Function%</code> */
    Function,
    /** Intrinsic: <code>%FunctionPrototype%</code> */
    FunctionPrototype,
    /** Intrinsic: <code>%Generator%</code> */
    Generator,
    /** Intrinsic: <code>%GeneratorFunction%</code> */
    GeneratorFunction,
    /** Intrinsic: <code>%GeneratorPrototype%</code> */
    GeneratorPrototype,
    /** Intrinsic: <code>%Int8Array%</code> */
    Int8Array,
    /** Intrinsic: <code>%Int8ArrayPrototype%</code> */
    Int8ArrayPrototype,
    /** Intrinsic: <code>%Int16Array%</code> */
    Int16Array,
    /** Intrinsic: <code>%Int16ArrayPrototype%</code> */
    Int16ArrayPrototype,
    /** Intrinsic: <code>%Int32Array%</code> */
    Int32Array,
    /** Intrinsic: <code>%Int32ArrayPrototype%</code> */
    Int32ArrayPrototype,
    /** Intrinsic: <code>%isFinite%</code> */
    isFinite,
    /** Intrinsic: <code>%isNaN%</code> */
    isNaN,
    /** Intrinsic: <code>%IteratorPrototype%</code> */
    IteratorPrototype,
    /** Intrinsic: <code>%JSON%</code> */
    JSON,
    /** Intrinsic: <code>%Map%</code> */
    Map,
    /** Intrinsic: <code>%MapPrototype%</code> */
    MapPrototype,
    /** Intrinsic: <code>%MapIteratorPrototype%</code> */
    MapIteratorPrototype,
    /** Intrinsic: <code>%Math%</code> */
    Math,
    /** Intrinsic: <code>%Number%</code> */
    Number,
    /** Intrinsic: <code>%NumberPrototype%</code> */
    NumberPrototype,
    /** Intrinsic: <code>%Object%</code> */
    Object,
    /** Intrinsic: <code>%ObjectPrototype%</code> */
    ObjectPrototype,
    /** Intrinsic: <code>%ObjProto_toString%</code> */
    ObjProto_toString,
    /** Intrinsic: <code>%parseFloat%</code> */
    parseFloat,
    /** Intrinsic: <code>%parseInt%</code> */
    parseInt,
    /** Intrinsic: <code>%Promise%</code> */
    Promise,
    /** Intrinsic: <code>%PromisePrototype%</code> */
    PromisePrototype,
    /** Intrinsic: <code>%Proxy%</code> */
    Proxy,
    /** Intrinsic: <code>%RangeError%</code> */
    RangeError,
    /** Intrinsic: <code>%RangeErrorPrototype%</code> */
    RangeErrorPrototype,
    /** Intrinsic: <code>%ReferenceError%</code> */
    ReferenceError,
    /** Intrinsic: <code>%ReferenceErrorPrototype%</code> */
    ReferenceErrorPrototype,
    /** Intrinsic: <code>%Reflect%</code> */
    Reflect,
    /** Intrinsic: <code>%RegExp%</code> */
    RegExp,
    /** Intrinsic: <code>%RegExpPrototype%</code> */
    RegExpPrototype,
    /** Intrinsic: <code>%Set%</code> */
    Set,
    /** Intrinsic: <code>%SetIteratorPrototype%</code> */
    SetIteratorPrototype,
    /** Intrinsic: <code>%SetPrototype%</code> */
    SetPrototype,
    /** Intrinsic: <code>%String%</code> */
    String,
    /** Intrinsic: <code>%StringIteratorPrototype%</code> */
    StringIteratorPrototype,
    /** Intrinsic: <code>%StringPrototype%</code> */
    StringPrototype,
    /** Intrinsic: <code>%Symbol%</code> */
    Symbol,
    /** Intrinsic: <code>%SymbolPrototype%</code> */
    SymbolPrototype,
    /** Intrinsic: <code>%SyntaxError%</code> */
    SyntaxError,
    /** Intrinsic: <code>%SyntaxErrorPrototype%</code> */
    SyntaxErrorPrototype,
    /** Intrinsic: <code>%ThrowTypeError%</code> */
    ThrowTypeError,
    /** Intrinsic: <code>%TypedArray%</code> */
    TypedArray,
    /** Intrinsic: <code>%TypedArrayPrototype%</code> */
    TypedArrayPrototype,
    /** Intrinsic: <code>%TypeError%</code> */
    TypeError,
    /** Intrinsic: <code>%TypeErrorPrototype%</code> */
    TypeErrorPrototype,
    /** Intrinsic: <code>%Uint8Array%</code> */
    Uint8Array,
    /** Intrinsic: <code>%Uint8ArrayPrototype%</code> */
    Uint8ArrayPrototype,
    /** Intrinsic: <code>%Uint8ClampedArray%</code> */
    Uint8ClampedArray,
    /** Intrinsic: <code>%Uint8ClampedArrayPrototype%</code> */
    Uint8ClampedArrayPrototype,
    /** Intrinsic: <code>%Uint16Array%</code> */
    Uint16Array,
    /** Intrinsic: <code>%Uint16ArrayPrototype%</code> */
    Uint16ArrayPrototype,
    /** Intrinsic: <code>%Uint32Array%</code> */
    Uint32Array,
    /** Intrinsic: <code>%Uint32ArrayPrototype%</code> */
    Uint32ArrayPrototype,
    /** Intrinsic: <code>%unescape%</code> */
    unescape,
    /** Intrinsic: <code>%URIError%</code> */
    URIError,
    /** Intrinsic: <code>%URIErrorPrototype%</code> */
    URIErrorPrototype,
    /** Intrinsic: <code>%WeakMap%</code> */
    WeakMap,
    /** Intrinsic: <code>%WeakMapPrototype%</code> */
    WeakMapPrototype,
    /** Intrinsic: <code>%WeakSet%</code> */
    WeakSet,
    /** Intrinsic: <code>%WeakSetPrototype%</code> */
    WeakSetPrototype,

    // Internationalization API
    /** Intrinsic: <code>%Intl%</code> */
    Intl,
    /** Intrinsic: <code>%Intl_Collator%</code> */
    Intl_Collator,
    /** Intrinsic: <code>%Intl_CollatorPrototype%</code> */
    Intl_CollatorPrototype,
    /** Intrinsic: <code>%Intl_NumberFormat%</code> */
    Intl_NumberFormat,
    /** Intrinsic: <code>%Intl_NumberFormatPrototype%</code> */
    Intl_NumberFormatPrototype,
    /** Intrinsic: <code>%Intl_DateTimeFormat%</code> */
    Intl_DateTimeFormat,
    /** Intrinsic: <code>%Intl_DateTimeFormatPrototype%</code> */
    Intl_DateTimeFormatPrototype,

    // internal
    /** Intrinsic: <code>%InternalError%</code> */
    InternalError,
    /** Intrinsic: <code>%InternalErrorPrototype%</code> */
    InternalErrorPrototype,

    // legacy
    /** Intrinsic: <code>%LegacyGeneratorPrototype%</code> */
    LegacyGeneratorPrototype,

    // Extension: Async Function Definitions
    /** Intrinsic: <code>%AsyncFunction%</code> */
    AsyncFunction,
    /** Intrinsic: <code>%AsyncFunctionPrototype%</code> */
    AsyncFunctionPrototype,

    // Extension: Realm Objects
    /** Intrinsic: <code>%Realm%</code> */
    Realm,
    /** Intrinsic: <code>%RealmPrototype%</code> */
    RealmPrototype,

    // Extension: Loader
    /** Intrinsic: <code>%Loader%</code> */
    Loader,
    /** Intrinsic: <code>%LoaderPrototype%</code> */
    LoaderPrototype,
    /** Intrinsic: <code>%LoaderIteratorPrototype%</code> */
    LoaderIteratorPrototype,
    /** Intrinsic: <code>%System%</code> */
    System,

    // Extension: String.prototype.matchAll
    RegExpStringIteratorPrototype,

    // Extension: SIMD
    /** Intrinsic: %SIMD% */
    SIMD,
    /** Intrinsic: %SIMD_Float32x4% */
    SIMD_Float32x4,
    /** Intrinsic: %SIMD_Float32x4Prototype% */
    SIMD_Float32x4Prototype,
    /** Intrinsic: %SIMD_Int32x4% */
    SIMD_Int32x4,
    /** Intrinsic: %SIMD_Int32x4Prototype% */
    SIMD_Int32x4Prototype,
    /** Intrinsic: %SIMD_Int16x8% */
    SIMD_Int16x8,
    /** Intrinsic: %SIMD_Int16x8Prototype% */
    SIMD_Int16x8Prototype,
    /** Intrinsic: %SIMD_Int8x16% */
    SIMD_Int8x16,
    /** Intrinsic: %SIMD_Int8x16Prototype% */
    SIMD_Int8x16Prototype,
    /** Intrinsic: %SIMD_Uint32x4% */
    SIMD_Uint32x4,
    /** Intrinsic: %SIMD_Uint32x4Prototype% */
    SIMD_Uint32x4Prototype,
    /** Intrinsic: %SIMD_Uint16x8% */
    SIMD_Uint16x8,
    /** Intrinsic: %SIMD_Uint16x8Prototype% */
    SIMD_Uint16x8Prototype,
    /** Intrinsic: %SIMD_Uint8x16% */
    SIMD_Uint8x16,
    /** Intrinsic: %SIMD_Uint8x16Prototype% */
    SIMD_Uint8x16Prototype,
    /** Intrinsic: %SIMD_Bool32x4% */
    SIMD_Bool32x4,
    /** Intrinsic: %SIMD_Bool32x4Prototype% */
    SIMD_Bool32x4Prototype,
    /** Intrinsic: %SIMD_Bool16x8% */
    SIMD_Bool16x8,
    /** Intrinsic: %SIMD_Bool16x8Prototype% */
    SIMD_Bool16x8Prototype,
    /** Intrinsic: %SIMD_Bool8x16% */
    SIMD_Bool8x16,
    /** Intrinsic: %SIMD_Bool8x16Prototype% */
    SIMD_Bool8x16Prototype,

    // Extension: SIMD.Float64x2, SIMD.Bool64x2
    /** Intrinsic: %SIMD_Float64x2% */
    SIMD_Float64x2,
    /** Intrinsic: %SIMD_Float64x2Prototype% */
    SIMD_Float64x2Prototype,
    /** Intrinsic: %SIMD_Bool64x2% */
    SIMD_Bool64x2,
    /** Intrinsic: %SIMD_Bool64x2Prototype% */
    SIMD_Bool64x2Prototype,

    /* @formatter:on */
    ;

    /**
     * Returns the intrinsic key.
     * 
     * @return the intrinsic key
     */
    public String getKey() {
        return name();
    }

    /**
     * Returns {@code true} if the intrinsic is internal.
     * 
     * @return {@code true} if internal intrinsic
     */
    public boolean isInternal() {
        switch (this) {
        case InternalError:
        case InternalErrorPrototype:
        case LegacyGeneratorPrototype:
            return true;
        default:
            return false;
        }
    }
}
