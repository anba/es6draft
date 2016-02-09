/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertFalse, assertSame, fail
} = Assert;

// 9.1.9 [[Set]]: super.prop assignment can silently overwrite non-writable properties
// https://bugs.ecmascript.org/show_bug.cgi?id=4331

for (let configurable of [true, false]) {
  let p = {prop: 0};
  let o = Object.defineProperty({},
    "prop", {
      value: 123,
      writable: false,
      configurable,
    }
  );
  assertFalse(Reflect.set(p, "prop", 456, o))
  assertSame(123, o.prop);
  assertSame(0, p.prop);
}

for (let configurable of [true, false]) {
  let p = {prop: 0};
  let o = Object.defineProperty({},
    "prop", {
      get() { return 123 },
      set() { fail `unreachable` },
      configurable,
    }
  );
  assertFalse(Reflect.set(p, "prop", 456, o))
  assertSame(123, o.prop);
  assertSame(0, p.prop);
}
