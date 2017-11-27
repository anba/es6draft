/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue, assertThrows, fail
} = Assert;

// 13.6.4.8 Runtime Semantics: ForIn/OfBodyEvaluation: Invalid assertion in step 3.h.v
// https://bugs.ecmascript.org/show_bug.cgi?id=3030

function testNoBindingException() {
  for (let {} of [null]) fail `loop body entered`;
}
assertThrows(TypeError, testNoBindingException);

function testBindingException() {
  for (let {a: b} of [null]) fail `loop body entered`;
}
assertThrows(TypeError, testBindingException);
