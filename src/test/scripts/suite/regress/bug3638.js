/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 19.2.1.1.1 CreateDynamicFunction: Missing application of early error restrictions
// https://bugs.ecmascript.org/show_bug.cgi?id=3638

const GeneratorFunction = function*(){}.constructor;

// Parameters and lexical declaration
assertThrows(SyntaxError, () => Function("a", "let a"));
assertThrows(SyntaxError, () => GeneratorFunction("a", "let a"));

// SuperCall in FormalParameters
assertThrows(SyntaxError, () => Function("a = super()", ""));
assertThrows(SyntaxError, () => GeneratorFunction("a = super()", ""));

// SuperCall in FunctionBody
assertThrows(SyntaxError, () => Function("super()"));
assertThrows(SyntaxError, () => GeneratorFunction("super()"));

// Duplicate lexical declarations
assertThrows(SyntaxError, () => Function("let a; let a"));
assertThrows(SyntaxError, () => GeneratorFunction("let a; let a"));

// Lexical and variable declarations
assertThrows(SyntaxError, () => Function("let a; var a"));
assertThrows(SyntaxError, () => GeneratorFunction("let a; var a"));
assertThrows(SyntaxError, () => Function("var a; let a"));
assertThrows(SyntaxError, () => GeneratorFunction("var a; let a"));

// Duplicate parameters in strict mode
assertThrows(SyntaxError, () => Function("a, a", "'use strict'"));
assertThrows(SyntaxError, () => GeneratorFunction("a, a", "'use strict'"));

// Duplicate parameters in non-simple parameter list
assertThrows(SyntaxError, () => Function("a, a = 0", ""));
assertThrows(SyntaxError, () => GeneratorFunction("a, a = 0", ""));
assertThrows(SyntaxError, () => Function("a, {a}", ""));
assertThrows(SyntaxError, () => GeneratorFunction("a, {a}", ""));
