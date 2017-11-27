/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
"use strict";

const {
  assertThrows
} = Assert;

const stackLimit = (function() {
  let limit = 0;
  try {
    (function f(){ f(limit++) })();
  } catch (e) {
  }
  return limit;
})();

function fsum(n, acc) {
  if (n === 0) return {n, acc};
  return new sum(n - 1, acc + n);
}
let sum = new Proxy(fsum, {});

function gauss(n) {
  return (n * n + n) / 2;
}

assertThrows(Error, () => sum(stackLimit * 10, 0));
