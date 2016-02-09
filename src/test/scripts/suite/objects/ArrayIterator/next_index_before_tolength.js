/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals,
} = Assert;

var runInner = true;
var log = [];

var obj = {
  get length() {
    log.push("get length");
    if (runInner) {
      runInner = false;
      for (var q of iter) log.push("inner: " + q)
    }
    return 2;
  },
  get 0() {
    log.push("get 0");
    return "zero";
  },
  get 1() {
    log.push("get 1");
    return "one";
  }
}

var iter = Array.prototype[Symbol.iterator].call(obj);
for (var p of iter) { log.push("outer: " + p); }

assertEquals([
  "get length",
  "get length",
  "get 0",
  "inner: zero",
  "get length",
  "get 1",
  "inner: one",
  "get length",
  "get 0",
  "outer: zero",
], log);
