/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows
} = Assert;

// 24.3.3, Str operation, step 5c: Initialisation state of [[BooleanData]] unchecked
// https://bugs.ecmascript.org/show_bug.cgi?id=1928

assertThrows(() => JSON.stringify(Boolean[Symbol.create]()), TypeError);
