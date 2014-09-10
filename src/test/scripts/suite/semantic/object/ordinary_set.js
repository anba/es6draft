/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertDataProperty,
} = Assert;

var p = new Proxy(Object.create(null), {
  get set() {
    Object.defineProperty(o, "prop", {value: 0});
    assertDataProperty(o, "prop", {value: 0, writable: false, enumerable: false, configurable: false});
  }
});
var o = Object.create(p);
o.prop = 1;
assertDataProperty(o, "prop", {value: 0, writable: false, enumerable: false, configurable: false});
