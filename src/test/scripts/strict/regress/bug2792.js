/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSyntaxError
} = Assert;

// B.1.1 should specify decimal integer literals starting with 0 and containing at least one of 8 or 9
// https://bugs.ecmascript.org/show_bug.cgi?id=2792

function sloppyMode() {
  // LegacyOctalIntegerLiteral
  for (var i = 0; i <= 7; ++i) {
    assertSyntaxError(`0${i};`);
  }
  for (var i = 0; i <= 7; ++i) {
    assertSyntaxError(`0${i}1;`);
  }

  // NonOctalDecimalIntegerLiteral
  assertSyntaxError(`08;`);
  assertSyntaxError(`09;`);
  assertSyntaxError(`08.5;`);
  assertSyntaxError(`09.5;`);
}
sloppyMode();

function strictMode() {
  for (var i = 0; i <= 9; ++i) {
    assertSyntaxError(`use strict; 0${i};`);
  }
}
strictMode();
