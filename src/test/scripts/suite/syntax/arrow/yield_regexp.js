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

// Parse "yield/b/g" as the token sequence YIELD REGEXP, and assert that token sequence
// rematched as ArrowParameters is a SyntaxError
assertSyntaxError(`function*g(){ (a = yield/b/g) => { }; }`);
assertSyntaxError(`function*g(){ (a = yield/b/g) => { yield/b/g }; }`);

// No SyntaxError if in ConciseBody, here it's matched as YIELD DIV IDENT DIV IDENT
assertNoSyntaxError(`function*g(){ () => yield/b/g; }`);
assertNoSyntaxError(`function*g(){ () => { yield/b/g }; }`);

// Same tests for other generator function contexts:
// (1) GeneratorExpression
assertSyntaxError(`(function*g(){ (a = yield/b/g) => { }; });`);
assertSyntaxError(`(function*g(){ (a = yield/b/g) => { yield/b/g }; });`);
assertNoSyntaxError(`(function*g(){ () => yield/b/g; });`);
assertNoSyntaxError(`(function*g(){ () => { yield/b/g }; });`);
// (2) GeneratorMethod in ObjectLiteral
assertSyntaxError(`({ *g(){ (a = yield/b/g) => { }; } });`);
assertSyntaxError(`({ *g(){ (a = yield/b/g) => { yield/b/g }; } });`);
assertNoSyntaxError(`({ *g(){ () => yield/b/g; } });`);
assertNoSyntaxError(`({ *g(){ () => { yield/b/g }; } });`);
// (3) GeneratorMethod in Class, here yield is never an IdentifierReference due to strict mode restrictions
assertSyntaxError(`(class { *g(){ (a = yield/b/g) => { }; } });`);
assertSyntaxError(`(class { *g(){ (a = yield/b/g) => { yield/b/g }; } });`);
assertSyntaxError(`(class { *g(){ () => yield/b/g; } });`);
assertSyntaxError(`(class { *g(){ () => { yield/b/g }; } });`);


// There is no such restriction if the enclosing context is not a generator, in that case
// the token sequence is always YIELD DIV IDENT DIV IDENT
assertNoSyntaxError(`function f(){ (a = yield/b/g) => { }; }`);
assertNoSyntaxError(`function f(){ (a = yield/b/g) => { yield/b/g }; }`);
assertNoSyntaxError(`function f(){ () => yield/b/g; }`);
assertNoSyntaxError(`function f(){ () => { yield/b/g }; }`);

// Default parameters in ArrowFunctions are first parsed in the generator context, though
assertSyntaxError(`function*g(){ (a = (a = yield/b/g) => { }) => { }; }`);
assertSyntaxError(`function*g(){ (a = (a = yield/b/g) => { yield/b/g }) => { }; }`);
assertNoSyntaxError(`function*g(){ (a = () => yield/b/g) => {}; }`);
assertNoSyntaxError(`function*g(){ (a = () => { yield/b/g }) => {}; }`);
