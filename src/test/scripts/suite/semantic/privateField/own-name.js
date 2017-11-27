/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

let c = class {
  static a = void Object.defineProperty(this, "name", {value: "C"});
};
assertSame("C", c.name);
