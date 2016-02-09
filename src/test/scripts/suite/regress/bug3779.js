/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  fail
} = Assert;

// B.3.1 __proto__ : Directly set [[Prototype]] instead calling [[SetPrototypeOf]] ?
// https://bugs.ecmascript.org/show_bug.cgi?id=3779

var p = new Proxy({}, {
  getPrototypeOf() {
    fail `getPrototypeOf called`;
  }
});

var o = {__proto__: p};
