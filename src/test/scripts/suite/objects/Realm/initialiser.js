/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertEquals
} = Assert;

const standardProperties = [
"NaN",
"Infinity",
"undefined",
"parseFloat",
"parseInt",
"isNaN",
"isFinite",
"eval",
"decodeURIComponent",
"encodeURIComponent",
"decodeURI",
"encodeURI",
"Object",
"Function",
"Array",
"String",
"Symbol",
"Boolean",
"Number",
"Date",
"RegExp",
"Error",
"EvalError",
"RangeError",
"ReferenceError",
"SyntaxError",
"TypeError",
"URIError",
"Map",
"WeakMap",
"Set",
"WeakSet",
"ArrayBuffer",
"Int8Array",
"Uint8Array",
"Uint8ClampedArray",
"Int16Array",
"Uint16Array",
"Int32Array",
"Uint32Array",
"Float32Array",
"Float64Array",
"DataView",
"Promise",
"JSON",
"Math",
"Proxy",
"Reflect",
"System",
"Intl",
"SIMD",
"escape",
"unescape",
];

const nonStandardProperties = [
"InternalError",
];

// Default global created if no arguments given
{
  let realm = new Reflect.Realm();

  // Default [[Prototype]] is %Object.prototype%
  assertSame(realm.global.Object.prototype, Object.getPrototypeOf(realm.global));

  // Test keys and own property names
  assertEquals(Object.keys(this), Object.keys(realm.global));
  assertEquals([...standardProperties, ...nonStandardProperties].sort(), Object.getOwnPropertyNames(realm.global).sort());
  assertEquals([], Object.getOwnPropertySymbols(realm.global));
}

// Empty global created if "initGlobal" is overridden
{
  let realm = new class extends Reflect.Realm {
    initGlobal() { }
  };

  // [[Prototype]] is set to %ObjectPrototype%
  assertSame(realm.stdlib.Object.value.prototype, Object.getPrototypeOf(realm.global));

  // No own property names present
  assertSame(0, Object.keys(realm.global).length);
  assertSame(0, Object.getOwnPropertyNames(realm.global).length);
  assertSame(0, Object.getOwnPropertySymbols(realm.global).length);
}

// Test arguments passed to "initGlobal" method
{
  let thisValue;
  let realm = new class extends Reflect.Realm {
    initGlobal(...args) {
      thisValue = this;
      assertSame(0, args.length);
    }
  };
  assertSame(realm, thisValue);
}
