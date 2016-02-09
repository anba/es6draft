/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// assignment to const variable don't throw in non-strict code
// https://bugs.ecmascript.org/show_bug.cgi?id=3253

function nonStrictMode() {
  const c = 0;
  // Enclose in with-statement so it's not possible to statically determine if
  // 'c' refers to the const binding.
  with ({}) {
    assertThrows(TypeError, () => {
      c = 1;
    });
  }
  assertSame(0, c);
}
nonStrictMode();

function nonStrictModeLegacy() {
  // Enclose in with-statement so it's not possible to statically determine if
  // 'c' refers to the const binding.
  (function c() {
    var expected = c;
    with ({}) {
      c = 1;
    }
    assertSame(expected, c);
  })();
}
nonStrictModeLegacy();

function strictMode() {
  const c = 0;
  // Enclose in with-statement so it's not possible to statically determine if
  // 'c' refers to the const binding.
  with ({}) {
    assertThrows(TypeError, () => {
      "use strict";
      c = 1;
    });
  }
  assertSame(0, c);
}
strictMode();

function strictModeLegacy() {
  // Enclose in with-statement so it's not possible to statically determine if
  // 'c' refers to the const binding.
  (function c() {
    var expected = c;
    with ({}) {
      assertThrows(TypeError, () => {
        "use strict";
        c = 1;
      });
    }
    assertSame(expected, c);
  })();
}
strictModeLegacy();
