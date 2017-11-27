/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 21.2.5.*: Add .name property for @@match, @@replace, @@search and @@split
// https://bugs.ecmascript.org/show_bug.cgi?id=3464

assertSame("[Symbol.match]", RegExp.prototype[Symbol.match].name);
assertSame("[Symbol.replace]", RegExp.prototype[Symbol.replace].name);
assertSame("[Symbol.search]", RegExp.prototype[Symbol.search].name);
assertSame("[Symbol.split]", RegExp.prototype[Symbol.split].name);
