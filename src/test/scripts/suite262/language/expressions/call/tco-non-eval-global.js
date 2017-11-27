/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
id: sec-function-calls-runtime-semantics-evaluation
info: Tail-call with identifier named "eval" in global environment.
description: >
  ...

flags: [noStrict]
features: [tail-call-optimization]
includes: [tcoHelper.js]
---*/

var callCount = 0;

function f(n) {
  "use strict";
  if (n === 0) {
    callCount += 1
    return;
  }
  return eval(n - 1);
}
eval = f;

f($MAX_ITERATIONS);

assert.sameValue(callCount, 1);
