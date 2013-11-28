/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

// Ensure tailcall removes intermediate caller from stack
{
  function returnCaller() {
    return returnCaller.caller;
  }

  function tail() {
    "use strict";
    return returnCaller();
  }

  function start() {
    let caller = tail();
    assertSame(start, caller);
  }

  start();
}

// Ensure tailcall removes intermediate caller from stack
{
  function returnCaller() {
    return returnCaller.caller;
  }

  function tail() {
    "use strict";
    return new returnCaller();
  }

  function start() {
    let caller = tail();
    assertSame(start, caller);
  }

  start();
}

// Ensure tailcall removes intermediate caller from stack (Function.prototype.call)
{
  function returnCaller() {
    return returnCaller.caller;
  }

  function tail() {
    "use strict";
    return returnCaller.call();
  }

  function start() {
    let caller = tail.call();
    assertSame(start, caller);
  }

  start();
}

// Ensure tailcall removes intermediate caller from stack (Function.prototype.apply)
{
  function returnCaller() {
    return returnCaller.caller;
  }

  function tail() {
    "use strict";
    return returnCaller.apply();
  }

  function start() {
    let caller = tail.apply();
    assertSame(start, caller);
  }

  start();
}

// Ensure tailcall removes intermediate caller from stack (mixed calls, user and native)
{
  function returnCaller() {
    return returnCaller.caller;
  }

  function tail() {
    "use strict";
    return returnCaller();
  }

  function start() {
    let caller = tail.call();
    assertSame(start, caller);
  }

  start();
}

// Ensure tailcall removes intermediate caller from stack (mixed calls, user and native)
{
  function returnCaller() {
    return returnCaller.caller;
  }

  function tail() {
    "use strict";
    return returnCaller.call();
  }

  function start() {
    let caller = tail();
    assertSame(start, caller);
  }

  start();
}

// Ensure tailcall removes intermediate caller from stack (mixed calls, only native)
{
  function returnCaller() {
    return returnCaller.caller;
  }

  function tail() {
    "use strict";
    return returnCaller.call();
  }

  function start() {
    let caller = tail.apply();
    assertSame(start, caller);
  }

  start();
}
