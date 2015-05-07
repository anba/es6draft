/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// Annex E: B.3.5 and FunctionDeclaration in eval code
// https://bugs.ecmascript.org/show_bug.cgi?id=4339

assertThrows(SyntaxError, () => {
  try {
    throw null;
  } catch (e) {
    eval("function e() { }");
  }
});
