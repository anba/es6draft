/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertSyntaxError
} = Assert;

// B.1.1 should specify decimal integer literals starting with 0 and containing at least one of 8 or 9
// https://bugs.ecmascript.org/show_bug.cgi?id=2792

function sloppyMode() {
  // LegacyOctalIntegerLiteral
  assertSame(0, 00);
  assertSame(1, 01);
  assertSame(2, 02);
  assertSame(3, 03);
  assertSame(4, 04);
  assertSame(5, 05);
  assertSame(6, 06);
  assertSame(7, 07);

  assertSame(1, 001);
  assertSame(9, 011);
  assertSame(17, 021);
  assertSame(25, 031);
  assertSame(33, 041);
  assertSame(41, 051);
  assertSame(49, 061);
  assertSame(57, 071);

  // NonOctalDecimalIntegerLiteral
  assertSame(8, 08);
  assertSame(9, 09);
  assertSame(8.5, 08.5);
  assertSame(9.6, 09.6);
}
sloppyMode();

function strictMode() {
  for (var i = 0; i <= 9; ++i) {
    assertSyntaxError(`use strict; 0${i};`);
  }
}
strictMode();
