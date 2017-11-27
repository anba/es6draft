/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertUndefined, assertThrows, fail
} = Assert;

// Simple class to extend any object with private state.
let e = class Ext extends function(o) { return o; } {
  #private;
  constructor(o) { super(o); }
  static init(o) { return new Ext(o); }
  static get(o) { return o.#private; }
  static set(o, v) { o.#private = v; }
};

// Double initialization throws TypeError
assertThrows(TypeError, () => e.init(e.init({})));

// Access without initialization throws TypeError
assertThrows(TypeError, () => e.get({}));
assertThrows(TypeError, () => e.set({}, 0));

// Access without initialization throws TypeError (primitive values)
for (let primitive of [void 0, null, 42, true, "hi", Symbol()]) {
  assertThrows(TypeError, () => e.get(primitive));
  assertThrows(TypeError, () => e.set(primitive, 0));
}

// Test with various objects.
for (let obj of [{}, [], /(?:)/, new Date, Object.preventExtensions({}), Object.seal({}), Object.freeze({})]) {
  assertSame(obj, e.init(obj));
  assertUndefined(e.get(obj));
  e.set(obj, 123);
  assertSame(123, e.get(obj));
}

// Test private fields are not shared with proxy target.
{
  let obj = {};
  let proxy = new Proxy(obj, new Proxy({}, {
    get(t, pk) { fail `trap=${pk} invoked`; }
  }));

  assertThrows(TypeError, () => e.get(obj));
  assertThrows(TypeError, () => e.get(proxy));

  e.init(proxy);

  assertThrows(TypeError, () => e.get(obj));
  assertUndefined(e.get(proxy));

  let value = {};
  e.set(proxy, value);

  assertSame(value, e.get(proxy));
}
