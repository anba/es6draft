/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 13.11.11 Evaluation: Lexical Environment not applied
// https://bugs.ecmascript.org/show_bug.cgi?id=3941

switch (0) {
  case 0: let x = "X";
  case 1: let y = "Y";
  default:
    assertSame("X", x);
    assertSame("Y", y);
}
