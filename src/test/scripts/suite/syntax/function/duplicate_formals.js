/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

function f(a = () => { function g(a, a) {} }) {}
assertSyntaxError(`"use strict"; function f(a = () => { function g(a, a) {} }) {}`);
assertSyntaxError(`function f(a = () => { function g(a, a) {} }) { "use strict" }`);
