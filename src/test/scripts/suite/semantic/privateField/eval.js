/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

class C {
  #private = 42;

  get() {
    return eval("this.#private");
  }

  get2() {
    return eval("if (false) this.#private2");
  }
}

let o = new C();

assertSame(42, o.get());

assertThrows(SyntaxError, () => o.get2());
