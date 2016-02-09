/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// super in eval
// https://bugs.ecmascript.org/show_bug.cgi?id=3844

var indirectEval = eval;

assertThrows(SyntaxError, () => eval(`super.property`));
assertThrows(SyntaxError, () => eval(`function f() { super.property }`));

assertThrows(SyntaxError, () => indirectEval(`super.property`));
assertThrows(SyntaxError, () => indirectEval(`function f() { super.property }`));

function f(c) {
  eval(`if (c) super.property`);
}
f(false);
assertThrows(ReferenceError, () => f(true));