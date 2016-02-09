/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 13.2.2.5 Evaluation: RTL or LTR evaluation order for VariableDeclaration?
// https://bugs.ecmascript.org/show_bug.cgi?id=3213

function testFunction() {
  var scope = {x: 0};
  with (scope) {
    var x = (delete scope.x, 1);
  }
  assertSame(1, scope.x);
  assertSame(void 0, x);
}
testFunction();
