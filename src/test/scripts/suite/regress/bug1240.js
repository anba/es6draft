/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// [10.6] MakeArgGetter/MakeArgGetter does not work well with duplicate arguments
// https://bugs.ecmascript.org/show_bug.cgi?id=1240

assertSame(1, (function(a,a){return arguments[0]})(1));
