/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// Direct eval calls are not subject to tail calls (https://bugs.ecmascript.org/show_bug.cgi?id=2404)
{
  function tailCall() {
    "use strict";
    let x = 0;
    return eval("x");
  }
  function start() {
    let x = 1;
    return tailCall();
  }
  assertSame(0, start());
}
