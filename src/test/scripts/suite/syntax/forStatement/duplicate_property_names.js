/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// Duplicate property names in ForStatement

function nonStrictMode() {
  for ({x: a, x: b};;) ;
}

function strictMode() {
  "use strict";
  for ({x: a, x: b};;) ;
}

assertSyntaxError(`for ({__proto__: a, __proto__: b};;) ;`);
assertSyntaxError(`"use strict"; for ({__proto__: a, __proto__: b};;) ;`);
