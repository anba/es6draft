/*
 * Copyright (c) André Bargull
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

assertTrue(isEven(stackLimit * 10));

assertTrue(isOdd(stackLimit * 10 + 1));
