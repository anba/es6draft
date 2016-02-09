/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 24.3.2.3 JO Abstract Operation: Missing property enumeration order note
// https://bugs.ecmascript.org/show_bug.cgi?id=3239

var log;
var o = {
  get a() {
    log += "|a";
  },
  get b() {
    log += "|b";
  },
  get 0() {
    log += "|0";
  },
  get [Symbol()]() {
    log += "|x";
  },
  get [Symbol()]() {
    log += "|y";
  },
  get 1() {
    log += "|1";
  },
  get c() {
    log += "|c";
  },
  get d() {
    log += "|d";
  },
};

log = "";
Object.keys(o).forEach(k => o[k]);
assertSame("|0|1|a|b|c|d", log);

log = "";
JSON.stringify(o);
assertSame("|0|1|a|b|c|d", log);
