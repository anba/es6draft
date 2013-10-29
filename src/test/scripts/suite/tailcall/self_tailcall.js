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

function sum(n, acc) {
  if (n === 0) return acc;
  return sum(n - 1, acc + n);
}

function gauss(n) {
  return (n * n + n) / 2;
}

for (let v of [1, 10, 100, 1000, 10000, stackLimit * 10]) {
  assertSame(gauss(v), sum(v, 0));
}
