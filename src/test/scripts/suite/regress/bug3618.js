/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertTrue, fail
} = Assert;

// 9.4.3.2 [[HasProperty]]: Don't traverse prototype chain for string indices
// https://bugs.ecmascript.org/show_bug.cgi?id=3618


var s = new String("abc");

var failOnTrap = false;
Object.setPrototypeOf(s, new Proxy({}, new Proxy({}, {
  get(t, pk, r) {
    if (failOnTrap) {
      fail `unexpected call to trap '${pk}'`;
    }
    return Reflect.get(t, pk, r);
  }
})));
failOnTrap = true;

assertSame("a", s[0]);
assertSame("b", s[1]);
assertSame("c", s[2]);

assertTrue(0 in s);
assertTrue(1 in s);
assertTrue(2 in s);
