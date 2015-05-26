/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// Date.prototype.toString definition is incoherent
// https://bugs.ecmascript.org/show_bug.cgi?id=4355

assertThrows(TypeError, () => Date.prototype.toString.call(undefined));
assertThrows(TypeError, () => Date.prototype.toString.call(null));
assertThrows(TypeError, () => Date.prototype.toString.call("not-an-object"));

assertSame("Invalid Date", Date.prototype.toString());
assertSame("Invalid Date", Date.prototype.toString.call(Date.prototype));
assertSame("Invalid Date", Date.prototype.toString.call(Object.prototype));
assertSame("Invalid Date", Date.prototype.toString.call(Function.prototype));
