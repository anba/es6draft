/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertUndefined
} = Assert;

// Function to determine if an object is an exotic namespace object
// https://bugs.ecmascript.org/show_bug.cgi?id=3299

import* as self from "./bug3299.jsm";

assertUndefined(self.toString);
assertSame("object", typeof self);
assertSame("[object Module]", Object.prototype.toString.call(self));
