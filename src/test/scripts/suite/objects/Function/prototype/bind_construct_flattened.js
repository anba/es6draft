/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

function f() { return new.target; }
var bf1 = f.bind();
var bf2 = bf1.bind();

var newTarget = Reflect.construct(bf2, [], bf1);
assertSame(f, newTarget);
