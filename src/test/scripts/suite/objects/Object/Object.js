/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertNotSame,
  assertInstanceOf,
} = Assert;

/* 19.1.1  The Object Constructor Called as a Function */

// functional changes in comparison to ES5.1
// - symbol values are wrapped

assertInstanceOf(Symbol, Object(Symbol.iterator));
assertNotSame(Object(Symbol.iterator), Object(Symbol.iterator));


/* 19.1.2  The Object Constructor */

// functional changes in comparison to ES5.1
// - symbol values are wrapped

assertInstanceOf(Symbol, new Object(Symbol.iterator));
assertNotSame(new Object(Symbol.iterator), new Object(Symbol.iterator));
