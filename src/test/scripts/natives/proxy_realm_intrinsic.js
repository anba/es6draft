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

// Create a new, blank realm

let stdlib = Object.create(null);
let realm = new Reflect.Realm({}, {
  defineProperty(t, pk, d) {
    Reflect.defineProperty(stdlib, pk, d);
    if (d.configurable) {
      return true;
    }
    return Reflect.defineProperty(t, pk, d);
  }
});
let intrinsics = realm.intrinsics;

assertEq(Object.getOwnPropertyNames(realm.global).length, ["Infinity", "NaN", "undefined"].length);
assertEq(Object.getOwnPropertySymbols(realm.global).length, 0);


// 18.3  Constructor Properties of the Global Object

assertEq(stdlib.Array, intrinsics.Array);
assertEq(stdlib.ArrayBuffer, intrinsics.ArrayBuffer);
assertEq(stdlib.Boolean, intrinsics.Boolean);
assertEq(stdlib.DataView, intrinsics.DataView);
assertEq(stdlib.Date, intrinsics.Date);
assertEq(stdlib.Error, intrinsics.Error);
assertEq(stdlib.EvalError, intrinsics.EvalError);
assertEq(stdlib.Float32Array, intrinsics.Float32Array);
assertEq(stdlib.Float64Array, intrinsics.Float64Array);
assertEq(stdlib.Function, intrinsics.Function);
assertEq(stdlib.Int8Array, intrinsics.Int8Array);
assertEq(stdlib.Int16Array, intrinsics.Int16Array);
assertEq(stdlib.Int32Array, intrinsics.Int32Array);
assertEq(stdlib.Map, intrinsics.Map);
assertEq(stdlib.Number, intrinsics.Number);
assertEq(stdlib.Object, intrinsics.Object);
assertEq(stdlib.RangeError, intrinsics.RangeError);
assertEq(stdlib.ReferenceError, intrinsics.ReferenceError);
assertEq(stdlib.RegExp, intrinsics.RegExp);
assertEq(stdlib.Set, intrinsics.Set);
assertEq(stdlib.String, intrinsics.String);
assertEq(stdlib.Symbol, intrinsics.Symbol);
assertEq(stdlib.SyntaxError, intrinsics.SyntaxError);
assertEq(stdlib.TypeError, intrinsics.TypeError);
assertEq(stdlib.Uint8Array, intrinsics.Uint8Array);
assertEq(stdlib.Uint8ClampedArray, intrinsics.Uint8ClampedArray);
assertEq(stdlib.Uint16Array, intrinsics.Uint16Array);
assertEq(stdlib.Uint32Array, intrinsics.Uint32Array);
assertEq(stdlib.URIError, intrinsics.URIError);
assertEq(stdlib.WeakMap, intrinsics.WeakMap);
assertEq(stdlib.WeakSet, intrinsics.WeakSet);

// 18.4  Other Properties of the Global Object
assertEq(stdlib.JSON, intrinsics.JSON);
assertEq(stdlib.Math, intrinsics.Math);
// TODO: See fixme in proxy.js
// assertEq(stdlib.Proxy, intrinsics.Proxy);
assertEq(stdlib.Reflect, intrinsics.Reflect);
assertEq(stdlib.System, intrinsics.System);

// ECMA - 402
assertEq(stdlib.Intl, intrinsics.Intl);
