/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

let x = "global";

{
  let x = "global-block";
  class C {
    constructor() {
      let x = "constructor-let";
    }

    #private = x;
    get() { return this.#private; }
  }
  assertSame("global-block", new C().get());
}

{
  let x = "global-block";
  class C {
    constructor() {
      var x = "constructor-var";
    }

    #private = x;
    get() { return this.#private; }
  }
  assertSame("global-block", new C().get());
}

{
  let x = "global-block";
  class C {
    constructor(x = "constructor-param") { }

    #private = x;
    get() { return this.#private; }
  }
  assertSame("global-block", new C().get());
}

{
  class x {
    constructor() { }

    #private = x;
    get() { return this.#private; }
  }
  assertSame(x, new x().get());
}
