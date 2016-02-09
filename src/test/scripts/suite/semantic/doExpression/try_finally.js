/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

function tryWithReturnFinallyNoValue() {
  return do { try { return 2; } finally { } };
}
assertSame(2, tryWithReturnFinallyNoValue());

function tryWithReturnFinallyWithValue() {
  return do { try { return 2; } finally { 3; } };
}
assertSame(2, tryWithReturnFinallyWithValue());

function tryWithReturnFinallyWithBreakNoValue() {
  return do { L: try { return 2; } finally { break L; } };
}
assertSame(void 0, tryWithReturnFinallyWithBreakNoValue());

function tryWithReturnFinallyWithBreakValue() {
  return do { L: try { return 2; } finally { 3; break L; } };
}
assertSame(3, tryWithReturnFinallyWithBreakValue());
