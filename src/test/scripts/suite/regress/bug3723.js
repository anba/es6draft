/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertFalse, assertTrue, fail
} = Assert;

// 21.2.3.1 RegExp ( pattern, flags ): steps 6.c and d must be run only if the flags argument is undefined
// https://bugs.ecmascript.org/show_bug.cgi?id=3723

function makeRegExpLike() {
  return {
    [Symbol.match]: true,
    sourceCalled: false,
    flagsCalled: false,
    get source() {
      assertFalse(this.sourceCalled);
      this.sourceCalled = true;
      return "";
    },
    get flags() {
      assertFalse(this.flagsCalled);
      this.flagsCalled = true;
      return "";
    }
  };
}

var regExpLike = makeRegExpLike();
RegExp(regExpLike);
assertTrue(regExpLike.sourceCalled);
assertTrue(regExpLike.flagsCalled);

var regExpLike = makeRegExpLike();
RegExp(regExpLike, "");
assertTrue(regExpLike.sourceCalled);
assertFalse(regExpLike.flagsCalled);
