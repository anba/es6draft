/**
 * Copyright (c) 2012-2014 André Bargull
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
    ObjectPrototype,
    ThrowTypeError,
    FunctionPrototype,
    Object,
    ObjProto_toString,
    Function,
    Array,
    ArrayPrototype,
    ArrayProto_values,
    ArrayIteratorPrototype,
    String,
    StringPrototype,
    StringIteratorPrototype,
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
    Symbol,
    SymbolPrototype,
    Math,
    JSON,
    Proxy,
    Reflect,
    System,

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

    // Promise Objects
    Promise,
    PromisePrototype,

    // Loader, Module, Realm Objects
    Loader,
    LoaderPrototype,
    LoaderIteratorPrototype,
    Realm,
    RealmPrototype,

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
    CompoundIteratorNext,
    InternalError,
    InternalErrorPrototype,

    // legacy
    LegacyGeneratorPrototype,

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
        case ListIteratorNext:
        case CompoundIteratorNext:
        case InternalError:
        case InternalErrorPrototype:
        case LegacyGeneratorPrototype:
            return true;
        default:
            return false;
        }
    }
}
