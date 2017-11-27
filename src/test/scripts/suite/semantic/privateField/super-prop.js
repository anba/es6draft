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
  get prop() {
    return "base-prop";
  }
}

class Derived extends Base {
  #private = super.prop;
  get() { return this.#private; }

  get prop() {
    return "derived-prop";
  }
}

let obj = new Derived();
assertSame("base-prop", obj.get());
