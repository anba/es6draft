/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertUndefined, assertEquals, assertCallable, assertDataProperty
} = Assert;

// B.3.1  __proto__ Property Names in Object Initializers

assertSame(null, Object.getPrototypeOf({__proto__: null}));
assertSame(Array.prototype, Object.getPrototypeOf({__proto__: Array.prototype}));

assertSame(null, Object.getPrototypeOf({"__proto__": null}));
assertSame(Array.prototype, Object.getPrototypeOf({"__proto__": Array.prototype}));

assertSame(null, Object.getPrototypeOf({'__proto__': null}));
assertSame(Array.prototype, Object.getPrototypeOf({'__proto__': Array.prototype}));

// Not installed as data property
assertUndefined(Object.getOwnPropertyDescriptor({__proto__: null}, "__proto__"));
assertUndefined(Object.getOwnPropertyDescriptor({__proto__: Array.prototype}, "__proto__"));

assertUndefined(Object.getOwnPropertyDescriptor({"__proto__": null}, "__proto__"));
assertUndefined(Object.getOwnPropertyDescriptor({"__proto__": Array.prototype}, "__proto__"));

assertUndefined(Object.getOwnPropertyDescriptor({'__proto__': null}, "__proto__"));
assertUndefined(Object.getOwnPropertyDescriptor({'__proto__': Array.prototype}, "__proto__"));


// No effect on getters, setters, methods, computed property keys
assertSame(Object.prototype, Object.getPrototypeOf({get __proto__() { throw new Error("unexpected") }}));
assertSame(Object.prototype, Object.getPrototypeOf({set __proto__(v) { throw new Error("unexpected") }}));
assertSame(Object.prototype, Object.getPrototypeOf({__proto__() { throw new Error("unexpected") }}));
assertSame(Object.prototype, Object.getPrototypeOf({*__proto__() { throw new Error("unexpected") }}));
assertSame(Object.prototype, Object.getPrototypeOf({["__proto__"]: Array.prototype}));

// Still installed as normal property
function assertAccessorWithFunction(o, pk, field) {
  let desc = Object.getOwnPropertyDescriptor(o, pk);
  assertCallable(desc[field]);
  assertEquals({enumerable: true, configurable: true, get: void 0, set: void 0, [field]: desc[field]}, desc);
}
assertAccessorWithFunction({get __proto__() { throw new Error("unexpected") }}, "__proto__", "get");
assertAccessorWithFunction({set __proto__(v) { throw new Error("unexpected") }}, "__proto__", "set");

function assertDataWithFunction(o, pk) {
  let desc = Object.getOwnPropertyDescriptor(o, pk);
  assertCallable(desc.value);
  assertEquals({writable: true, enumerable: true, configurable: true, value: desc.value}, desc);
}
assertDataWithFunction({__proto__() { throw new Error("unexpected") }}, "__proto__");
assertDataWithFunction({*__proto__() { throw new Error("unexpected") }}, "__proto__");

assertDataProperty({["__proto__"]: Array.prototype}, "__proto__", {writable: true, enumerable: true, configurable: true, value: Array.prototype});
