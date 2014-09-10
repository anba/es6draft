/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows
} = Assert;

// 15.4.3.4: Missing ReturnIfAbrupt step after ToObject in step 1
// https://bugs.ecmascript.org/show_bug.cgi?id=1656

assertThrows(TypeError, () => Array.prototype.concat.call(void 0));
assertThrows(TypeError, () => Array.prototype.concat.call(null));
