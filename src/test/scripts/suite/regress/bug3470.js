/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 21.2.3.3.2 RegExpInitialize: Incorrect check in step 13
// https://bugs.ecmascript.org/show_bug.cgi?id=3470

var r = /a/;
assertSame("a", r.source);

r.compile(/b/);
assertSame("b", r.source);
