/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 14.4.13 EvaluateBody: Invalid assertion in step 1
// https://bugs.ecmascript.org/show_bug.cgi?id=2640

// no crash
(function*(a = 0){})();
