/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// Should `regexp.compile(regexp, flags)` also no longer throws just like the RexExp constructor did in rev29 for consistency?
// https://bugs.ecmascript.org/show_bug.cgi?id=3505

assertSame("/b/i", /a/.compile("b", "i").toString());
assertThrows(TypeError, () => /a/.compile(/b/, "i"));
