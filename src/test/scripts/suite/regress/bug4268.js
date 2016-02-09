/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals, assertSame
} = Assert;

// Presence of iterator.close checked too late
// https://bugs.ecmascript.org/show_bug.cgi?id=4268

var iterable = {
  [Symbol.iterator]() {
    return this;
  },
  value: 0,
  next() {
    return {value: ++this.value, done: false};
  }
};

var values = [];
for (var v of iterable) {
  values.push(v);
  if (v === 2) {
    iterable.return = () => {
      values.push(-1);
      return {done: true};
    };
    break;
  }
}
assertEquals([1, 2, -1], values);
