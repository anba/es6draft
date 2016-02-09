/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

{
  let log = "";
  let p = new Proxy(Object.preventExtensions(Object.defineProperties({}, {
    a: {configurable: true},
    b: {configurable: false},
  })), {
    getOwnPropertyDescriptor(t, pk) {
      log += pk;
      return Object.getOwnPropertyDescriptor(t, pk);
    }
  });
  Object.isSealed(p);
  assertSame("a", log);
}
