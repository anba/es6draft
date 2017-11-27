/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 19.2.3.1 Function.prototype.apply: Missing PrepareForTailCall in step 2
// https://bugs.ecmascript.org/show_bug.cgi?id=4511

function returnCaller() {
  return returnCaller.caller;
}

function test() {
  assertSame(test, returnCaller.apply());
  assertSame(test, returnCaller.apply(void 0));
  assertSame(test, returnCaller.apply(void 0, void 0));
  assertSame(test, returnCaller.apply(void 0, null));

  assertSame(test, function(){ "use strict"; return returnCaller.apply(); }());
  assertSame(test, function(){ "use strict"; return returnCaller.apply(void 0); }());
  assertSame(test, function(){ "use strict"; return returnCaller.apply(void 0, void 0); }());
  assertSame(test, function(){ "use strict"; return returnCaller.apply(void 0, null); }());
}
test();
