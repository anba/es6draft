/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

const stackLimit = (function() {
  let limit = 0;
  try {
    (function f(){ f(limit++) })();
  } catch (e) {
  }
  return limit;
})();

// Ensure tail-call does not work in non-strict mode
{
  function f(limit) {
    if (limit == 0) return;
    return f(limit - 1);
  }

  try {
    f(stackLimit * 10);
    fail("tail call in non-strict mode should not be supported");
  } catch (e) {
  }
}
