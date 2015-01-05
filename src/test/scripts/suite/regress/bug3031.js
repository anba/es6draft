/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 13.6.4.8 ForIn/OfBodyEvaluation, 14.6.2.1 Statement Rules: Disable tail-call in for-in/of loops
// https://bugs.ecmascript.org/show_bug.cgi?id=3031

{
  function returnCaller() { return returnCaller.caller }

  function testForIn() {
    function loop() {
      "use strict";
      for (var v of [0]) {
        return returnCaller();
      }
    }
    assertSame(null, loop());
  }
  testForIn();

  function testForOf() {
    function loop() {
      "use strict";
      for (var v of [0]) {
        return returnCaller();
      }
    }
    assertSame(null, loop());
  }
  testForOf();

  function testFor() {
    function loop() {
      "use strict";
      for (;;) {
        return returnCaller();
      }
    }
    assertSame(testFor, loop());
  }
  testFor();
}
