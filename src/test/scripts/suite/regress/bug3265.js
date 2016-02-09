/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 19.2.3.2 Function.prototype.bind: Drop "bound" prefix for already bound functions?
// https://bugs.ecmascript.org/show_bug.cgi?id=3265

function f() {}
var bf = f.bind(null);
var bbf = bf.bind(null);

assertSame("f", f.name);
assertSame("bound f", bf.name);
assertSame("bound bound f", bbf.name);
