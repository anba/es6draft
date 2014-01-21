/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertNotUndefined
} = Assert;

// 26.1.11: Missing return in Reflect.ownKeys
// https://bugs.ecmascript.org/show_bug.cgi?id=1937

assertNotUndefined(Reflect.ownKeys({}));
