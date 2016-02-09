/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

const legacyArguments = function f() { return f.arguments; }();

// No crash
function spreadOpt() {
  var count = (...args) => args.length;
  return count(...legacyArguments);
}
assertSame(0, spreadOpt());

// No crash
function sliceOpt() {
  return [].slice.call(legacyArguments).length;
}
assertSame(10000, sliceOpt(...Array(10000).fill(0)));

// No crash
Object.setPrototypeOf(Object.getPrototypeOf(Reflect.enumerate({})), legacyArguments);
for (var k in {a: 0, b: 0}) break;

// No crash
Object.setPrototypeOf(Object.getPrototypeOf([][Symbol.iterator]()), legacyArguments);
for (var k of [1, 2]) break;
