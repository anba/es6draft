/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// 9.2.4 [[Call]]: Use callee's realm for ToObject() operation
// https://bugs.ecmascript.org/show_bug.cgi?id=2484

let realm = new Reflect.Realm();

// Wrapper created in callee's realm
realm.eval(`
  function returnThis() {
    return this;
  }
`);
assertSame(realm.global.Number.prototype, Object.getPrototypeOf(realm.global.returnThis.call(1)));

// ReferenceError created in caller's realm
assertThrows(realm.global.SyntaxError, () => realm.eval(`
  function noSuperBinding() {
    super.foo();
  }
`));
// assertThrows(ReferenceError, () => realm.global.noSuperBinding());
