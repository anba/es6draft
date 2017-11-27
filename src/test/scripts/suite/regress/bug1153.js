/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertUndefined
} = Assert;

// 21.1.3.3: codePointAt: `undefined` vs. `NaN` return value
// https://bugs.ecmascript.org/show_bug.cgi?id=1153

assertUndefined("".codePointAt(0));
