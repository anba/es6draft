/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

const root = Reflect.Realm.immutableRoot();
const newRealm = root.spawn();

assertSame(root.global, root.eval("this"));
assertSame(root.global, root.global.eval("this"));
assertSame(root.global, root.global.Function("return this")());

assertSame(newRealm.global, newRealm.eval("this"));
assertSame(newRealm.global, newRealm.global.eval("this"));
assertSame(newRealm.global, newRealm.global.Function("return this")());
