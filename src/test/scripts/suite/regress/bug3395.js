/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows, fail
} = Assert;

// 7.4.7.5 IteratorClose should throw if hasReturn and !iterator.return().done
// https://bugs.ecmascript.org/show_bug.cgi?id=3395

const IteratorPrototype = Object.getPrototypeOf(Object.getPrototypeOf([][Symbol.iterator]()));

function returnIter(returnAction) {
  class Iter extends null {
    constructor() {
      return Object.create(new.target.prototype);
    }
    next() {
      return {value: 0, done: false};
    }
    return() {
      return returnAction();
    }
  }
  Object.setPrototypeOf(Iter.prototype, IteratorPrototype);

  return new Iter();
}

class ValidError extends Error {}
class InvalidError extends Error {}

var log = "";
assertThrows(ValidError, () => {
  var iter = returnIter(() => { log += "k"; throw new InvalidError });
  for (var o of iter) {
    log += "o";
    throw new ValidError;
  }
  fail `unreachable`;
});
assertSame("ok", log);

var log = "";
assertThrows(ValidError, () => {
  var iter = returnIter(() => { log += "k"; return null });
  for (var o of iter) {
    log += "o";
    throw new ValidError;
  }
  fail `unreachable`;
});
assertSame("ok", log);

var log = "";
assertThrows(ValidError, () => {
  var iter = returnIter(() => { log += "k"; throw new ValidError });
  for (var o of iter) {
    log += "o";
    break;
  }
  fail `unreachable`;
});
assertSame("ok", log);

var log = "";
assertThrows(TypeError, () => {
  var iter = returnIter(() => { log += "k"; return null });
  for (var o of iter) {
    log += "o";
    break;
  }
  fail `unreachable`;
});
assertSame("ok", log);

var log = "";
assertThrows(ValidError, () => {
  var iter = returnIter(() => { log += "k"; throw new ValidError });
  for (var o of iter) {
    log += "o";
    return;
  }
  fail `unreachable`;
});
assertSame("ok", log);

var log = "";
assertThrows(TypeError, () => {
  var iter = returnIter(() => { log += "k"; return null });
  for (var o of iter) {
    log += "o";
    return 123;
  }
  fail `unreachable`;
});
assertSame("ok", log);
