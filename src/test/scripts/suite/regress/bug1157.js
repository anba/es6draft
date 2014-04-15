/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertTrue, assertFalse
} = Assert;

// 15.14.5.2: Map.prototype.clear interacts badly with Map.prototype.forEach
// https://bugs.ecmascript.org/show_bug.cgi?id=1157

{
  let calledOnce = false;
  let m = new Map([for (c of "abc") [c, c.toUpperCase()]]);
  assertSame(3, m.size);
  m.forEach((v, k) => {
    assertFalse(calledOnce);
    calledOnce = true;
    assertSame("a", k);
    assertSame("A", v);
    m.clear();
  });
  assertTrue(calledOnce);
}

{
  let calledOnce = false;
  let s = new Set(new String("abc"));
  assertSame(3, s.size);
  s.forEach(v => {
    assertFalse(calledOnce);
    calledOnce = true;
    assertSame("a", v);
    s.clear();
  });
  assertTrue(calledOnce);
}
