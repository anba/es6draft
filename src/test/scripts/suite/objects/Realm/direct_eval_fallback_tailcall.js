/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

// Invalid direct eval call and tail calls:
// - direct eval fallback and 'wrong' eval function have both tail calls enabled
// - chaining them should preserve the tail call property

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

let callCount;
let realm = new Realm({
  directEval: {
    fallback(thisArgument, callee, ...args) {
      "use strict";
      callCount += 1;
      return callee(...args);
    }
  }
});
realm.eval(`
  function sum(n, acc) {
    "use strict";
    if (n === 0) return acc;
    return eval(n - 1, acc + n);
  }
  eval = sum;
`);

for (let v of [1, 10, 100, 1000, 10000, stackLimit * 10]) {
  callCount = 0;
  assertSame(gauss(v), realm.eval(`sum(${v}, 0)`));
  assertSame(v, callCount);
}
