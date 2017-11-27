/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
var global = this;

function __createIterableObject(array, methods = {}) {
  var i = 0, iterator = {
    next() {
      if (i < array.length) {
        return {value: array[i++], done: false};
      }
      return {value: void 0, done: true}
    },
    return: methods.return,
    throw: methods.throw,
  };
  return {
    [Symbol.iterator]() {
      return iterator;
    }
  };
}
