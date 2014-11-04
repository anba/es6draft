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

// Unnamed declarations are allowed in default export declaration
{
  let declarations = ["class {}", "function (){}", "function* (){}"];
  for (let decl of declarations) {
    assertNoSyntaxError(`export default ${decl};`);
  }
}

// Unnamed declarations are not allowed outside of default export declaration
{
  let declarations = ["class {}", "function (){}", "function* (){}"];
  for (let decl of declarations) {
    assertSyntaxError(`${decl};`);
  }
}
