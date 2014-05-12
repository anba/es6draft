/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

if (typeof assertEq === 'undefined') {
  assertEq = function assertEq(actual, expected, message = "Not same") {
    if (actual !== expected) {
      throw new Error(`Assertion failed: got ${actual}, expected ${expected}`);
    }
  }
}

const globalObject = %GlobalObject();
const globalThis = %GlobalThis();

assertEq(globalThis, this);
if (globalThis === globalObject) {
  assertEq(globalObject, {});
}

// 18.3  Constructor Properties of the Global Object

assertEq(globalObject.Array, %Intrinsic("Array"));
assertEq(globalObject.ArrayBuffer, %Intrinsic("ArrayBuffer"));
assertEq(globalObject.Boolean, %Intrinsic("Boolean"));
assertEq(globalObject.DataView, %Intrinsic("DataView"));
assertEq(globalObject.Date, %Intrinsic("Date"));
assertEq(globalObject.Error, %Intrinsic("Error"));
assertEq(globalObject.EvalError, %Intrinsic("EvalError"));
assertEq(globalObject.Float32Array, %Intrinsic("Float32Array"));
assertEq(globalObject.Float64Array, %Intrinsic("Float64Array"));
assertEq(globalObject.Function, %Intrinsic("Function"));
assertEq(globalObject.Int8Array, %Intrinsic("Int8Array"));
assertEq(globalObject.Int16Array, %Intrinsic("Int16Array"));
assertEq(globalObject.Int32Array, %Intrinsic("Int32Array"));
assertEq(globalObject.Map, %Intrinsic("Map"));
assertEq(globalObject.Number, %Intrinsic("Number"));
assertEq(globalObject.Object, %Intrinsic("Object"));
assertEq(globalObject.RangeError, %Intrinsic("RangeError"));
assertEq(globalObject.ReferenceError, %Intrinsic("ReferenceError"));
assertEq(globalObject.RegExp, %Intrinsic("RegExp"));
assertEq(globalObject.Set, %Intrinsic("Set"));
assertEq(globalObject.String, %Intrinsic("String"));
assertEq(globalObject.Symbol, %Intrinsic("Symbol"));
assertEq(globalObject.SyntaxError, %Intrinsic("SyntaxError"));
assertEq(globalObject.TypeError, %Intrinsic("TypeError"));
assertEq(globalObject.Uint8Array, %Intrinsic("Uint8Array"));
assertEq(globalObject.Uint8ClampedArray, %Intrinsic("Uint8ClampedArray"));
assertEq(globalObject.Uint16Array, %Intrinsic("Uint16Array"));
assertEq(globalObject.Uint32Array, %Intrinsic("Uint32Array"));
assertEq(globalObject.URIError, %Intrinsic("URIError"));
assertEq(globalObject.WeakMap, %Intrinsic("WeakMap"));
assertEq(globalObject.WeakSet, %Intrinsic("WeakSet"));

// 18.4  Other Properties of the Global Object
assertEq(globalObject.JSON, %Intrinsic("JSON"));
assertEq(globalObject.Math, %Intrinsic("Math"));
// TODO: See fixme in proxy.js
// assertEq(globalObject.Proxy, %Intrinsic("Proxy"));
assertEq(globalObject.Reflect, %Intrinsic("Reflect"));
assertEq(globalObject.System, %Intrinsic("System"));

// ECMA - 402
assertEq(globalObject.Intl, %Intrinsic("Intl"));
