/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 21.1.3.11 String.prototype.match: Missing ReturnIfAbrupt after step 5
// https://bugs.ecmascript.org/show_bug.cgi?id=3715

assertThrows(SyntaxError, () => "".match("+"));
