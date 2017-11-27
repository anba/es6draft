/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertUndefined
} = Assert;

// 26.3.2 [ @@iterator ]: Missing type checks
// https://bugs.ecmascript.org/show_bug.cgi?id=3381

import* as self from "./bug3381.jsm";

var desc = Object.getOwnPropertyDescriptor(self, Symbol.iterator);
assertUndefined(desc);
