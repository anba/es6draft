/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// 14.4.1: missing early error for direct super call in function* definitions
// https://bugs.ecmascript.org/show_bug.cgi?id=4420

assertSyntaxError(`function f(a = super()) {}`);
assertSyntaxError(`function f(a = super.b) {}`);
assertSyntaxError(`function f(a = super.b()) {}`);

assertSyntaxError(`function f() { super() }`);
assertSyntaxError(`function f() { super.b }`);
assertSyntaxError(`function f() { super.b() }`);

assertSyntaxError(`function* g(a = super()) {}`);
assertSyntaxError(`function* g(a = super.b) {}`);
assertSyntaxError(`function* g(a = super.b()) {}`);

assertSyntaxError(`function* g() { super() }`);
assertSyntaxError(`function* g() { super.b }`);
assertSyntaxError(`function* g() { super.b() }`);
