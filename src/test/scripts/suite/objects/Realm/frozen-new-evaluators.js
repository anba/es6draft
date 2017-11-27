/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertNotSame,
} = Assert;

const root = Reflect.Realm.immutableRoot();
const newRealm = root.spawn();

// Evaluator functions are not shared.
assertNotSame(root.global.Function, newRealm.global.Function);
assertNotSame(root.global.eval, newRealm.global.eval);

// Other built-in functions and prototypes are shared.
assertSame(root.global.Object, newRealm.global.Object);
assertSame(root.global.Array, newRealm.global.Array);
assertSame(root.global.Function.prototype, newRealm.global.Function.prototype);

// Evaluators inherit from parent realm.
assertSame(root.global.Function, Reflect.getPrototypeOf(newRealm.global.Function));
assertSame(root.global.eval, Reflect.getPrototypeOf(newRealm.global.eval));
