/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertUndefined
} = Assert;

// RegExp.prototype not an instance web compatibility workaround
// https://github.com/tc39/ecma262/pull/511

assertUndefined(RegExp.prototype.global);
assertUndefined(RegExp.prototype.ignoreCase);
assertUndefined(RegExp.prototype.multiline);
assertUndefined(RegExp.prototype.sticky);
assertUndefined(RegExp.prototype.unicode);
