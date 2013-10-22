/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame,
  assertNotConstructor,
  assertNotCallable,
  assertDataProperty,
  assertAccessorProperty,
} = Assert;
const global = this;

/* 18  The Global Object */

assertSame("object", typeof global);
assertNotConstructor(global);
assertNotCallable(global);


/* 18.1  Value Properties of the Global Object */

assertDataProperty(global, "Infinity", {value: 1 / 0, writable: false, enumerable: false, configurable: false});
assertDataProperty(global, "NaN", {value: 0 / 0, writable: false, enumerable: false, configurable: false});
assertDataProperty(global, "undefined", {value: void 0, writable: false, enumerable: false, configurable: false});


/* 18.2  Function Properties of the Global Object */

assertDataProperty(global, "eval", {value: global.eval, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "isFinite", {value: global.isFinite, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "isNaN", {value: global.isNaN, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "parseFloat", {value: global.parseFloat, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "parseInt", {value: global.parseInt, writable: true, enumerable: false, configurable: true});


/* 18.3  URI Handling Function Properties */

assertDataProperty(global, "decodeURI", {value: global.decodeURI, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "decodeURIComponent", {value: global.decodeURIComponent, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "encodeURI", {value: global.encodeURI, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "encodeURIComponent", {value: global.encodeURIComponent, writable: true, enumerable: false, configurable: true});


/* 18.4  Constructor Properties of the Global Object */

assertDataProperty(global, "Array", {value: global.Array, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "ArrayBuffer", {value: global.ArrayBuffer, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "Boolean", {value: global.Boolean, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "DataView", {value: global.DataView, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "Date", {value: global.Date, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "Error", {value: global.Error, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "EvalError", {value: global.EvalError, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "Float32Array", {value: global.Float32Array, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "Float64Array", {value: global.Float64Array, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "Function", {value: global.Function, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "Int8Array", {value: global.Int8Array, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "Int16Array", {value: global.Int16Array, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "Int32Array", {value: global.Int32Array, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "Map", {value: global.Map, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "Number", {value: global.Number, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "Object", {value: global.Object, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "RangeError", {value: global.RangeError, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "ReferenceError", {value: global.ReferenceError, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "RegExp", {value: global.RegExp, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "Set", {value: global.Set, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "String", {value: global.String, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "SyntaxError", {value: global.SyntaxError, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "TypeError", {value: global.TypeError, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "Uint8Array", {value: global.Uint8Array, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "Uint8ClampedArray", {value: global.Uint8ClampedArray, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "Uint16Array", {value: global.Uint16Array, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "Uint32Array", {value: global.Uint32Array, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "URIError", {value: global.URIError, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "WeakMap", {value: global.WeakMap, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "WeakSet", {value: global.WeakSet, writable: true, enumerable: false, configurable: true});


/* 18.5  Other Properties of the Global Object */

assertDataProperty(global, "JSON", {value: global.JSON, writable: true, enumerable: false, configurable: true});
assertDataProperty(global, "Math", {value: global.Math, writable: true, enumerable: false, configurable: true});
