/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows
} = Assert;

// 9.4.4.5 [[Delete]]: Missing ReturnIfAbrupt after step 4
// https://bugs.ecmascript.org/show_bug.cgi?id=2867

function tryDeleteCaller() {
  "use strict";
  delete arguments.caller;
}
assertThrows(() => tryDeleteCaller(), TypeError);
