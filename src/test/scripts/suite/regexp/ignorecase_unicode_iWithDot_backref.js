/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue, assertFalse
} = Assert;

let re = /(i)\1/ui;

// \u0130 and \u0131 are not matched in (i)
assertFalse(re.test("\u0130i"));
assertFalse(re.test("\u0131i"));

// \u0130 and \u0131 are not matched in backreference \1
assertFalse(re.test("i\u0130"));
assertFalse(re.test("i\u0131"));
