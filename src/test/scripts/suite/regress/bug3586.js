/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals
} = Assert;

// 7.3.15 TestIntegrityLevel (O, level):, step 11: Should one examine all keys even if the result is known?
// https://bugs.ecmascript.org/show_bug.cgi?id=3586

for (let testFn of [Object.isSealed, Object.isFrozen]) {
  let log = [];
  let proxy = new Proxy({a: 0, b: 0}, {
    getOwnPropertyDescriptor(t, pk) {
      log.push(pk);
      return Reflect.getOwnPropertyDescriptor(t, pk);
    }
  });
  Object.preventExtensions(proxy);

  testFn(proxy);
  assertEquals(["a"], log);
}
