/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
"use strict";

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

function sum(n, acc) {
  if (n === 0) return {n, acc};
  return new sum(n - 1, acc + n);
}

function gauss(n) {
  return (n * n + n) / 2;
}

for (let v of [1, 10, 100, 1000, 10000, stackLimit * 10]) {
  let result = sum(v, 0);
  assertSame(0, result.n);
  assertSame(gauss(v), result.acc);
}
