/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// 22.2.1.2 %TypedArray%: Set internal slots in consecutive steps
// https://bugs.ecmascript.org/show_bug.cgi?id=2163

class MyError extends Error {}
let source = new Int8Array(10);
let accessed = 0;
Object.defineProperty(source.buffer, "constructor", {
  get() {
    accessed += 1;
    throw new MyError;
  }
});

let target = new class extends Int8Array { constructor() { /* no super */ } };
assertThrows(MyError, () => Int8Array.call(target, source));
assertSame(1, accessed);
assertSame("[object Int8Array]", Object.prototype.toString.call(target));

// FIXME: spec bug xxxx
if (false) {
class StopConstructor extends Error {}
let called = 0;
target.constructor = function Ctor(len) {
  called += 1;
  // check [[ArrayLength]] was not changed
  assertSame(0, len);
  throw new StopConstructor;
};

// Use %TypedArray%.prototype.map to obtain [[ArrayLength]] value
assertThrows(StopConstructor, () => target.map(() => {}));
assertSame(1, called);
}
