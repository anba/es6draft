/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 21.2.5.3 get RegExp.prototype.flags ( ): flags are not sorted alphabetically
// https://bugs.ecmascript.org/show_bug.cgi?id=3423

assertSame("gimuy", /(?:)/yumig.flags);
