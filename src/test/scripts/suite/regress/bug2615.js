/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

// 21.2.5.7 RegExp.prototype.replace: Off by one error when processing capturing groups
// https://bugs.ecmascript.org/show_bug.cgi?id=2615

/a(b*)(c+)/.replace("_abcc_", (group0, group1, group2, index, string) => {
  assertSame("abcc", group0);
  assertSame("b", group1);
  assertSame("cc", group2);
  assertSame(1, index);
  assertSame("_abcc_", string);
});

/a(b*)(c+)/.replace("_acc_", (group0, group1, group2, index, string) => {
  assertSame("acc", group0);
  assertSame("", group1);
  assertSame("cc", group2);
  assertSame(1, index);
  assertSame("_acc_", string);
});
