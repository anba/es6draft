/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 23.4.1.1 WeakSet: Missing ReturnIfAbrupt after step 2
// https://bugs.ecmascript.org/show_bug.cgi?id=3660

class Err extends Error { }

var newTarget = function (){}.toMethod({});
Object.defineProperty(newTarget, "prototype", {
  get() {
    throw new Err;
  }
});

assertThrows(Err, () => Reflect.construct(WeakSet, [], newTarget));
