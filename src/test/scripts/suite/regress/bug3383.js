/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 9.2.13, 18.2.1.2: Direct eval + default parameter expression
// https://bugs.ecmascript.org/show_bug.cgi?id=3383

// TODO: Update when spec is fixed

function f1(a = eval("var b = 1")) { return b }
assertSame(1, f1());

function f2(a = eval("var b = 1")) { let b = 2; return b }
assertSame(2, f2());

function f3(a = eval("var b = 1"), c = b) { return b + c }
assertSame(2, f3());

function f4(a = eval("var b = 1"), c = b) { let b = 2; return b + c }
assertSame(3, f4());

function f5(a = eval("var b = 1"), c = b + b) { return b + c }
assertSame(3, f5());
