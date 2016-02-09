/*
 * Copyright (c) 2012-2015 André Bargull
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

const varDeclarations = ["var a = 0"];
const lexicalDeclarations = ["let a = 0", "const a = 0", "class a{}", "function a(){}", "function* a(){}"];
const importDeclarations = ["import* as a from 'dummy'", "import a from 'dummy'", "import a, {} from 'dummy'", "import {a} from 'dummy'", "import {b as a} from 'dummy'"];
const declarations = [...lexicalDeclarations, ...importDeclarations];

const exportVarDeclarations = [for (d of varDeclarations) `export ${d}`];
const exportLexicalDeclarations = [for (d of lexicalDeclarations) `export ${d}`];
const exportSpecifiers = ["export {a}", "export {b as a}", "export {a} from 'dummy'", "export {b as a} from 'dummy'"];
const exportDeclarations = [...exportLexicalDeclarations, ...exportVarDeclarations, ...exportSpecifiers];

// Duplicate lexically declared names (let, const, class, function, function*, import)
{
  for (let [decl1, decl2] of [for (d1 of declarations) for (d2 of declarations) [d1, d2]]) {
    assertSyntaxError(`${decl1}; ${decl2};`);
  }
}

// Duplicate lexically declared names, one in export (let, const, class, function, function*)
{
  for (let [exp, decl] of [for (e of exportLexicalDeclarations) for (d of declarations) [e, d]]) {
    assertSyntaxError(`${exp}; ${decl};`);
    assertSyntaxError(`${decl}; ${exp};`);
  }
}

// Duplicate exported bindings
{
  for (let [exp1, exp2] of [for (e1 of exportDeclarations) for (e2 of exportDeclarations) [e1, e2]]) {
    assertSyntaxError(`${exp1}; ${exp2};`);
  }
}

// Intersection of lexically declared and var declared names must be empty
{
  for (let [vdecl, decl] of [for (v of [...varDeclarations, ...exportVarDeclarations]) for (d of [...declarations, ...exportLexicalDeclarations]) [v, d]]) {
    assertSyntaxError(`${vdecl}; ${decl};`);
    assertSyntaxError(`${decl}; ${vdecl};`);
  }
}

// Duplicate VariableStatement is allowed, unless both appear in export (no duplicate exported bindings)
{
  assertNoSyntaxError("var a; var a;");
  assertNoSyntaxError("export var a; var a;");
  assertNoSyntaxError("var a; export var a;");
  assertSyntaxError("export var a; export var a;");
}

// Duplicate default exports
{
  let exports = ["default 1", "function default(){}", "function* default(){}", "class default{}"];
  for (let [export1, export2] of [for (e1 of exports) for (e2 of exports) [e1, e2]]) {
    assertSyntaxError(`export ${export1}; export ${export2}`);
  }
}

// 'super' in ModuleItemList
{
  assertSyntaxError("super"); // ...
}

