/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

// 19.1.3.5 Object.prototype.toLocaleString: this-binding no longer boxed
// https://bugs.ecmascript.org/show_bug.cgi?id=3237

Number.prototype.toString = function() {
  "use strict";
  return typeof this;
};
var e = Object.prototype.toLocaleString.call(0);

assertSame("number", e);
