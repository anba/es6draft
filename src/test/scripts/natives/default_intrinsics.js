/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
%Include("lib/assert.js");

const globalThis = %GlobalThis();
assertEq(globalThis, this);


// 18.3  Constructor Properties of the Global Object

assertEq(Array, %Intrinsic("Array"));
assertEq(ArrayBuffer, %Intrinsic("ArrayBuffer"));
assertEq(Boolean, %Intrinsic("Boolean"));
assertEq(DataView, %Intrinsic("DataView"));
assertEq(Date, %Intrinsic("Date"));
assertEq(Error, %Intrinsic("Error"));
assertEq(EvalError, %Intrinsic("EvalError"));
assertEq(Float32Array, %Intrinsic("Float32Array"));
assertEq(Float64Array, %Intrinsic("Float64Array"));
assertEq(Function, %Intrinsic("Function"));
assertEq(Int8Array, %Intrinsic("Int8Array"));
assertEq(Int16Array, %Intrinsic("Int16Array"));
assertEq(Int32Array, %Intrinsic("Int32Array"));
assertEq(Map, %Intrinsic("Map"));
assertEq(Number, %Intrinsic("Number"));
assertEq(Object, %Intrinsic("Object"));
assertEq(RangeError, %Intrinsic("RangeError"));
assertEq(ReferenceError, %Intrinsic("ReferenceError"));
assertEq(RegExp, %Intrinsic("RegExp"));
assertEq(Set, %Intrinsic("Set"));
assertEq(String, %Intrinsic("String"));
assertEq(Symbol, %Intrinsic("Symbol"));
assertEq(SyntaxError, %Intrinsic("SyntaxError"));
assertEq(TypeError, %Intrinsic("TypeError"));
assertEq(Uint8Array, %Intrinsic("Uint8Array"));
assertEq(Uint8ClampedArray, %Intrinsic("Uint8ClampedArray"));
assertEq(Uint16Array, %Intrinsic("Uint16Array"));
assertEq(Uint32Array, %Intrinsic("Uint32Array"));
assertEq(URIError, %Intrinsic("URIError"));
assertEq(WeakMap, %Intrinsic("WeakMap"));
assertEq(WeakSet, %Intrinsic("WeakSet"));

// 18.4  Other Properties of the Global Object
assertEq(JSON, %Intrinsic("JSON"));
assertEq(Math, %Intrinsic("Math"));
// TODO: See fixme in proxy.js
// assertEq(Proxy, %Intrinsic("Proxy"));
assertEq(Reflect, %Intrinsic("Reflect"));
assertEq(System, %Intrinsic("System"));

// ECMA - 402
assertEq(Intl, %Intrinsic("Intl"));
