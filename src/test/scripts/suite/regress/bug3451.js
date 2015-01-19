/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertCallable
} = Assert;

// Missing .name property for @@species getter
// https://bugs.ecmascript.org/show_bug.cgi?id=3451

for (let c of [RegExp, Array, Object.getPrototypeOf(Int8Array), Map, Set, ArrayBuffer, Promise]) {
  let {get} = Object.getOwnPropertyDescriptor(c, Symbol.species);
  assertCallable(get);
  assertSame("get [Symbol.species]", get.name);
}
