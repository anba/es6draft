/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

// 23.2.5.2.2 %SetIteratorPrototype% [ @@iterator ] ( ): Missing .name description for symbol-valued function
// https://bugs.ecmascript.org/show_bug.cgi?id=2169

let setIter = new Set()[Symbol.iterator]();
assertSame("[Symbol.iterator]", setIter[Symbol.iterator].name);
