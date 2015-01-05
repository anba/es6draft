/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertEquals,
  fail,
} = Assert;

// 21.2.5.7 RegExp.prototype.replace: Restrict position to [0, s.length]
// https://bugs.ecmascript.org/show_bug.cgi?id=2780

{
  class RE extends RegExp {
    get global() {
      return true;
    }

    exec(s) {
      if (this.lastIndex === 0) {
        this.lastIndex = 3;
        return Object.assign(["abc"], {index: -100});
      }
      if (this.lastIndex === 3) {
        this.lastIndex = 6;
        return Object.assign(["def"], {index: +100});
      }
      assertSame(6, this.lastIndex);
      this.lastIndex = 0;
      return null;
    }
  }
  let c = 0;
  let string = "ABCDEF";
  let r = new RE();
  r[Symbol.replace](string, (m, p, s) => {
    assertSame(0, r.lastIndex);
    assertSame(string, s);
    switch (c++) {
    case 0:
      assertSame("abc", m);
      assertSame(0, p);
      break;
    case 1:
      assertSame("def", m);
      assertSame(string.length, p);
      break;
    default:
      fail `Too many matches`;
    }
  });
  assertSame(2, c);
  assertSame(0, r.lastIndex);
}
