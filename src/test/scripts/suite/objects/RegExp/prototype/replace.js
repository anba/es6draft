/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

assertSame("undefined".length, "replaceme".length);

var regexpLike = {
  indices: [0, 10],
  exec() {
    if (this.indices.length) {
      return {index: this.indices.shift()};
    }
    return null;
  },
  global: false
};
assertSame("ok-replaceme", RegExp.prototype[Symbol.replace].call(regexpLike, "replaceme-replaceme", "ok"));

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
assertSame("ok-ok", RegExp.prototype[Symbol.replace].call(regexpLike, "replaceme-replaceme", "ok"));
