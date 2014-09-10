/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertTrue, assertThrows,
} = Assert;

// 9.3.12 (OrdinaryHasInstance): non-callable objects and bound functions
// https://bugs.ecmascript.org/show_bug.cgi?id=1162

assertThrows(TypeError, () => [] instanceof {});

function F() {}
assertTrue((new F) instanceof F.bind(null));
