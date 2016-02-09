/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 12.3.4.* Evaluation: Parenthesized expression not handled and typo
// https://bugs.ecmascript.org/show_bug.cgi?id=3359

function nonStrictEvalWithParen() {
  (eval)("var x = 0");
  assertSame(0, x);

  ((eval))("var y = 1");
  assertSame(1, y);
}
nonStrictEvalWithParen();
