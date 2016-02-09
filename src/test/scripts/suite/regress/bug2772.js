/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 20.1.3.6 Number.prototype.toString: Remove brackets in function definition or add explicit length
// https://bugs.ecmascript.org/show_bug.cgi?id=2772

assertSame(1, Number.prototype.toString.length);
