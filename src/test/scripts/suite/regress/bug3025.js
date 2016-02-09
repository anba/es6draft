/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals
} = Assert;

// 9.5.12 [[OwnPropertyKeys]]: Mutable keys array can violate object invariants
// https://bugs.ecmascript.org/show_bug.cgi?id=3025

let o = Object.defineProperty({}, "prop", {value: 123, configurable: false});
let p = new Proxy(o, {
  ownKeys() {
    return Object.defineProperty([], "0", {
      get() {
        this.length = 0;
        return "prop";
      }, configurable: true
    });
  }
});
assertEquals(["prop"], Object.getOwnPropertyNames(p));
