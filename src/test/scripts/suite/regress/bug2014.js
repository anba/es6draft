/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows
} = Assert;

// 9.3.1 [[GetPrototypeOf]]: Missing type check for trap result
// https://bugs.ecmascript.org/show_bug.cgi?id=2014

let proxy = new Proxy({}, {getPrototypeOf() { return 0 }});
assertThrows(TypeError, () => Object.getPrototypeOf(proxy));
