/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 21.2.5.2 RegExp.prototype.exec: Call RegExpBuiltinExec in step 5
// https://bugs.ecmascript.org/show_bug.cgi?id=2775

// No infinite recursion
/(?:)/.exec("");

assertThrows(TypeError, () => RegExp[Symbol.create]().exec(""));
