/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 8.1.1.3.1 BindThisValue: Change assertion to if+throw
// https://bugs.ecmascript.org/show_bug.cgi?id=3675

// super() in ArrowFunction
{
  class Base {
    constructor(f) {
      f && f();
    }
  }
  class Derived extends Base {
    constructor() {
      super(() => {
        super();
      });
    }
  }
  assertThrows(TypeError, () => new Derived);
}

// super() in ArgumentsList
{
  class Base { }
  class Derived extends Base {
    constructor() {
      super(super());
    }
  }
  assertThrows(TypeError, () => new Derived);
}
