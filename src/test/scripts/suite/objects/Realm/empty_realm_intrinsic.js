/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// Create a new, empty realm
const realm = new class extends Reflect.Realm {
  initGlobal() {}
};
const stdlib = Object.defineProperties(Object.create(null), realm.stdlib);
const { intrinsics } = realm;

assertSame(Object.getOwnPropertyNames(realm.global).length, 0);
assertSame(Object.getOwnPropertySymbols(realm.global).length, 0);

// 18.3  Constructor Properties of the Global Object
assertSame(stdlib.Array, intrinsics.Array);
assertSame(stdlib.ArrayBuffer, intrinsics.ArrayBuffer);
assertSame(stdlib.Boolean, intrinsics.Boolean);
assertSame(stdlib.DataView, intrinsics.DataView);
assertSame(stdlib.Date, intrinsics.Date);
assertSame(stdlib.Error, intrinsics.Error);
assertSame(stdlib.EvalError, intrinsics.EvalError);
assertSame(stdlib.Float32Array, intrinsics.Float32Array);
assertSame(stdlib.Float64Array, intrinsics.Float64Array);
assertSame(stdlib.Function, intrinsics.Function);
assertSame(stdlib.Int8Array, intrinsics.Int8Array);
assertSame(stdlib.Int16Array, intrinsics.Int16Array);
assertSame(stdlib.Int32Array, intrinsics.Int32Array);
assertSame(stdlib.Map, intrinsics.Map);
assertSame(stdlib.Number, intrinsics.Number);
assertSame(stdlib.Object, intrinsics.Object);
assertSame(stdlib.RangeError, intrinsics.RangeError);
assertSame(stdlib.ReferenceError, intrinsics.ReferenceError);
assertSame(stdlib.RegExp, intrinsics.RegExp);
assertSame(stdlib.Set, intrinsics.Set);
assertSame(stdlib.String, intrinsics.String);
assertSame(stdlib.Symbol, intrinsics.Symbol);
assertSame(stdlib.SyntaxError, intrinsics.SyntaxError);
assertSame(stdlib.TypeError, intrinsics.TypeError);
assertSame(stdlib.Uint8Array, intrinsics.Uint8Array);
assertSame(stdlib.Uint8ClampedArray, intrinsics.Uint8ClampedArray);
assertSame(stdlib.Uint16Array, intrinsics.Uint16Array);
assertSame(stdlib.Uint32Array, intrinsics.Uint32Array);
assertSame(stdlib.URIError, intrinsics.URIError);
assertSame(stdlib.WeakMap, intrinsics.WeakMap);
assertSame(stdlib.WeakSet, intrinsics.WeakSet);

// 18.4  Other Properties of the Global Object
assertSame(stdlib.JSON, intrinsics.JSON);
assertSame(stdlib.Math, intrinsics.Math);
assertSame(stdlib.Proxy, intrinsics.Proxy);
assertSame(stdlib.Reflect, intrinsics.Reflect);
assertSame(stdlib.System, intrinsics.System);
