/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 14.1.18 Wrong scoping of non-strict direct evals in ComputedPropertyNames in parameters
// https://bugs.ecmascript.org/show_bug.cgi?id=4419

var x = "outer";

function evalInComputedWithDefault({[eval(`var x = "inner"; "a"`)]: y} = null) {
  assertSame(0, y);
  return x;
}
assertSame("outer", evalInComputedWithDefault({a: 0}));

function evalInComputedNoDefault({[eval(`var x = "inner"; "a"`)]: y}) {
  assertSame(0, y);
  return x;
}
assertSame("outer", evalInComputedNoDefault({a: 0}));

function evalInPatternWithDefault({a: y = eval(`var x = "inner"; 0`)} = null) {
  assertSame(0, y);
  return x;
}
assertSame("outer", evalInPatternWithDefault({}));

function evalInPatternNoDefault({a: y = eval(`var x = "inner"; 0`)}) {
  assertSame(0, y);
  return x;
}
assertSame("outer", evalInPatternNoDefault({}));
