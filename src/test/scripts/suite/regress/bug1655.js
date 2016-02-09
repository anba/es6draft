/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals
} = Assert;

// 15.4.3.4.1: Missing type checks and wrong name in preamble
// https://bugs.ecmascript.org/show_bug.cgi?id=1655
// https://bugs.ecmascript.org/show_bug.cgi?id=1875

let array = [0];
let result = array.concat(1);
assertEquals([0, 1], result);
