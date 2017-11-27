/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertFalse, assertNotConstructor
} = Assert;

// Getter and setter functions have no prototype property
// https://bugs.ecmascript.org/show_bug.cgi?id=3300

var object = {
  get x() {},
  set x(_) {},
};
var desc = Object.getOwnPropertyDescriptor(object, "x");

var getter = desc.get;
assertFalse(getter.hasOwnProperty("prototype"));
assertNotConstructor(getter);

var setter = desc.set;
assertFalse(setter.hasOwnProperty("prototype"));
assertNotConstructor(setter);
