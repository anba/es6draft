/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows, fail
} = Assert;

// stub for Assert.assertSyntaxError
function assertSyntaxError(source) {
  return assertThrows(SyntaxError, () => parseModule(source));
}

function assertNoSyntaxError(source) {
  try {
    parseModule(source);
  } catch (e) {
    fail `Expected no syntax error, but got ${e}`
  }
}

// 'default' is allowed in function, function* and class if in export declaration
{
  let declarations = ["class default{}", "function default(){}", "function* default(){}"];
  for (let decl of declarations) {
    assertNoSyntaxError(`export ${decl};`);
  }
}

// 'default' is not allowed in function, function* and class outside of export declaration
{
  let declarations = ["class default{}", "function default(){}", "function* default(){}"];
  for (let decl of declarations) {
    assertSyntaxError(`${decl};`);
  }
}

// 'default' is not allowed in var, let and const
{
  let declarations = ["var default=0", "let default=0", "const default=0"];
  for (let decl of declarations) {
    assertSyntaxError(`export ${decl};`);
    assertSyntaxError(`${decl};`);
  }
}
