/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
// Wrapper script to provide the requested testing functions.
{
  let currentTest = "";
  Object.assign(this, {
    test(name, f) {
      "use strict";
      currentTest = name;
      return f();
    },
    fail: Assert.fail,
    equal(expected, actual) {
      "use strict";
      return Assert.assertSame(expected, actual, `${currentTest}:`);
    },
    notEqual(expected, actual) {
      "use strict";
      return Assert.assertNotSame(expected, actual, `${currentTest}:`);
    },
    throws(f) {
      "use strict";
      return Assert.assertThrows(Error, f, `${currentTest}:`);
    },
    ok(value) {
      "use strict";
      return Assert.assertTrue(!!value, `${currentTest}:`);
    },
  });
}

// Enable SIMD Phase 2 tests.
simdPhase2 = true;

loadRelativeToScript("./resources/ecmascript_simd_tests.js");
