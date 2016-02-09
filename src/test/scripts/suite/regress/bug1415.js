/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// 9.1.1: remove early return in OrdinaryToPrimitive
// https://bugs.ecmascript.org/show_bug.cgi?id=1415

assertSame(0, 2 - {valueOf(){ return 2 }, toString(){ return 1 }});
assertSame(0, 2 - {valueOf(){ return 2 }, toString(){ return {} }});
assertSame(1, 2 - {valueOf(){ return {} }, toString(){ return 1 }});
assertThrows(TypeError, () => 2 - {valueOf(){ return {} }, toString(){ return {} }});
