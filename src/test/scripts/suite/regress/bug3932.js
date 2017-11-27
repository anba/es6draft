/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows, assertSyntaxError,
} = Assert;

// 15.1.1 Early Errors: Incomplete early error restrictions for "super" and "new.target" in eval code
// https://bugs.ecmascript.org/show_bug.cgi?id=3932

assertThrows(SyntaxError, () => eval("new.target"));
assertThrows(SyntaxError, () => eval("super.prop"));
assertThrows(SyntaxError, () => eval("super()"));
