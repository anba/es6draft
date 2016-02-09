/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// 13.6 Iteration Statements: lookahead restriction for "for-of" needs to be "let", not "let ["
// https://bugs.ecmascript.org/show_bug.cgi?id=2768

assertSyntaxError(`for (let.a of b) ;`);
