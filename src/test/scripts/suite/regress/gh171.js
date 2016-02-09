/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// Remove [[Construct]] from generators
// https://github.com/tc39/ecma262/pull/171

function* g(){}

assertThrows(TypeError, () => new g());
