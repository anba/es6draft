/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 14.6.1 Tail Position: ConciseBody not handled
// https://bugs.ecmascript.org/show_bug.cgi?id=2244

function returnCaller() {
  return returnCaller.caller;
}
let tailCall = (function() {
  "use strict";
  return () => returnCaller();
})();
function test() {
  assertSame(test, tailCall());
}
test();
