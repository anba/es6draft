/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertBuiltinFunction, assertSame, assertEquals
} = Assert;

System.load("lib/recorder.jsm");
const Recorder = System.get("lib/recorder.jsm");

/* Promise.prototype.catch ( onRejected ) */

assertBuiltinFunction(Promise.prototype.catch, "catch", 1);

// Promise.prototype.catch
{
  let onRejected = {};
  let returnValue = {};
  let history = [];
  let then = Recorder.watch(() => returnValue, history);
  let thenable = Recorder.watch({then}, history);
  let result = Promise.prototype.catch.call(thenable, onRejected);
  Recorder.unwatch(then);
  Recorder.unwatch(thenable);

  assertSame(returnValue, result);
  assertEquals([
    {name: "get", target: thenable, property: "then", result: then, receiver: thenable},
    {name: "apply", target: then, thisArgument: thenable, argumentsList: [void 0, onRejected], result: returnValue},
  ], history);
}
