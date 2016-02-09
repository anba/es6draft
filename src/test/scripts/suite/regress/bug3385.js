/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertTrue
} = Assert;

// Require that the mandatory parameter to object literal setter syntax not have an overall default
// https://bugs.ecmascript.org/show_bug.cgi?id=3385

var o = {
  set property(value = null) {
    o._property = value;
  }
};

o.property = void 0;
assertSame(null, o._property);

o.property = null;
assertSame(null, o._property);

o.property = 0;
assertSame(0, o._property);
