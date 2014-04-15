/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertTrue
} = Assert;

// function as base class, .constructor not updated
{
  function F() {}
  F.prototype = {
  };
  class C extends F {}

  let c = new C;
  assertTrue(c instanceof C);
}

// function as base class, .constructor updated
{
  function F() {}
  F.prototype = {
    constructor: F
  };
  class C extends F {}

  let c = new C;
  assertTrue(c instanceof C);
}
