/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertEquals
} = Assert;

var regexpLike = {
  indices: [0, 10],
  exec() {
    if (this.indices.length) {
      return {index: this.indices.shift()};
    }
    return null;
  },
  global: true
};
assertEquals(["undefined", "undefined"], RegExp.prototype[Symbol.match].call(regexpLike, "empty-empty"));
