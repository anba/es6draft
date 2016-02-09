/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertEquals
} = Assert;

// 21.2.5.2.3 AdvanceStringIndex: Invalid assertion, ≤ instead of <
// https://bugs.ecmascript.org/show_bug.cgi?id=4234

class RE extends RegExp {
  exec(input) {
    if (this.lastIndex === 0) {
      this.lastIndex = Number.MAX_SAFE_INTEGER;
      return Object.assign([""], {input, index: 0});
    }
    assertSame(Number.MAX_SAFE_INTEGER + 1, this.lastIndex);
    return null;
  }
}

assertEquals([""], new RE("", "g")[Symbol.match](""));
