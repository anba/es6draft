/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// Unscopables should use HasProperty and not Get
// https://bugs.ecmascript.org/show_bug.cgi?id=3206

var log = "";

var unscopables = {
  get a() {
    log += "a";
    return void 0;
  },
  get b() {
    log += "b";
    return null;
  },
  get c() {
    log += "c";
    return false;
  },
  get d() {
    log += "d";
    return true;
  },
  get e() {
    log += "e";
    return {};
  },
};

var object = {
  [Symbol.unscopables]: unscopables,
  get a() {
    log += "A";
  },
  get b() {
    log += "B";
  },
  get c() {
    log += "C";
  },
  get d() {
    log += "D";
  },
  get e() {
    log += "E";
  },
};

var a, b, c, d, e;
with (object) {
 a; b; c; d; e;
}
assertSame("aAbBcCde", log);
