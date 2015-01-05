/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertFalse
} = Assert;

// Flattened bound functions need to store original target function.
{
  let ordinaryHasInstance = Function.prototype[Symbol.hasInstance];
  let f = function (){};
  let count = 0;
  for (let i = 0; i < 3; ++i) {
    let bf = f.bind();
    Object.defineProperty(bf, Symbol.hasInstance, {
      get() {
        count += 1;
        return ordinaryHasInstance;
      }
    });
    f = bf;
  }
  assertFalse({} instanceof f);
  assertSame(3, count);
}
