/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 15.12.3 4b(ii) JSON.stringify replacer processing underspecified.
// https://bugs.ecmascript.org/show_bug.cgi?id=170

var replacer = [0, 1, 2, 3];
Object.prototype[3] = 3;
Object.defineProperty(replacer, 1, {
  get() {
    Object.defineProperty(replacer, 4, {value: 4});
    delete replacer[2];
    delete replacer[3];
    replacer[5] = 5;
    return 1;
  }
});
var s = JSON.stringify({0: {1: {3: {4: {5: {2: "omitted"}}}}}}, replacer);
assertSame('{"0":{"1":{"3":{"3":3}},"3":3},"3":3}', s);
