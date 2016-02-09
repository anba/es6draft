/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertUndefined
} = Assert;

// 6.1.7 The Object Type: Not necessary to special case "-0" for array indices
// https://bugs.ecmascript.org/show_bug.cgi?id=2830

function* zip(a, b) {
  a = a[Symbol.iterator](), b = b[Symbol.iterator]();
  for (let x, y; (x = a.next(), y = b.next(), !x.done && !y.done); ) {
    yield [x.value, y.value];
  }
}

{
  let indices = [0, -0, +0, "0", "-0", "+0"];
  let arrayExpected = [7, 7, 7, 7, void 0, void 0];
  let typedArrayExpected = [7, 7, 7, 7, void 0, void 0];
  let array = [7];
  let typedArray = new Int8Array(array);

  for (let [index, expected] of zip(indices, arrayExpected)) {
    assertSame(expected, array[index]);
  }
  for (let [index, expected] of zip(indices, typedArrayExpected)) {
    assertSame(expected, typedArray[index]);
  }
}
