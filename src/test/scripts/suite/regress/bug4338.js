/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 13.13.14 LabelledEvaluation: Completion value for break completions
// https://bugs.ecmascript.org/show_bug.cgi?id=4338

assertSame(0, eval(`0; L: ;`));
assertSame(void 0, eval(`0; L: break L;`));