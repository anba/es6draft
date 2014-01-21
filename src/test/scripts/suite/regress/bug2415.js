/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows
} = Assert;

// 24.1.2.1 ArrayBuffer(...): Missing constructor reentrancy checks
// https://bugs.ecmascript.org/show_bug.cgi?id=2415

let buf = ArrayBuffer[Symbol.create]();
assertThrows(() => ArrayBuffer.call(buf, {valueOf(){ ArrayBuffer.call(buf, 0); return 1 }}), TypeError);
