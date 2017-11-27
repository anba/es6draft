/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// Private methods can be accessors.
{
  class C {
    constructor() {
      this.value = 0;
    }

    get #p() { return this.value; }
    set #p(v) { this.value = v + 100; }

    get_p() { return this.#p; }
    set_p(v) { this.#p = v; }
  }

  let o = new C();
  assertSame(0, o.get_p());
  o.set_p(10);
  assertSame(100 + 10, o.get_p());
}

// Private methods can be accessors.
{
  class C {
    static get #p() { return this.value; }
    static set #p(v) { this.value = v + 100; }

    static get_p() { return this.#p; }
    static set_p(v) { this.#p = v; }
  }
  C.value = 0;

  assertSame(0, C.get_p());
  C.set_p(10);
  assertSame(100 + 10, C.get_p());
}
