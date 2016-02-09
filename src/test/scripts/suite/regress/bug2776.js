/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertFalse, assertTrue, assertThrows
} = Assert;

// 21.2.5.2.1 RegExpExec ( R, S ): Add type check for result value
// https://bugs.ecmascript.org/show_bug.cgi?id=2776

for (let v of [void 0, 0, 1, 1.4, NaN, "", "abc", Symbol()]) {
  class RE extends RegExp {
    exec() { return v }
  }
  assertThrows(TypeError, () => (new RE).test(""));
}

for (let v of [null]) {
  class RE extends RegExp {
    exec() { return v }
  }
  assertFalse((new RE).test(""));
}

for (let v of [[], {}, () => {}]) {
  class RE extends RegExp {
    exec() { return v }
  }
  assertTrue((new RE).test(""));
}
