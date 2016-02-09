/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

//  Wrong escape sequence for tabulation code in JSON stringifier
// https://bugs.ecmascript.org/show_bug.cgi?id=4374

assertSame('"\\t"', JSON.stringify("\t"));
