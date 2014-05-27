/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertFalse, assertEquals
} = Assert;

// 12.2.4.2.4 ComprehensionComponentEvaluation, 13.6.4.6 ForIn/OfExpressionEvaluation: Align behaviour for iteration over undefined/null?
// https://bugs.ecmascript.org/show_bug.cgi?id=2737

// undefined and null are ignored in for-in
let forInUndefined = false;
for (let k in void 0) forInUndefined = true;
assertFalse(forInUndefined);

let forInNull = false;
for (let k in null) forInNull = true;
assertFalse(forInNull);


// undefined and null are ignored in for-of
let forOfUndefined = false;
for (let k of void 0) forOfUndefined = true;
assertFalse(forOfUndefined);

let forOfNull = false;
for (let k of null) forOfNull = true;
assertFalse(forOfNull);


// undefined and null are ignored in comprehension
assertEquals([], [for (k of void 0) k]);
assertEquals([], [for (k of null) k]);
assertEquals({value: void 0, done: true}, (for (k of void 0) k).next());
assertEquals({value: void 0, done: true}, (for (k of null) k).next());

