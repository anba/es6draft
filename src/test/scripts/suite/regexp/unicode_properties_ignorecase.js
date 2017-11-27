/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertFalse, assertTrue
} = Assert;

assertTrue(/[\P{gc=Lu}]/ui.test("A"));
assertTrue(/\P{gc=Lu}/ui.test("A"));

assertTrue(/[\P{gc=Lu}]/ui.test("a"));
assertTrue(/\P{gc=Lu}/ui.test("a"));

assertFalse(/[^\P{gc=Lu}]/ui.test("A"));
assertFalse(/[^\P{gc=Lu}]/ui.test("a"));
