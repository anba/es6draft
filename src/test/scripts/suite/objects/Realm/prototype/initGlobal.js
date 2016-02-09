/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertEquals, assertUndefined,
  assertBuiltinFunction,
} = Assert;

/* 26.2.3.7.3 Reflect.Realm.prototype.initGlobal ( ) */

const initGlobal = Reflect.Realm.prototype.initGlobal;

assertBuiltinFunction(initGlobal, "initGlobal", 0);

{
  let realm = new Reflect.Realm();
  let global = realm.global;
  let names = Object.getOwnPropertyNames(global);
  let properties = names.reduce((m, n) => m.set(n, Object.getOwnPropertyDescriptor(global, n)), new Map);

  // Remove all global properties
  names.forEach(n => delete global[n]);

  // Assert non-configurable properties are not removed
  assertEquals(["Infinity", "NaN", "undefined"], Object.getOwnPropertyNames(global).sort());

  // Re-init global
  realm.initGlobal();

  // Assert built-ins are restored to the same values
  assertSame(names.length, Object.getOwnPropertyNames(global).length);
  for (let name of names) {
    assertEquals(properties.get(name), Object.getOwnPropertyDescriptor(global, name));
  }
}

{
  let realm = new Reflect.Realm();
  let global = realm.global;
  let propertyName = "customProperty";

  // Add non-standard property on the global object
  Object.defineProperty(global, propertyName, {value: 123, configurable: true});

  // Remove all global properties
  Object.getOwnPropertyNames(global).forEach(n => delete global[n]);

  // Re-init global
  realm.initGlobal();

  // Assert non-standard property is not revived
  assertUndefined(Object.getOwnPropertyDescriptor(global, propertyName));
}
