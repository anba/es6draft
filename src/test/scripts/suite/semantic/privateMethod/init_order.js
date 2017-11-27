/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// Private methods are installed before private fields.
{
  class C {
    #a = this.#m;
    #m() { }

    get_a() {
      return this.#a;
    }
    get_m() {
      return this.#m;
    }
  }

  let o = new C();
  assertSame(o.get_a(), o.get_m());
}

// The same applies for the static counter parts.
{
  class C {
    static #a = this.#m;
    static #m() { }

    static get_a() {
      return this.#a;
    }
    static get_m() {
      return this.#m;
    }
  }

  assertSame(C.get_a(), C.get_m());
}
