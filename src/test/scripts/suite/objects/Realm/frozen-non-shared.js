/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertNotSame,
} = Assert;

const root = Reflect.Realm.immutableRoot();

assertNotSame(root, Reflect.Realm.immutableRoot());
