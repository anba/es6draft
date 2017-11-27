/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertUndefined, assertNotUndefined, assertThrows, fail
} = Assert;

class Err extends Error { }
function throwError() { throw new Err(); }

// constructor not invoked when initializer throws exception.
{
  class C {
    #a = throwError();

    constructor() {
      fail `constructor called`;
    }
  }
  assertThrows(Err, () => new C());
}

// Private field not added even if initializer throws exception. (1)
{
  let o;
  class B {
    constructor() {
      o = this;
    }
  }
  class C extends B {
    #a = throwError();

    static get(o) {
      return o.#a;
    }
  }

  assertThrows(Err, () => new C());
  assertNotUndefined(o);
  assertThrows(TypeError, () => C.get(o));
}

// Private field not added even if initializer throws exception. (2)
{
  let o;
  class B {
    constructor() {
      o = this;
    }
  }
  class C extends B {
    #a = "ok";
    #b = throwError();

    get a() {
      return this.#a;
    }

    get b() {
      return this.#b;
    }
  }

  assertThrows(Err, () => new C());
  assertNotUndefined(o);
  assertSame("ok", o.a);
  assertThrows(TypeError, () => o.b);
}
