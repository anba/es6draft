/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertFalse, assertEquals, assertThrows
} = Assert;

// 12.2.4.2.4 ComprehensionComponentEvaluation, 13.6.4.6 ForIn/OfExpressionEvaluation: Align behaviour for iteration over undefined/null?
// https://bugs.ecmascript.org/show_bug.cgi?id=2737

// undefined and null are not ignored in comprehension
assertThrows(TypeError, () => [for (k of void 0) k]);
assertThrows(TypeError, () => [for (k of null) k]);
assertThrows(TypeError, () => (for (k of void 0) k).next());
assertThrows(TypeError, () => (for (k of null) k).next());
