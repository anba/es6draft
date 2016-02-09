/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertInstanceOf
} = Assert;

// 19.1.2.1: Symbol type not handled in Object constructor
// https://bugs.ecmascript.org/show_bug.cgi?id=2019

assertInstanceOf(Symbol, Object(Symbol()));
assertInstanceOf(Symbol, new Object(Symbol()));
