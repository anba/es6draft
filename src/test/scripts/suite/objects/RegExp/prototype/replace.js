/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertEquals, assertThrows
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

class Err extends Error {}
var log, regexpLike = {
  lastIndex: 0,
  global: false,
  unicode: false,
  exec() {
    return {
      get 0() {
        log.push("get 0");
      },
      get 1() {
        log.push("get 1");
      },
      get 2() {
        throw new Err();
      },
      length: 0xffffffff
    };
  },
  replace: RegExp.prototype[Symbol.replace]
}

log = [];
assertThrows(Err, () => RegExp.prototype[Symbol.replace].call(regexpLike, "", ""));
assertEquals(["get 0", "get 1"], log);

log = [];
assertThrows(Err, () => RegExp.prototype[Symbol.replace].call(regexpLike, "", () => ""));
assertEquals(["get 0", "get 1"], log);
