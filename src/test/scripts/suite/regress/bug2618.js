/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

// 21.2.5.7 RegExp.prototype.replace: non-global replace does not work
// https://bugs.ecmascript.org/show_bug.cgi?id=2618

assertSame("cab", "abab".replace(/ab/, "c"));
