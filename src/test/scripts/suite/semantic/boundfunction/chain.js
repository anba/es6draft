/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// Chained bound functions are flattened.
function f() { return 0; }
for (var i = 0; i < 100000; ++i) { f = f.bind(); delete f.name; }
assertSame(0, f());
