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

// Default global created if "init" option not used
{
  let realm = new Reflect.Realm({});

  // Default [[Prototype]] is %Object.prototype%
  assertSame(realm.global.Object.prototype, Object.getPrototypeOf(realm.global));

  // Same keys resp. own property names
  assertEquals(Object.keys(this), Object.keys(realm.global));
  assertEquals(Object.getOwnPropertyNames(this), Object.getOwnPropertyNames(realm.global).concat(defaultIncludes));
  assertEquals(Object.getOwnPropertySymbols(this), Object.getOwnPropertySymbols(realm.global));
}

// Empty global created if "init" option used
{
  let realm = new Reflect.Realm({ init(){} });

  // [[Prototype]] is set to null
  assertSame(null, Object.getPrototypeOf(realm.global));

  // No own property names present
  assertSame(0, Object.keys(realm.global).length);
  assertSame(0, Object.getOwnPropertyNames(realm.global).length);
  assertSame(0, Object.getOwnPropertySymbols(realm.global).length);
}

// Test arguments passed to "init" option: realm
{
  let captured = {};
  let realm = new Reflect.Realm({
    init(realm, builtins) {
      // proposal currently makes thisArgument == argumentsList[0] == realm object
      assertSame(this, realm);
      Object.assign(captured, {realm, global: realm.global});
    }
  });

  // Test that realm in "init" function is the newly created realm
  assertSame(realm, captured.realm);
  assertSame(realm.global, captured.global);
}

// Test arguments passed to "init" option: builtins
{
  const thisGlobal = this;
  let realm = new Reflect.Realm({
    init(realm, builtins) {
      // Same keys resp. own property names as the global
      assertEquals(Object.keys(thisGlobal), Object.keys(builtins));
      assertEquals(Object.getOwnPropertyNames(thisGlobal), Object.getOwnPropertyNames(builtins).concat(defaultIncludes));
      assertEquals(Object.getOwnPropertySymbols(thisGlobal), Object.getOwnPropertySymbols(builtins));
    }
  });
}
