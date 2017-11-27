/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 9.4.4.2 [[DefineOwnProperty]]: Invalid assertion in step 4
// https://bugs.ecmascript.org/show_bug.cgi?id=2866

function tryDefineCaller() {
  "use strict";
  Object.defineProperty(arguments, "caller", {value: null});
}
tryDefineCaller();
