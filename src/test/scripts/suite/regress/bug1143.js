/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 15.12: Quote Abstract Operation replaces " with \\ instead of \"
// https://bugs.ecmascript.org/show_bug.cgi?id=1143

assertSame('"\\""', JSON.stringify('"'));
assertSame('"\\\\"', JSON.stringify('\\'));
