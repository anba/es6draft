/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError, assertThrows
} = Assert;

function assertModuleSyntaxError(source) {
  return assertThrows(SyntaxError, () => parseModule(source));
}

assertSyntaxError("({/ : 0});");
assertSyntaxError("let {/} = 0;");
assertSyntaxError("function f(/){}");
assertModuleSyntaxError("import {/}");
