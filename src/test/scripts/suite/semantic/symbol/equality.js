/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertFalse, assertTrue,
} = Assert;

let sym = Symbol();

assertTrue(sym == Object(sym));
assertTrue(Object(sym) == sym);

assertFalse(sym != Object(sym));
assertFalse(Object(sym) != sym);

assertFalse(sym === Object(sym));
assertFalse(Object(sym) === sym);

assertTrue(sym !== Object(sym));
assertTrue(Object(sym) !== sym);
