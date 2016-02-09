/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertSyntaxError
} = Assert;

// 12.1: CoverParenthesisedExpressionAndArrowParameterList should take IdentifierReference
// https://bugs.ecmascript.org/show_bug.cgi?id=1966

let f = (yield) => +yield;
assertSame(123, f("123"));

assertSyntaxError(`"use strict"; let f = (yield) => +yield;`);
