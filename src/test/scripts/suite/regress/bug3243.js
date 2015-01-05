/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// 12.1.1 Early Errors: Inconsistent handling of escape sequence in keywords
// https://bugs.ecmascript.org/show_bug.cgi?id=3243

assertSyntaxError(String.raw `\u0069f;`);
assertSyntaxError(String.raw `"use strict"; \u0069f;`);

Function(String.raw `\u0069mplements;`);
assertSyntaxError(String.raw `"use strict"; \u0069mplements;`);
