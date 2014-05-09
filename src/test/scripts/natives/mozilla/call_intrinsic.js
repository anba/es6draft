/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

assertEq(%Intrinsic("Object"), Object);

assertEq(%GlobalThis(), this);

// Create a new, blank realm

class R extends Reflect.Realm {
  get initGlobal() {}
}

s = Object.create(null);

r = new R({}, {
  defineProperty(t, pk, d) {
    // print(`def=${Object(pk).toString()}`);
    Reflect.defineProperty(s, pk, d);
    if (d.configurable) {
      return true;
    }
    return Reflect.defineProperty(t, pk, d);
  }
});


// 18.3  Constructor Properties of the Global Object

assertEq(s.Array, %IntrinsicFrom("Array", r));
assertEq(s.ArrayBuffer, %IntrinsicFrom("ArrayBuffer", r));
assertEq(s.Boolean, %IntrinsicFrom("Boolean", r));
assertEq(s.DataView, %IntrinsicFrom("DataView", r));
assertEq(s.Date, %IntrinsicFrom("Date", r));
assertEq(s.Error, %IntrinsicFrom("Error", r));
assertEq(s.EvalError, %IntrinsicFrom("EvalError", r));
assertEq(s.Float32Array, %IntrinsicFrom("Float32Array", r));
assertEq(s.Float64Array, %IntrinsicFrom("Float64Array", r));
assertEq(s.Function, %IntrinsicFrom("Function", r));
assertEq(s.Int8Array, %IntrinsicFrom("Int8Array", r));
assertEq(s.Int16Array, %IntrinsicFrom("Int16Array", r));
assertEq(s.Int32Array, %IntrinsicFrom("Int32Array", r));
assertEq(s.Map, %IntrinsicFrom("Map", r));
assertEq(s.Number, %IntrinsicFrom("Number", r));
assertEq(s.Object, %IntrinsicFrom("Object", r));
assertEq(s.RangeError, %IntrinsicFrom("RangeError", r));
assertEq(s.ReferenceError, %IntrinsicFrom("ReferenceError", r));
assertEq(s.RegExp, %IntrinsicFrom("RegExp", r));
assertEq(s.Set, %IntrinsicFrom("Set", r));
assertEq(s.String, %IntrinsicFrom("String", r));
assertEq(s.Symbol, %IntrinsicFrom("Symbol", r));
assertEq(s.SyntaxError, %IntrinsicFrom("SyntaxError", r));
assertEq(s.TypeError, %IntrinsicFrom("TypeError", r));
assertEq(s.Uint8Array, %IntrinsicFrom("Uint8Array", r));
assertEq(s.Uint8ClampedArray, %IntrinsicFrom("Uint8ClampedArray", r));
assertEq(s.Uint16Array, %IntrinsicFrom("Uint16Array", r));
assertEq(s.Uint32Array, %IntrinsicFrom("Uint32Array", r));
assertEq(s.URIError, %IntrinsicFrom("URIError", r));
assertEq(s.WeakMap, %IntrinsicFrom("WeakMap", r));
assertEq(s.WeakSet, %IntrinsicFrom("WeakSet", r));

// 18.4  Other Properties of the Global Object
assertEq(s.JSON, %IntrinsicFrom("JSON", r));
assertEq(s.Math, %IntrinsicFrom("Math", r));
// TODO: See fixme in proxy.js
// assertEq(s.Proxy, %IntrinsicFrom("Proxy", r));
assertEq(s.WeakSet, %IntrinsicFrom("WeakSet", r));
assertEq(s.Reflect, %IntrinsicFrom("Reflect", r));
assertEq(s.System, %IntrinsicFrom("System", r));

// ECMA - 402
assertEq(s.Intl, %IntrinsicFrom("Intl", r));
