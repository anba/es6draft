/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertEquals, assertUndefined,
  assertFalse, assertTrue,
  assertBuiltinFunction,
} = Assert;

/* 26.2.3.7.4 Reflect.Realm.prototype.nonEval (function, thisValue, argumentsList ) */

const nonEval = Reflect.Realm.prototype.nonEval;

assertBuiltinFunction(nonEval, "nonEval", 3);

// Test thisValue and arguments
{
  let realm = new Reflect.Realm();
  let thisValue = {};
  let firstArgument = {}, secondArgument = {};
  let returnValue = {};
  let calledOnce = false;
  function f(...args) {
    assertFalse(calledOnce);
    calledOnce = true;
    assertSame(thisValue, this);
    assertSame(2, args.length);
    assertSame(firstArgument, args[0]);
    assertSame(secondArgument, args[1]);
    return returnValue;
  }
  let actualReturnValue = realm.nonEval(f, thisValue, [firstArgument, secondArgument]);
  assertTrue(calledOnce);
  assertSame(returnValue, actualReturnValue);
}

// Test tail-call property
{
  let realm = new Reflect.Realm();
  function returnCaller() {
    return returnCaller.caller;
  }
  function f() {
    return realm.nonEval(returnCaller, null, []);
  }
  assertSame(f, f());
}
