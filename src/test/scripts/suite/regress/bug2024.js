/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
} = Assert;

// 22.2.3.26, %TypedArray%.prototype.sort: Invalid assertion in step 7
// https://bugs.ecmascript.org/show_bug.cgi?id=2024

let ta = new (class extends Int8Array {
  get length() { return 5 }
})(0);

ta.sort();
