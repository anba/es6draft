/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertEquals,
  fail,
} = Assert;

// 21.2.5.7 RegExp.prototype.replace: Step 3 is invalid, nCaptures needs to be determined dynamically
// https://bugs.ecmascript.org/show_bug.cgi?id=2778

// Functional replace
{
  class RE extends RegExp {
    get global() {
      return true;
    }

    exec(s) {
      if (this.lastIndex === 0) {
        this.lastIndex = 3;
        return Object.assign(["abc", "def", "ghi"], {index: 0});
      }
      if (this.lastIndex === 3) {
        this.lastIndex = 6;
        return Object.assign(["jkl", "mno"], {index: 3});
      }
      assertSame(6, this.lastIndex);
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
      assertEquals(["def", "ghi", 0, "ABCDEF"], rest);
      break;
    case 1:
      assertSame("jkl", m);
      assertEquals(["mno", 3, "ABCDEF"], rest);
      break;
    default:
      fail `Too many matches`;
    }
  });
  assertSame(2, c);
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
        return Object.assign(["abc", "def", "ghi"], {index: 0});
      }
      if (this.lastIndex === 3) {
        this.lastIndex = 6;
        return Object.assign(["jkl", "mno"], {index: 3});
      }
      assertSame(6, this.lastIndex);
      this.lastIndex = 0;
      return null;
    }
  }
  let r = new RE();
  let s = r[Symbol.replace]("ABCDEF", "|$&,$`,$',$1,$2");
  assertSame("|abc,,DEF,def,ghi|jkl,ABC,,mno,$2", s);
  assertSame(0, r.lastIndex);
}
