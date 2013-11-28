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

function gauss(n) {
  return (n * n + n) / 2;
}

// Ensure tail-call works in implicit strict mode (class method)
{
  class TailCall {
    sum(n, acc) {
      if (n === 0) return acc;
      return this.sum(n - 1, acc + n);
    }
  }

  for (let v of [1, 10, 100, 1000, 10000, stackLimit * 10]) {
    assertSame(gauss(v), new TailCall().sum(v, 0));
  }
}

// Ensure tail-call works in implicit strict mode (class static method)
{
  class TailCall {
    static sum(n, acc) {
      if (n === 0) return acc;
      return this.sum(n - 1, acc + n);
    }
  }

  for (let v of [1, 10, 100, 1000, 10000, stackLimit * 10]) {
    assertSame(gauss(v), TailCall.sum(v, 0));
  }
}
