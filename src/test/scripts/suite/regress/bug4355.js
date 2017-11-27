/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// Date.prototype.toString definition is incoherent
// https://bugs.ecmascript.org/show_bug.cgi?id=4355

assertThrows(TypeError, () => Date.prototype.toString.call(undefined));
assertThrows(TypeError, () => Date.prototype.toString.call(null));
assertThrows(TypeError, () => Date.prototype.toString.call("not-an-object"));

assertThrows(TypeError, () => Date.prototype.toString());
assertThrows(TypeError, () => Date.prototype.toString.call(Date.prototype));
assertThrows(TypeError, () => Date.prototype.toString.call(Object.prototype));
assertThrows(TypeError, () => Date.prototype.toString.call(Function.prototype));
