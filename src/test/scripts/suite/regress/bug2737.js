/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertFalse, assertEquals, assertThrows
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


// undefined and null are not ignored in for-of
let forOfUndefined = false;
assertThrows(() => { for (let k of void 0) forOfUndefined = true; }, TypeError);
assertFalse(forOfUndefined);

let forOfNull = false;
assertThrows(() => { for (let k of null) forOfNull = true; }, TypeError);
assertFalse(forOfNull);


// undefined and null are not ignored in comprehension
assertThrows(() => [for (k of void 0) k], TypeError);
assertThrows(() => [for (k of null) k], TypeError);
assertThrows(() => (for (k of void 0) k).next(), TypeError);
assertThrows(() => (for (k of null) k).next(), TypeError);

