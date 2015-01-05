/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 15.4.3.10: CreateOwnDataProperty no longer valid to use
// https://bugs.ecmascript.org/show_bug.cgi?id=1883

let a = (new class extends Array{ constructor(){ this.push(0) } }).slice(0, 1);
assertSame(1, a.length);
assertSame(0, a[0]);
