/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals
} = Assert;

// IteratorComplete: access own property?
// https://bugs.ecmascript.org/show_bug.cgi?id=4239

var iterable = {
  [Symbol.iterator]() {
    var values = [1, 2, 3];
    return {
      next() {
        if (values.length) {
          return Object.create({done: false, value: values.shift()});
        }
        return Object.create({done: true});
      }
    }
  }
};
var values = [];
for (var v of iterable) {
  values.push(v);
}
assertEquals([1, 2, 3], values);
