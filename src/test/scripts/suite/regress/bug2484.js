/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertThrows
} = Assert;

// 9.2.4 [[Call]]: Use callee's realm for ToObject() operation
// https://bugs.ecmascript.org/show_bug.cgi?id=2484

// Note: This behaviour is most likely not intended, but correct per rev22 spec

let realm = new Realm();
realm.eval(`
  function returnThis() {
    return this;
  }
  function noSuperBinding() {
    super();
  }
`);

// Wrapper created in caller's realm
assertSame(Number.prototype, Object.getPrototypeOf(realm.global.returnThis.call(1)));

// ReferenceError created in caller's realm
assertThrows(() => realm.global.noSuperBinding(), ReferenceError);
