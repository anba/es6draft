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
    /** Intrinsic: <tt>%Array%</tt> */
    Array,
    /** Intrinsic: <tt>%ArrayBuffer%</tt> */
    ArrayBuffer,
    /** Intrinsic: <tt>%ArrayBufferPrototype%</tt> */
    ArrayBufferPrototype,
    /** Intrinsic: <tt>%ArrayIteratorPrototype%</tt> */
    ArrayIteratorPrototype,
    /** Intrinsic: <tt>%ArrayPrototype%</tt> */
    ArrayPrototype,
    /** Intrinsic: <tt>%ArrayProto_values%</tt> */
    ArrayProto_values,
    /** Intrinsic: <tt>%Boolean%</tt> */
    Boolean,
    /** Intrinsic: <tt>%BooleanPrototype%</tt> */
    BooleanPrototype,
    /** Intrinsic: <tt>%DataView%</tt> */
    DataView,
    /** Intrinsic: <tt>%DataViewPrototype%</tt> */
    DataViewPrototype,
    /** Intrinsic: <tt>%Date%</tt> */
    Date,
    /** Intrinsic: <tt>%DatePrototype%</tt> */
    DatePrototype,
    /** Intrinsic: <tt>%decodeURI%</tt> */
    decodeURI,
    /** Intrinsic: <tt>%decodeURIComponent%</tt> */
    decodeURIComponent,
    /** Intrinsic: <tt>%encodeURI%</tt> */
    encodeURI,
    /** Intrinsic: <tt>%encodeURIComponent%</tt> */
    encodeURIComponent,
    /** Intrinsic: <tt>%Error%</tt> */
    Error,
    /** Intrinsic: <tt>%ErrorPrototype%</tt> */
    ErrorPrototype,
    /** Intrinsic: <tt>%escape%</tt> */
    escape,
    /** Intrinsic: <tt>%eval%</tt> */
    eval,
    /** Intrinsic: <tt>%EvalError%</tt> */
    EvalError,
    /** Intrinsic: <tt>%EvalErrorPrototype%</tt> */
    EvalErrorPrototype,
    /** Intrinsic: <tt>%Float32Array%</tt> */
    Float32Array,
    /** Intrinsic: <tt>%Float32ArrayPrototype%</tt> */
    Float32ArrayPrototype,
    /** Intrinsic: <tt>%Float64Array%</tt> */
    Float64Array,
    /** Intrinsic: <tt>%Float64ArrayPrototype%</tt> */
    Float64ArrayPrototype,
    /** Intrinsic: <tt>%Function%</tt> */
    Function,
    /** Intrinsic: <tt>%FunctionPrototype%</tt> */
    FunctionPrototype,
    /** Intrinsic: <tt>%Generator%</tt> */
    Generator,
    /** Intrinsic: <tt>%GeneratorFunction%</tt> */
    GeneratorFunction,
    /** Intrinsic: <tt>%GeneratorPrototype%</tt> */
    GeneratorPrototype,
    /** Intrinsic: <tt>%Int8Array%</tt> */
    Int8Array,
    /** Intrinsic: <tt>%Int8ArrayPrototype%</tt> */
    Int8ArrayPrototype,
    /** Intrinsic: <tt>%Int16Array%</tt> */
    Int16Array,
    /** Intrinsic: <tt>%Int16ArrayPrototype%</tt> */
    Int16ArrayPrototype,
    /** Intrinsic: <tt>%Int32Array%</tt> */
    Int32Array,
    /** Intrinsic: <tt>%Int32ArrayPrototype%</tt> */
    Int32ArrayPrototype,
    /** Intrinsic: <tt>%isFinite%</tt> */
    isFinite,
    /** Intrinsic: <tt>%isNaN%</tt> */
    isNaN,
    /** Intrinsic: <tt>%IteratorPrototype%</tt> */
    IteratorPrototype,
    /** Intrinsic: <tt>%JSON%</tt> */
    JSON,
    /** Intrinsic: <tt>%Map%</tt> */
    Map,
    /** Intrinsic: <tt>%MapPrototype%</tt> */
    MapPrototype,
    /** Intrinsic: <tt>%MapIteratorPrototype%</tt> */
    MapIteratorPrototype,
    /** Intrinsic: <tt>%Math%</tt> */
    Math,
    /** Intrinsic: <tt>%Number%</tt> */
    Number,
    /** Intrinsic: <tt>%NumberPrototype%</tt> */
    NumberPrototype,
    /** Intrinsic: <tt>%Object%</tt> */
    Object,
    /** Intrinsic: <tt>%ObjectPrototype%</tt> */
    ObjectPrototype,
    /** Intrinsic: <tt>%ObjProto_toString%</tt> */
    ObjProto_toString,
    /** Intrinsic: <tt>%parseFloat%</tt> */
    parseFloat,
    /** Intrinsic: <tt>%parseInt%</tt> */
    parseInt,
    /** Intrinsic: <tt>%Promise%</tt> */
    Promise,
    /** Intrinsic: <tt>%PromisePrototype%</tt> */
    PromisePrototype,
    /** Intrinsic: <tt>%Proxy%</tt> */
    Proxy,
    /** Intrinsic: <tt>%RangeError%</tt> */
    RangeError,
    /** Intrinsic: <tt>%RangeErrorPrototype%</tt> */
    RangeErrorPrototype,
    /** Intrinsic: <tt>%ReferenceError%</tt> */
    ReferenceError,
    /** Intrinsic: <tt>%ReferenceErrorPrototype%</tt> */
    ReferenceErrorPrototype,
    /** Intrinsic: <tt>%Reflect%</tt> */
    Reflect,
    /** Intrinsic: <tt>%RegExp%</tt> */
    RegExp,
    /** Intrinsic: <tt>%RegExpPrototype%</tt> */
    RegExpPrototype,
    /** Intrinsic: <tt>%Set%</tt> */
    Set,
    /** Intrinsic: <tt>%SetIteratorPrototype%</tt> */
    SetIteratorPrototype,
    /** Intrinsic: <tt>%SetPrototype%</tt> */
    SetPrototype,
    /** Intrinsic: <tt>%String%</tt> */
    String,
    /** Intrinsic: <tt>%StringIteratorPrototype%</tt> */
    StringIteratorPrototype,
    /** Intrinsic: <tt>%StringPrototype%</tt> */
    StringPrototype,
    /** Intrinsic: <tt>%Symbol%</tt> */
    Symbol,
    /** Intrinsic: <tt>%SymbolPrototype%</tt> */
    SymbolPrototype,
    /** Intrinsic: <tt>%SyntaxError%</tt> */
    SyntaxError,
    /** Intrinsic: <tt>%SyntaxErrorPrototype%</tt> */
    SyntaxErrorPrototype,
    /** Intrinsic: <tt>%ThrowTypeError%</tt> */
    ThrowTypeError,
    /** Intrinsic: <tt>%TypedArray%</tt> */
    TypedArray,
    /** Intrinsic: <tt>%TypedArrayPrototype%</tt> */
    TypedArrayPrototype,
    /** Intrinsic: <tt>%TypeError%</tt> */
    TypeError,
    /** Intrinsic: <tt>%TypeErrorPrototype%</tt> */
    TypeErrorPrototype,
    /** Intrinsic: <tt>%Uint8Array%</tt> */
    Uint8Array,
    /** Intrinsic: <tt>%Uint8ArrayPrototype%</tt> */
    Uint8ArrayPrototype,
    /** Intrinsic: <tt>%Uint8ClampedArray%</tt> */
    Uint8ClampedArray,
    /** Intrinsic: <tt>%Uint8ClampedArrayPrototype%</tt> */
    Uint8ClampedArrayPrototype,
    /** Intrinsic: <tt>%Uint16Array%</tt> */
    Uint16Array,
    /** Intrinsic: <tt>%Uint16ArrayPrototype%</tt> */
    Uint16ArrayPrototype,
    /** Intrinsic: <tt>%Uint32Array%</tt> */
    Uint32Array,
    /** Intrinsic: <tt>%Uint32ArrayPrototype%</tt> */
    Uint32ArrayPrototype,
    /** Intrinsic: <tt>%unescape%</tt> */
    unescape,
    /** Intrinsic: <tt>%URIError%</tt> */
    URIError,
    /** Intrinsic: <tt>%URIErrorPrototype%</tt> */
    URIErrorPrototype,
    /** Intrinsic: <tt>%WeakMap%</tt> */
    WeakMap,
    /** Intrinsic: <tt>%WeakMapPrototype%</tt> */
    WeakMapPrototype,
    /** Intrinsic: <tt>%WeakSet%</tt> */
    WeakSet,
    /** Intrinsic: <tt>%WeakSetPrototype%</tt> */
    WeakSetPrototype,

    // Internationalization API
    /** Intrinsic: <tt>%Intl%</tt> */
    Intl,
    /** Intrinsic: <tt>%Intl_Collator%</tt> */
    Intl_Collator,
    /** Intrinsic: <tt>%Intl_CollatorPrototype%</tt> */
    Intl_CollatorPrototype,
    /** Intrinsic: <tt>%Intl_NumberFormat%</tt> */
    Intl_NumberFormat,
    /** Intrinsic: <tt>%Intl_NumberFormatPrototype%</tt> */
    Intl_NumberFormatPrototype,
    /** Intrinsic: <tt>%Intl_DateTimeFormat%</tt> */
    Intl_DateTimeFormat,
    /** Intrinsic: <tt>%Intl_DateTimeFormatPrototype%</tt> */
    Intl_DateTimeFormatPrototype,

    // internal
    /** Intrinsic: <tt>%InternalError%</tt> */
    InternalError,
    /** Intrinsic: <tt>%InternalErrorPrototype%</tt> */
    InternalErrorPrototype,

    // legacy
    /** Intrinsic: <tt>%LegacyGeneratorPrototype%</tt> */
    LegacyGeneratorPrototype,

    // ES7 extension: Async Function Definitions
    /** Intrinsic: <tt>%AsyncFunction%</tt> */
    AsyncFunction,
    /** Intrinsic: <tt>%AsyncFunctionPrototype%</tt> */
    AsyncFunctionPrototype,

    // ES7 extension: Realm Objects
    /** Intrinsic: <tt>%Realm%</tt> */
    Realm,
    /** Intrinsic: <tt>%RealmPrototype%</tt> */
    RealmPrototype,

    // ESx extension: Loader
    /** Intrinsic: <tt>%Loader%</tt> */
    Loader,
    /** Intrinsic: <tt>%LoaderPrototype%</tt> */
    LoaderPrototype,
    /** Intrinsic: <tt>%LoaderIteratorPrototype%</tt> */
    LoaderIteratorPrototype,
    /** Intrinsic: <tt>%System%</tt> */
    System,

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
