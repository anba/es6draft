/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertUndefined, assertThrows
} = Assert;

const root = Reflect.Realm.immutableRoot();

assertUndefined(root.global.Math.random);
assertUndefined(root.global.Date.now);
assertUndefined(root.global.Error().stack);
assertUndefined(root.global.System.makeWeakRef);

assertThrows(root.global.TypeError, () => new root.global.Date);
assertThrows(root.global.TypeError, () => root.global.Date());

assertThrows(root.global.TypeError, () => root.global.Intl.DateTimeFormat().format());
assertThrows(root.global.TypeError, () => root.global.Intl.DateTimeFormat().formatToParts());
