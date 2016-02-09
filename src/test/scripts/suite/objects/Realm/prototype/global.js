/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertNotSame, assertNotNull,
  assertThrows, assertBuiltinFunction,
} = Assert;

/* 26.2.3.3 get Reflect.Realm.prototype.global */

const get_global = Object.getOwnPropertyDescriptor(Reflect.Realm.prototype, "global").get;

assertBuiltinFunction(get_global, "get global", 0);

// steps 1-2 - TypeError if thisValue is not an object
{
  let primitives = [void 0, true, false, 0, 1, 0.1, 0 / 0, "", "abc", Symbol()];
  for (let v of primitives) {
    assertThrows(TypeError, () => get_global.call(v));
  }
}

// steps 1-2 - TypeError if thisValue is not a Realm object
{
  let objects = [{}, [], Object, Object.create, function(){}, () => {}];
  for (let v of objects) {
    assertThrows(TypeError, () => get_global.call(v));
  }
}

// steps 3-4 - TypeError if thisValue is an uninitialized Realm object
{
  assertThrows(TypeError, () => get_global.call(Reflect.Realm[Symbol.create]()));
}

// step 5 - Return the Realm's [[globalThis]] object
{
  let realmA = new Reflect.Realm(), realmB = new Reflect.Realm();

  // Global object is an object type
  assertNotNull(get_global.call(realmA));
  assertSame(get_global.call(realmA), Object(get_global.call(realmA)));

  // A new realm has a new global object
  assertNotSame(this, get_global.call(realmA));

  // Different realms have different global objects
  assertNotSame(get_global.call(realmA), get_global.call(realmB));

  // "get global" returns the global object
  assertSame(get_global.call(realmA), get_global.call(realmA).Function("return this")());
}
