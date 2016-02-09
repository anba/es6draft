/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertBuiltinFunction,
} = Assert;


/* Promise.reject ( r ) */

assertBuiltinFunction(Promise.reject, "reject", 1);

