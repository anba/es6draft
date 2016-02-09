/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// Allow early ReferenceErrors in Function and eval calls
// https://github.com/tc39/ecma262/pull/156

assertThrows(ReferenceError, () => Function(`throw "unreachable"; 0 = 1;`));
