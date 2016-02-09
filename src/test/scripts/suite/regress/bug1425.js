/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 11.4.3, Table 30: typeof operator result for Array and Arguments exotic objects
// https://bugs.ecmascript.org/show_bug.cgi?id=1425

assertSame("object", typeof []);
assertSame("object", typeof (function(){ return arguments })());
