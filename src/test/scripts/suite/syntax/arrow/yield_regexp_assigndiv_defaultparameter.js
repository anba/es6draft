/*
 * Copyright (c) André Bargull
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
// - "yield" is accepted if parent context allows "yield"
// - YieldExpression is disallowed in arrow function parameters

// Parse "yield/=b/g" as the token sequence YIELD REGEXP, and assert that YieldExpression
// is disallowed in arrow function parameters
assertSyntaxError(`function*g(p = (a = yield/=b/g) => { }){ }`);
assertSyntaxError(`function*g(p = (a = yield/=b/g) => { yield/=b/g }){ }`);

// No SyntaxError if in ConciseBody, here it's matched as YIELD ASSIGN_DIV IDENT DIV IDENT
assertNoSyntaxError(`function*g(p = () => yield/=b/g){ }`);
assertNoSyntaxError(`function*g(p = () => { yield/=b/g }){ }`);

// Same tests for other generator function contexts:
// (1) GeneratorExpression
assertSyntaxError(`(function*g(p = (a = yield/=b/g) => { }){ });`);
assertSyntaxError(`(function*g(p = (a = yield/=b/g) => { yield/=b/g }){ });`);
assertNoSyntaxError(`(function*g(p = () => yield/=b/g){ });`);
assertNoSyntaxError(`(function*g(p = () => { yield/=b/g }){ });`);
// (2) GeneratorMethod in ObjectLiteral
assertSyntaxError(`({ *g(p = (a = yield/=b/g) => { }){ } });`);
assertSyntaxError(`({ *g(p = (a = yield/=b/g) => { yield/=b/g }){ } });`);
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
assertSyntaxError(`function*g(p = (a = (a = yield/=b/g) => { }) => { }){ }`);
assertSyntaxError(`function*g(p = (a = (a = yield/=b/g) => { yield/=b/g }) => { }){ }`);
assertNoSyntaxError(`function*g(p = (a = () => yield/=b/g) => {}){ }`);
assertNoSyntaxError(`function*g(p = (a = () => { yield/=b/g }) => {}){ }`);
