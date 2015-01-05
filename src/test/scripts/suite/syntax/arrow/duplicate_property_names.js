/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
} = Assert;

// https://bugs.ecmascript.org/show_bug.cgi?id=2506

// Duplicate property names in ArrowParameters is not a SyntaxError

function nonStrictMode() {
  ({x: a, x: b}) => {};
}

function strictMode() {
  "use strict";
  ({x: a, x: b}) => {};
}
