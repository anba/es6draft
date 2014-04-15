/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

// 21.2.5.7 RegExp.prototype.replace: Call replacer function after all matches were found
// https://bugs.ecmascript.org/show_bug.cgi?id=2617

{
  let r = /x/g;
  let c = 0;
  '0x2x4x6x8'.replace(r, () => {
    c += 1;
    assertSame(0, r.lastIndex);
  });
  assertSame(4, c);
}

{
  let r = /test/g;
  let s = "test-string".replace(r, () => {
    r.lastIndex = 0;
    return "a";
  });
  assertSame("a-string", s);
}
