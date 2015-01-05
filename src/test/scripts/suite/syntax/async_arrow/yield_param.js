/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

function assertNoSyntaxError(source) {
  return Function(source);
}

// (non-strict, strict) function, yield without escape
assertNoSyntaxError(`function f() { async yield => {} }`);
assertSyntaxError(`"use strict"; function f() { async yield => {} }`);

// (non-strict, strict) generator, yield without escape
assertSyntaxError(`function* g() { async yield => {} }`);
assertSyntaxError(`"use strict"; function* g() { async yield => {} }`);

// (non-strict, strict) function, yield with escape
assertNoSyntaxError(String.raw `function f() { async yi\u0065ld => {} }`);
assertSyntaxError(String.raw `"use strict"; function f() { async yi\u0065ld => {} }`);

// (non-strict, strict) generator, yield with escape
assertSyntaxError(String.raw `function* g() { async yi\u0065ld => {} }`);
assertSyntaxError(String.raw `"use strict"; function* g() { async yi\u0065ld => {} }`);
