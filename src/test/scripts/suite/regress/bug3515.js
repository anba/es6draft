/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue
} = Assert;

// Classes: Inherited methods and properties ought to be non-enumerable
// https://bugs.ecmascript.org/show_bug.cgi?id=3515

assertTrue(Object.getOwnPropertyDescriptor(class { m() {}}.prototype, "m").enumerable);
assertTrue(Object.getOwnPropertyDescriptor(class { static m() {}}, "m").enumerable);
