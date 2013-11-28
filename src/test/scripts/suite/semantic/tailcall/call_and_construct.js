/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

// Ensure tail-call works when mixing [[Call]] and [[Construct]]
{
  function returnCaller() {
    return returnCaller.caller;
  }

  function Call() {
    "use strict";
    return returnCaller();
  }

  function Construct() {
    "use strict";
    return new Call();
  }

  function start() {
    let caller = Construct();
    assertSame(start, caller);
  }

  start();
}

// Ensure tail-call works when mixing [[Call]] and [[Construct]]
{
  function returnCaller() {
    return returnCaller.caller;
  }

  function Construct() {
    "use strict";
    return new returnCaller();
  }

  function Call() {
    "use strict";
    return Construct();
  }

  function start() {
    let caller = Call();
    assertSame(start, caller);
  }

  start();
}

