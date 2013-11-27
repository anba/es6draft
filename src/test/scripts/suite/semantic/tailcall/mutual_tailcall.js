/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
"use strict";

const {
  assertTrue
} = Assert;

const stackLimit = (function() {
  let limit = 0;
  try {
    (function f(){ f(limit++) })();
  } catch (e) {
  }
  return limit;
})();

function isOdd(n) {
  if (n === 0) return false;
  return isEven(n - 1);
}

function isEven(n) {
  if (n === 0) return true;
  return isOdd(n - 1);
}

for (let v of [0, 2, 4, 10, 100, 1000, 10000, stackLimit * 10]) {
  assertTrue(isEven(v));
}

for (let v of [1, 3, 5, 10 + 1, 100 + 1, 1000 + 1, 10000 + 1, stackLimit * 10 + 1]) {
  assertTrue(isOdd(v));
}
