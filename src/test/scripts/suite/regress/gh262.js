/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertUndefined
} = Assert;

// Feature testing no longer possible with RegExp.prototype.sticky
// https://github.com/tc39/ecma262/issues/262

assertUndefined(RegExp.prototype.sticky);
