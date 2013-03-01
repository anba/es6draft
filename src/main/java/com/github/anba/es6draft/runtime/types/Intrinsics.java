/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types;

/**
 * <h1>8 Types</h1><br>
 * <h2>8.1 ECMAScript Language Types</h2><br>
 * <h3>8.1.6 The Object Type</h3>
 * <ul>
 * <li>8.1.6.3 Well-Known Symbols and Intrinsics
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
    Map,
    MapPrototype,
    MapIteratorPrototype,
    WeakMap,
    WeakMapPrototype,
    Set,
    SetPrototype,
    SetIteratorPrototype,
    StopIteration,

    // not yet (?) in spec
    String,
    StringPrototype,
    Boolean,
    BooleanPrototype,
    Number,
    NumberPrototype,
    Math,
    Date,
    DatePrototype,
    RegExp,
    RegExpPrototype,
    Error,
    ErrorPrototype,
    JSON,

    // not yet (?) in spec
    ArrayBuffer,
    ArrayBufferPrototype,
    Int8Array,
    Int8ArrayPrototype,
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
    DataView,
    DataViewPrototype,

    // Internationalization API
    Intl,
    Intl_Collator,
    Intl_CollatorPrototype,
    Intl_NumberFormat,
    Intl_NumberFormatPrototype,
    Intl_DateTimeFormat,
    Intl_DateTimeFormatPrototype,

    // internal
    ListIteratorPrototype,

    /* @formatter:on */
}
