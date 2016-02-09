/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame,
} = Assert;

// 19.2.3.2 Function.prototype.bind: NaN and 0 which is the larger one?
// https://bugs.ecmascript.org/show_bug.cgi?id=3289

function makeFn(length) {
  return Object.defineProperty(function f(a, b){}, "length", {value: length});
}

assertSame("abc", makeFn("abc").length);

assertSame(0, makeFn("abc").bind().length);
assertSame(0, makeFn("1").bind().length);
assertSame(0, makeFn(NaN).bind().length);
assertSame(0, makeFn(-Infinity).bind().length);

assertSame(+Infinity, makeFn(+Infinity).bind().length);
assertSame(+Infinity, makeFn(+Infinity).bind(null, "a").length);
assertSame(+Infinity, makeFn(+Infinity).bind(null, "a", "b").length);

assertSame(1, makeFn(1).bind().length);
assertSame(0, makeFn(1).bind(null, "a").length);
assertSame(0, makeFn(1).bind(null, "a", "b").length);

assertSame(2, makeFn(2).bind().length);
assertSame(1, makeFn(2).bind(null, "a").length);
assertSame(0, makeFn(2).bind(null, "a", "b").length);
