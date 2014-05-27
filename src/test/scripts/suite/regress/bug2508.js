/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSyntaxError
} = Assert;

// 14.1 FunctionDeclaration, 14.4 GeneratorDeclaration: Disallow "yield" as BindingIdentifier in Function/GeneratorDeclaration when enclosed by generator
// https://bugs.ecmascript.org/show_bug.cgi?id=2508

// Function -> FunctionDeclaration
function outerFunction() {
  function yield(){}
}

// Function -> GeneratorDeclaration
function outerFunction() {
  function* yield(){}
}

// Generator -> FunctionDeclaration
assertSyntaxError(`function* outerGenerator() {
  function yield(){}
}`);

// Generator -> GeneratorDeclaration
assertSyntaxError(`function* outerGenerator() {
  function* yield(){}
}`);

// Function -> FunctionExpression
function outerFunction() {
  (function yield(){});
}

// Generator -> FunctionExpression
function* outerGenerator() {
  (function yield(){});
}

// Function -> GeneratorExpression
assertSyntaxError(`function outerFunction() {
  (function* yield(){});
}`);

// Generator -> GeneratorExpression
assertSyntaxError(`function* outerGenerator() {
  (function* yield(){});
}`);
