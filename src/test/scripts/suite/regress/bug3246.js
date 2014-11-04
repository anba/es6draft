/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertThrows
} = Assert;

// super.x = value should throw if super.x is not a setter
// https://bugs.ecmascript.org/show_bug.cgi?id=3246

{
  class Base {}
  Base.prototype.property = 5;

  class Derived extends Base {
    test() {
      assertSame(5, super.property);
      super.property = 10;
      assertSame(5, super.property);
      assertSame(10, this.property);
    }
  }
  new Derived().test();
}

{
  class Base {}
  Base.prototype.property = 5;

  class Derived extends Base {
    test() {
      super.property = 10;
    }
    get property() { return 15; }
  }

  let inst = new Derived();
  assertSame(15, inst.property);

  inst.property = 5;
  assertSame(15, inst.property);

  assertThrows(TypeError, () => {
    "use strict";
    inst.property = 5;
  });
  assertSame(15, inst.property);

  inst.test();
  assertSame(10, inst.property);

  inst.property = 5;
  assertSame(5, inst.property);
}