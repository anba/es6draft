/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// global this
{
  class C {
    #private = this;
    get() { return this.#private; }
  }
  let obj = new C();
  assertSame(obj, obj.get());
}

// function this
(function () {
  "use strict";
  class C {
    #private = this;
    get() { return this.#private; }
  }
  let obj = new C();
  assertSame(obj, obj.get());
}).call("this-value");

// function this
(function () {
  "use strict";
  class C {
    #private = () => this;
    get() { return this.#private(); }
  }
  let obj = new C();
  assertSame(obj, obj.get());
}).call("this-value-arrow");
