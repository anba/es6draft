/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue, assertFalse
} = Assert;

// RegExp processing unicode+ignoreCase of \W is not the same as !\w when matching "S" or "K"
// https://github.com/tc39/ecma262/issues/512

assertTrue(/\w/ui.test("s"));
assertTrue(/\w/ui.test("S"));

assertFalse(/\W/ui.test("s"));
assertFalse(/\W/ui.test("S"));


assertTrue(/\w/ui.test("k"));
assertTrue(/\w/ui.test("K"));

assertFalse(/\W/ui.test("k"));
assertFalse(/\W/ui.test("K"));
