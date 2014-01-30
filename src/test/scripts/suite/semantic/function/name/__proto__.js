/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertFalse, assertCallable
} = Assert;

function assertAnonymousFunction(f) {
  return assertFalse(f.hasOwnProperty("name"));
}

// B.3.1  __proto___ Property Names in Object Initialisers
// Anonymous function as __proto__ [[Prototype]] won't be named "__proto__"
{
  let o1 = {
    __proto__: function() {}
  };
  assertCallable(Object.getPrototypeOf(o1));
  assertAnonymousFunction(Object.getPrototypeOf(o1));

  let o2 = {
    __proto__: function*() {}
  };
  assertCallable(Object.getPrototypeOf(o2));
  assertAnonymousFunction(Object.getPrototypeOf(o2));

  let o3 = {
    __proto__: () => {}
  };
  assertCallable(Object.getPrototypeOf(o3));
  assertAnonymousFunction(Object.getPrototypeOf(o3));

  let o4 = {
    __proto__: class {}
  };
  assertCallable(Object.getPrototypeOf(o4));
  assertAnonymousFunction(Object.getPrototypeOf(o4));
}
