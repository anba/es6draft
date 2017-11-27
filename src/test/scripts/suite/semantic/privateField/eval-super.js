/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

class C extends class {} {
  #private = 42;

  constructor() {
    eval("super()");
  }

  get() {
    return this.#private;
  }
}

let o = new C();

assertSame(42, o.get());
