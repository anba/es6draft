/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 19.1.4.6 Object.prototype.toString ( ): remove "Math" and "JSON" from censored list
// https://bugs.ecmascript.org/show_bug.cgi?id=2054

assertSame("[object JSON]", Object.prototype.toString.call(JSON));
assertSame("[object Math]", Object.prototype.toString.call(Math));
