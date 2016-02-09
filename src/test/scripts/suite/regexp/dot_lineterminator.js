/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertFalse, assertTrue
} = Assert;

for (let t of ["\n", "\r", "\u2028", "\u2029"]) {
  assertFalse(/./.test(t));
}

assertTrue(/./.test("\x85"));
