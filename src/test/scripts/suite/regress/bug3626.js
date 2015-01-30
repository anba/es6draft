/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 14.4.12 EvaluateBody: Allow "new function*" to reference `this`
// https://bugs.ecmascript.org/show_bug.cgi?id=3626

function* g() {
  yield this;
}

var it = new g();
var {value} = it.next();
assertSame(it, value);
