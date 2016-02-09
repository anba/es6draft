/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 12.3.2.1 Evaluation: Call ToPropertyKey in step 9
// https://bugs.ecmascript.org/show_bug.cgi?id=3235

var sym = Symbol();
var o = {[sym]: 0};
assertSame(0, o[sym]);
