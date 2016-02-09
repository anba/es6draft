/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertNotSame, assertUndefined
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

function GetCaller(f) {
  return Object.getOwnPropertyDescriptor(f, "caller");
}

// bind this Realm function
assertUndefined(GetCaller(Function.prototype.bind.call(fn)));
assertUndefined(GetCaller(foreignFunction.prototype.bind.call(fn)));

// bind foreign Realm function
assertUndefined(GetCaller(Function.prototype.bind.call(foreignFn)));
assertUndefined(GetCaller(foreignFunction.prototype.bind.call(foreignFn)));
