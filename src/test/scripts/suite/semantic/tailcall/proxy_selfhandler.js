/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
"use strict";

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

function sum(n, acc) {
  let p = new Proxy(() => fail `unreachable`, {
    get apply() {
      if (n === 0) return () => acc;
      acc += n;
      n -= 1;
      return p;
    }
  });
  return p();
}

function gauss(n) {
  return (n * n + n) / 2;
}

assertSame(gauss(stackLimit * 10), sum(stackLimit * 10, 0));
