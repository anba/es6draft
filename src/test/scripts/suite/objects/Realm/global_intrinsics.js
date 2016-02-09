/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

const globalObject = this;
const { intrinsics } = System.realm;

// 18.3  Constructor Properties of the Global Object
assertSame(globalObject.Array, intrinsics.Array);
assertSame(globalObject.ArrayBuffer, intrinsics.ArrayBuffer);
assertSame(globalObject.Boolean, intrinsics.Boolean);
assertSame(globalObject.DataView, intrinsics.DataView);
assertSame(globalObject.Date, intrinsics.Date);
assertSame(globalObject.Error, intrinsics.Error);
assertSame(globalObject.EvalError, intrinsics.EvalError);
assertSame(globalObject.Float32Array, intrinsics.Float32Array);
assertSame(globalObject.Float64Array, intrinsics.Float64Array);
assertSame(globalObject.Function, intrinsics.Function);
assertSame(globalObject.Int8Array, intrinsics.Int8Array);
assertSame(globalObject.Int16Array, intrinsics.Int16Array);
assertSame(globalObject.Int32Array, intrinsics.Int32Array);
assertSame(globalObject.Map, intrinsics.Map);
assertSame(globalObject.Number, intrinsics.Number);
assertSame(globalObject.Object, intrinsics.Object);
assertSame(globalObject.RangeError, intrinsics.RangeError);
assertSame(globalObject.ReferenceError, intrinsics.ReferenceError);
assertSame(globalObject.RegExp, intrinsics.RegExp);
assertSame(globalObject.Set, intrinsics.Set);
assertSame(globalObject.String, intrinsics.String);
assertSame(globalObject.Symbol, intrinsics.Symbol);
assertSame(globalObject.SyntaxError, intrinsics.SyntaxError);
assertSame(globalObject.TypeError, intrinsics.TypeError);
assertSame(globalObject.Uint8Array, intrinsics.Uint8Array);
assertSame(globalObject.Uint8ClampedArray, intrinsics.Uint8ClampedArray);
assertSame(globalObject.Uint16Array, intrinsics.Uint16Array);
assertSame(globalObject.Uint32Array, intrinsics.Uint32Array);
assertSame(globalObject.URIError, intrinsics.URIError);
assertSame(globalObject.WeakMap, intrinsics.WeakMap);
assertSame(globalObject.WeakSet, intrinsics.WeakSet);

// 18.4  Other Properties of the Global Object
assertSame(globalObject.JSON, intrinsics.JSON);
assertSame(globalObject.Math, intrinsics.Math);
assertSame(globalObject.Proxy, intrinsics.Proxy);
assertSame(globalObject.Reflect, intrinsics.Reflect);
assertSame(globalObject.System, intrinsics.System);
