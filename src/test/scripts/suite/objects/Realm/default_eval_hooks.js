/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame,
  assertFalse,
  assertTrue,
  assertUndefined,
} = Assert;

// Indirect eval call
{
  let realm = new Reflect.Realm();
  let result = realm.eval(`(1, eval)("[].__proto__")`);
  assertSame(realm.global.Array.prototype, result);
}

// Valid direct eval call
{
  let realm = new Reflect.Realm();
  let result = realm.eval(`eval("[].__proto__")`);
  assertSame(realm.global.Array.prototype, result);
}

// Invalid direct eval call
{
  let realm = new Reflect.Realm();
  let result = realm.eval(`{ let eval = x => "eval: " + x; eval("[].__proto__") }`);
  assertSame("eval: [].__proto__", result);
}
