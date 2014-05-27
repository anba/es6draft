/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSyntaxError
} = Assert;

function assertNoSyntaxError(source) {
  return Function(source);
}

// Similar to "yield_regexp_assigndiv.js", except arrow function is in default parameter initializer of generator:
// - "yield/b/g" is always parsed as YIELD ASSIGN_DIV IDENT DIV IDENT, so no SyntaxErrors (except when in strict mode)

// Parse "yield/=b/g" as the token sequence YIELD ASSIGN_DIV IDENT DIV IDENT, and assert that token sequence
// rematched as ArrowParameters is not a SyntaxError
assertNoSyntaxError(`function*g(p = (a = yield/=b/g) => { }){ }`);
assertNoSyntaxError(`function*g(p = (a = yield/=b/g) => { yield/=b/g }){ }`);

// No SyntaxError if in ConciseBody, here it's matched as YIELD ASSIGN_DIV IDENT DIV IDENT
assertNoSyntaxError(`function*g(p = () => yield/=b/g){ }`);
assertNoSyntaxError(`function*g(p = () => { yield/=b/g }){ }`);

// Same tests for other generator function contexts:
// (1) GeneratorExpression
assertNoSyntaxError(`(function*g(p = (a = yield/=b/g) => { }){ });`);
assertNoSyntaxError(`(function*g(p = (a = yield/=b/g) => { yield/=b/g }){ });`);
assertNoSyntaxError(`(function*g(p = () => yield/=b/g){ });`);
assertNoSyntaxError(`(function*g(p = () => { yield/=b/g }){ });`);
// (2) GeneratorMethod in ObjectLiteral
assertNoSyntaxError(`({ *g(p = (a = yield/=b/g) => { }){ } });`);
assertNoSyntaxError(`({ *g(p = (a = yield/=b/g) => { yield/=b/g }){ } });`);
assertNoSyntaxError(`({ *g(p = () => yield/=b/g){ } });`);
assertNoSyntaxError(`({ *g(p = () => { yield/=b/g }){ } });`);
// (3) GeneratorMethod in Class, here yield is never an IdentifierReference due to strict mode restrictions
assertSyntaxError(`(class { *g(p = (a = yield/=b/g) => { }){ } });`);
assertSyntaxError(`(class { *g(p = (a = yield/=b/g) => { yield/=b/g }){ } });`);
assertSyntaxError(`(class { *g(p = () => yield/=b/g){ } });`);
assertSyntaxError(`(class { *g(p = () => { yield/=b/g }){ } });`);


// There is no such restriction if the enclosing context is not a generator, in that case
// the token sequence is always YIELD ASSIGN_DIV IDENT DIV IDENT
assertNoSyntaxError(`function f(p = (a = yield/=b/g) => { }){ }`);
assertNoSyntaxError(`function f(p = (a = yield/=b/g) => { yield/=b/g }){ }`);
assertNoSyntaxError(`function f(p = () => yield/=b/g){ }`);
assertNoSyntaxError(`function f(p = () => { yield/=b/g }){ }`);

// Default parameters in ArrowFunctions are first parsed in the generator context, though
assertNoSyntaxError(`function*g(p = (a = (a = yield/=b/g) => { }) => { }){ }`);
assertNoSyntaxError(`function*g(p = (a = (a = yield/=b/g) => { yield/=b/g }) => { }){ }`);
assertNoSyntaxError(`function*g(p = (a = () => yield/=b/g) => {}){ }`);
assertNoSyntaxError(`function*g(p = (a = () => { yield/=b/g }) => {}){ }`);
