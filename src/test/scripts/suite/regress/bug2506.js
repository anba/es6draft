/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertSyntaxError
} = Assert;

// 12.1.5, 14.2: Tweak grammar to allow CoverInitialisedName and duplicate PropertyNames in ArrowParameters?
// https://bugs.ecmascript.org/show_bug.cgi?id=2506

// Duplicate property names in object literal in strict mode
function strictMode() {
  "use strict";
  ({x: a, x: b}) => {};
  ({x: a, x: b} = {}) => {};
}

// CoverInitializedName in CoverParenthesizedExpressionAndArrowParameterList for ArrowParameters
({x = 0}) => {};
({x = 0}, {y = 0}) => {};
({x = 0} = {}) => {};
({x = 0} = {}, {y = 0} = {}) => {};
