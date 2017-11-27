/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 10.2.1.2.2, 10.2.1.4.16, 10.2.1.4.17: Invalid assertions
// https://bugs.ecmascript.org/show_bug.cgi?id=1786

function triggerAssertion(global, callCounter, code) {
  Object.setPrototypeOf(global, new Proxy(Object.create(null), {
    has(t, pk) {
      if (pk == "foo" && callCounter > 0 && --callCounter == 0) {
        Object.preventExtensions(global);
        return true;
      }
      return false;
    }
  }));
  (1,eval)(`eval("${code}")`)
}

// Note: No longer reproducible without Reflect.Realm

// 10.2.1.2.2, step 3
// triggerAssertion(this, 4, "var foo = 0");
// assertSame(0, foo);
