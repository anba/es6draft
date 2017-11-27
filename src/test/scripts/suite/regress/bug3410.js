/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// 18.2.1.2 EvalDeclarationInstantiation: Missing binding checks and initialization
// https://bugs.ecmascript.org/show_bug.cgi?id=3410

function nonStrictEvalVarDeclBindingNotPresent() {
  assertSame("undefined", typeof a);
  assertThrows(ReferenceError, () => a);
  eval("var a = 1");
  assertSame(1, a);
}
nonStrictEvalVarDeclBindingNotPresent();

function nonStrictEvalVarDeclBindingPresent() {
  var a = 0;
  assertSame(0, a);
  eval("var a = 1");
  assertSame(1, a);
}
nonStrictEvalVarDeclBindingPresent();

function nonStrictEvalFunctionDeclBindingNotPresent() {
  assertSame("undefined", typeof f);
  assertThrows(ReferenceError, () => f);
  eval("function f() { return 1 }");
  assertSame(1, f());
}
nonStrictEvalFunctionDeclBindingNotPresent();

function nonStrictEvalFunctionDeclBindingPresent() {
  function f() { return 0 }
  assertSame(0, f());
  eval("function f() { return 1 }");
  assertSame(1, f());
}
nonStrictEvalFunctionDeclBindingPresent();
