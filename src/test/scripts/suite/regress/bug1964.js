/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// 13.7, 13.8, 13.12: "yield" as label
// https://bugs.ecmascript.org/show_bug.cgi?id=1964

function f() { yield: ; }
assertSyntaxError(`function* g() { yield: ; }`);
assertSyntaxError(`"use strict"; function f() { yield: ; }`);
assertSyntaxError(`"use strict"; function* g() { yield: ; }`);
