/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertNotSame, assertTrue, assertFalse
} = Assert;

const root = Reflect.Realm.immutableRoot();
const newRealm = root.spawn();

assertNotSame(root, newRealm);
assertNotSame(root.global, newRealm.global);

assertTrue(Object.isFrozen(root));
assertFalse(Object.isFrozen(newRealm));

assertTrue(Object.isFrozen(root.global));
assertFalse(Object.isFrozen(newRealm.global));

assertSame(root.global, Object.getPrototypeOf(newRealm.global));
