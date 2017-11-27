/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame,
  assertFalse,
  assertThrows,
} = Assert;

// eval issues with new declaration forms
// https://bugs.ecmascript.org/show_bug.cgi?id=1111

function nonStrictEvalLet() {
  assertFalse(eval("let x1; delete x1;"));
  assertSame("undefined", typeof x1);

  assertFalse(eval("let x2 = 0; delete x2;"));
  assertSame("undefined", typeof x2);

  assertFalse(eval("let {x3} = {}; delete x3;"));
  assertSame("undefined", typeof x3);

  assertFalse(eval("let {x4 = 0} = {}; delete x4;"));
  assertSame("undefined", typeof x4);

  assertFalse(eval("let [x5] = []; delete x5;"));
  assertSame("undefined", typeof x5);

  assertFalse(eval("let [x6 = 0] = []; delete x6;"));
  assertSame("undefined", typeof x6);
}
nonStrictEvalLet();

function nonStrictEvalConst() {
  assertFalse(eval("const x2 = 0; delete x2;"));
  assertSame("undefined", typeof x2);

  assertFalse(eval("const {x3} = {}; delete x3;"));
  assertSame("undefined", typeof x3);

  assertFalse(eval("const {x4 = 0} = {}; delete x4;"));
  assertSame("undefined", typeof x4);

  assertFalse(eval("const [x5] = []; delete x5;"));
  assertSame("undefined", typeof x5);

  assertFalse(eval("const [x6 = 0] = []; delete x6;"));
  assertSame("undefined", typeof x6);
}
nonStrictEvalConst();

function nonStrictEvalClass() {
  assertFalse(eval("class x1 {}; delete x1;"));
  assertSame("undefined", typeof x1);
}
nonStrictEvalConst();
