/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

// Invalid direct eval calls are subject to tail calls
{
  function eval() {
    return eval.caller;
  }
  function tailCall() {
    "use strict";
    return eval();
  }
  function start() {
    return tailCall();
  }
  assertSame(start, start());
}
