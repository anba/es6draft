/**
 * Copyright (c) 2012-2013 André Bargull
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
 * <li>6.1.7.4 Well-Known Symbols and Intrinsics
 * </ul>
 */
public enum Intrinsics {/* @formatter:off */
    Object,
    ObjectPrototype,
    ObjProto_toString,
    Function,
    FunctionPrototype,
    Array,
    ArrayPrototype,
    ArrayIteratorPrototype,
    String,
    StringPrototype,
    Boolean,
    BooleanPrototype,
    Number,
    NumberPrototype,
    Date,
    DatePrototype,
    RegExp,
    RegExpPrototype,
    Map,
    MapPrototype,
    MapIteratorPrototype,
    WeakMap,
    WeakMapPrototype,
    Set,
    SetPrototype,
    SetIteratorPrototype,
    WeakSet,
    WeakSetPrototype,
    GeneratorFunction,
    Generator,
    GeneratorPrototype,
    Error,
    EvalError,
    RangeError,
    ReferenceError,
    SyntaxError,
    TypeError,
    URIError,
    ErrorPrototype,
    EvalErrorPrototype,
    RangeErrorPrototype,
    ReferenceErrorPrototype,
    SyntaxErrorPrototype,
    TypeErrorPrototype,
    URIErrorPrototype,

    // not yet (?) in spec
    StringIteratorPrototype,
    Symbol,
    SymbolPrototype,
    Math,
    JSON,
    Proxy,
    Reflect,

    // binary module
    ArrayBuffer,
    ArrayBufferPrototype,
    TypedArray,
    TypedArrayPrototype,
    Int8Array,
    Int8ArrayPrototype,
    DataView,
    DataViewPrototype,

    // not yet (?) in spec
    Uint8Array,
    Uint8ArrayPrototype,
    Uint8ClampedArray,
    Uint8ClampedArrayPrototype,
    Int16Array,
    Int16ArrayPrototype,
    Uint16Array,
    Uint16ArrayPrototype,
    Int32Array,
    Int32ArrayPrototype,
    Uint32Array,
    Uint32ArrayPrototype,
    Float32Array,
    Float32ArrayPrototype,
    Float64Array,
    Float64ArrayPrototype,

    // Internationalization API
    Intl,
    Intl_Collator,
    Intl_CollatorPrototype,
    Intl_NumberFormat,
    Intl_NumberFormatPrototype,
    Intl_DateTimeFormat,
    Intl_DateTimeFormatPrototype,

    // internal
    ListIteratorNext,
    InternalError,
    InternalErrorPrototype,

    // legacy
    LegacyGeneratorPrototype,

    /* @formatter:on */
}
