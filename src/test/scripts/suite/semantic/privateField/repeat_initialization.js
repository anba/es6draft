/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// Side-effects in private field initializers do not allow repeated initialization.

class Ext extends class { constructor(o) { return o; } } {
  #private = f();
  constructor(o) { super(o); }
  static init(o) { return new Ext(o); }
  static get(o) { return o.#private; }
};

let obj = {};
let callCount = 0;

function f() {
  if (callCount++ === 0) {
    Ext.init(obj);
  }
  return "init:" + callCount;
}

assertThrows(TypeError, () => Ext.init(obj));
assertSame("init:2", Ext.get(obj));
assertSame(2, callCount);
