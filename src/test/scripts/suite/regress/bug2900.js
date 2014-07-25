/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertNotSame
} = Assert;

// 19.2.3.2 Function.prototype.bind: Incorrect argument to BoundFunctionTargetRealm
// https://bugs.ecmascript.org/show_bug.cgi?id=2900

let foreignRealm = new Reflect.Realm();
let {
  ThrowTypeError: foreignThrowTypeError,
  Function: foreignFunction,
} = foreignRealm.intrinsics;
let ThrowTypeError = System.realm.intrinsics.ThrowTypeError;

assertNotSame(ThrowTypeError, foreignThrowTypeError);
assertNotSame(Function, foreignFunction);

let foreignFn = foreignRealm.eval(`
  function F() { }
  F;
`);
let fn = function G() { };

function GetThrowTypeError(f) {
  return Object.getOwnPropertyDescriptor(f, "caller").get;
}

// bind this Realm function
assertSame(ThrowTypeError, GetThrowTypeError(Function.prototype.bind.call(fn)));
assertSame(ThrowTypeError, GetThrowTypeError(foreignFunction.prototype.bind.call(fn)));

// bind foreign Realm function
assertSame(foreignThrowTypeError, GetThrowTypeError(Function.prototype.bind.call(foreignFn)));
assertSame(foreignThrowTypeError, GetThrowTypeError(foreignFunction.prototype.bind.call(foreignFn)));
