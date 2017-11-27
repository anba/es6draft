/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertEquals,
  fail,
} = Assert;

// 21.2.5.7 RegExp.prototype.replace: Don't call ToString() if captured group is undefined
// https://bugs.ecmascript.org/show_bug.cgi?id=2779

// Functional replace
{
  class RE extends RegExp {
    get global() {
      return true;
    }

    exec(s) {
      if (this.lastIndex === 0) {
        this.lastIndex = 3;
        return Object.assign(["abc", void 0, {toString(){ return "def" }}], {index: 0});
      }
      assertSame(3, this.lastIndex);
      this.lastIndex = 0;
      return null;
    }
  }
  let c = 0;
  let r = new RE();
  r[Symbol.replace]("ABCDEF", (m, ...rest) => {
    assertSame(0, r.lastIndex);
    switch (c++) {
    case 0:  
      assertSame("abc", m);
      assertEquals([void 0, "def", 0, "ABCDEF"], rest);
      break;
    default:
      fail `Too many matches`;
    }
  });
  assertSame(1, c);
  assertSame(0, r.lastIndex);
}

// String replace
{
  class RE extends RegExp {
    get global() {
      return true;
    }

    exec(s) {
      if (this.lastIndex === 0) {
        this.lastIndex = 3;
        return Object.assign(["abc", void 0, {toString(){ return "def" }}], {index: 0});
      }
      assertSame(3, this.lastIndex);
      this.lastIndex = 0;
      return null;
    }
  }
  let r = new RE();
  let s = r[Symbol.replace]("ABCDEF", "|$1|$2|");
  assertSame("||def|DEF", s);
  assertSame(0, r.lastIndex);
}
