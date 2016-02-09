/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// ArrowParameters not strict
// https://bugs.ecmascript.org/show_bug.cgi?id=4152

var f = (package) => {};
var f = (package) => "use strict";
assertSyntaxError(`"use strict"; var f = (package) => {};`);
assertSyntaxError(`var f = (package) => { "use strict"; };`);
