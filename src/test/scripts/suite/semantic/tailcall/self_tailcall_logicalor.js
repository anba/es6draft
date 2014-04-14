/*
 * Copyright (c) 2012-2014 André Bargull
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

function False() {
  return false;
}

function sumOr(n, acc) {
  if (n === 0) return acc;
  return False() || sumOr(n - 1, acc + n);
}

function gauss(n) {
  return (n * n + n) / 2;
}

assertSame(gauss(stackLimit * 10), sumOr(stackLimit * 10, 0));
