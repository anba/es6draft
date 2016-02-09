/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// Remove B.3.1 from list of non-strict extensions
// https://github.com/tc39/ecma262/pull/148

function f() {
  "use strict";
  return {__proto__: null};
}

assertSame(null, Object.getPrototypeOf(f()));
