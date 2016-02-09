/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertNotSame, assertUndefined
} = Assert;

// Need decision on this binding of ES6 modules
// https://bugs.ecmascript.org/show_bug.cgi?id=3257

var indirectEval = eval;
assertUndefined(this);
assertNotSame(indirectEval("this"), this);
assertNotSame(Function("return this")(), this);
