/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, fail
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

  function test() {
    try {
      f(stackLimit * 10);
    } catch (e) {
      // catch expected stackoverflow error
      return;
    }
    fail `tail call in non-strict mode should not be supported`;
  }
  test();
}
