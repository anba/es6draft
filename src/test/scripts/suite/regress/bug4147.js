/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals, fail
} = Assert;

// 22.1.3.1 Array.concat: isConcatSpreadable should use iterator if possible
// https://bugs.ecmascript.org/show_bug.cgi?id=4147

var a = {
  [Symbol.isConcatSpreadable]: true,
  length: 1,
  0: "ok",
  [Symbol.iterator]() {
    fail `iterator called`;
  }
};

assertEquals(["ok"], Array.prototype.concat.call(a));
