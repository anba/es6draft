/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame,
  assertNotConstructor,
  assertNotCallable,
  assertDataProperty,
} = Assert;
const global = this;

function assertConstProperty(object, name, value = object[name]) {
  return assertDataProperty(object, name, {value, writable: false, enumerable: false, configurable: false});
}

function assertValueProperty(object, name, value = object[name]) {
  return assertDataProperty(object, name, {value, writable: true, enumerable: false, configurable: true});
}

function assertFunctionProperty(object, name, value = object[name]) {
  return assertDataProperty(object, name, {value, writable: true, enumerable: false, configurable: true});
}

function assertConstructorProperty(object, name, value = object[name]) {
  return assertDataProperty(object, name, {value, writable: true, enumerable: false, configurable: true});
}

/* 18  The Global Object */

assertSame("object", typeof global);
assertNotConstructor(global);
assertNotCallable(global);


/* 18.1  Value Properties of the Global Object */

assertConstProperty(global, "Infinity", 1 / 0);
assertConstProperty(global, "NaN", 0 / 0);
assertConstProperty(global, "undefined", void 0);


/* 18.2  Function Properties of the Global Object */

assertFunctionProperty(global, "eval");
assertFunctionProperty(global, "isFinite");
assertFunctionProperty(global, "isNaN");
assertFunctionProperty(global, "parseFloat");
assertFunctionProperty(global, "parseInt");


/* 18.3  URI Handling Function Properties */

assertFunctionProperty(global, "decodeURI");
assertFunctionProperty(global, "decodeURIComponent");
assertFunctionProperty(global, "encodeURI");
assertFunctionProperty(global, "encodeURIComponent");


/* 18.4  Constructor Properties of the Global Object */

assertConstructorProperty(global, "Array");
assertConstructorProperty(global, "ArrayBuffer");
assertConstructorProperty(global, "Boolean");
assertConstructorProperty(global, "DataView");
assertConstructorProperty(global, "Date");
assertConstructorProperty(global, "Error");
assertConstructorProperty(global, "EvalError");
assertConstructorProperty(global, "Float32Array");
assertConstructorProperty(global, "Float64Array");
assertConstructorProperty(global, "Function");
assertConstructorProperty(global, "Int8Array");
assertConstructorProperty(global, "Int16Array");
assertConstructorProperty(global, "Int32Array");
assertConstructorProperty(global, "Map");
assertConstructorProperty(global, "Number");
assertConstructorProperty(global, "Object");
assertConstructorProperty(global, "RangeError");
assertConstructorProperty(global, "ReferenceError");
assertConstructorProperty(global, "RegExp");
assertConstructorProperty(global, "Set");
assertConstructorProperty(global, "String");
assertConstructorProperty(global, "Symbol");
assertConstructorProperty(global, "SyntaxError");
assertConstructorProperty(global, "TypeError");
assertConstructorProperty(global, "Uint8Array");
assertConstructorProperty(global, "Uint8ClampedArray");
assertConstructorProperty(global, "Uint16Array");
assertConstructorProperty(global, "Uint32Array");
assertConstructorProperty(global, "URIError");
assertConstructorProperty(global, "WeakMap");
assertConstructorProperty(global, "WeakSet");


/* 18.5  Other Properties of the Global Object */

assertValueProperty(global, "JSON");
assertValueProperty(global, "Math");
assertValueProperty(global, "Proxy");
assertValueProperty(global, "Reflect");
