/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 12.14.5.2 DestructuringAssignmentEvaluation: Stray ToObject conversion
// https://bugs.ecmascript.org/show_bug.cgi?id=2663

var r;
({r = null} = {});
assertSame(null, r);
