/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 19.2.1.1.1 CreateDynamicFunction: Missing early error checks for SuperProperty
// https://bugs.ecmascript.org/show_bug.cgi?id=3969

assertThrows(SyntaxError, () => Function("super.prop"));
assertThrows(SyntaxError, () => Function("p = super.prop", ""));
