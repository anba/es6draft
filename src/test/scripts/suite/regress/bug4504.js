/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 16.1 Forbidden Extensions: Extension B.3.1 is not restricted to non-strict code
// https://bugs.ecmascript.org/show_bug.cgi?id=4504

(function() {
  "use strict";

  var p = {};
  var o = {__proto__: p};

  assertSame(p, Object.getPrototypeOf(o));
})();
