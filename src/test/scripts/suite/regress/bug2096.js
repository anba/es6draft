/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 7.4.7.1 ListIterator next( ): Invalid assertion in step 2
// https://bugs.ecmascript.org/show_bug.cgi?id=2096

assertThrows(TypeError, () => Reflect.enumerate({}).next.call({}));
