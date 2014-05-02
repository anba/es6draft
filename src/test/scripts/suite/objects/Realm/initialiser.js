/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertEquals
} = Assert;

// lib/assert.js installs "Assert" as a property on the global object
const defaultIncludes = ["Assert"];

// Default global created if no arguments given
{
  let realm = new Reflect.Realm();

  // Default [[Prototype]] is %Object.prototype%
  assertSame(realm.global.Object.prototype, Object.getPrototypeOf(realm.global));

  // Same keys resp. own property names
  assertEquals(Object.keys(this), Object.keys(realm.global));
  assertEquals(Object.getOwnPropertyNames(this), Object.getOwnPropertyNames(realm.global).concat(defaultIncludes));
  assertEquals(Object.getOwnPropertySymbols(this), Object.getOwnPropertySymbols(realm.global));
}

// Empty global created if "initGlobal" is overridden
{
  let realm = new class extends Reflect.Realm {
    initGlobal() { }
  };

  // [[Prototype]] is set to %ObjectPrototype%
  assertSame(realm.stdlib.Object.value.prototype, Object.getPrototypeOf(realm.global));

  // No own property names present
  assertSame(0, Object.keys(realm.global).length);
  assertSame(0, Object.getOwnPropertyNames(realm.global).length);
  assertSame(0, Object.getOwnPropertySymbols(realm.global).length);
}

// Test arguments passed to "initGlobal" method
{
  let thisValue;
  let realm = new class extends Reflect.Realm {
    initGlobal(...args) {
      thisValue = this;
      assertSame(0, args.length);
    }
  };
  assertSame(realm, thisValue);
}
