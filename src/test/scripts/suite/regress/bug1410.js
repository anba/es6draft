/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 15.5.1.1: `String(undefined)` no longer returns "undefined"
// https://bugs.ecmascript.org/show_bug.cgi?id=1410

assertSame("undefined", String(undefined));
