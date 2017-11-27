/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 20.3.4.27 Date.prototype.setTime: Insufficient internal type checks
// https://bugs.ecmascript.org/show_bug.cgi?id=3938

assertThrows(TypeError, () => Date.prototype.setTime.call({}, 0));
