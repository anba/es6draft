/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
} = Assert;

// Duplicate property names in ForInStatement is not a SyntaxError

function nonStrictMode() {
  for ({x: a, x: b} in {}) ;
}

function strictMode() {
  "use strict";
  for ({x: a, x: b} in {}) ;
}
