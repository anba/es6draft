/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertBuiltinFunction,
} = Assert;


/* Promise.all ( iterable ) */

assertBuiltinFunction(Promise.all, "all", 1);

