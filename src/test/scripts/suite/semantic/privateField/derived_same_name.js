/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

class Base {
  #private = "base";

  static get(o) {
    return o.#private;
  }
}

class Derived extends Base {
  #private = "derived";

  static get(o) {
    return o.#private;
  }
}

let obj = new Derived();

assertSame("base", Base.get(obj));
assertSame("derived", Derived.get(obj));
