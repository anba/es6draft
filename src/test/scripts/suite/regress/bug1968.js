/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// 12.1.5: "yield" should be allowed in Object Initialisers
// https://bugs.ecmascript.org/show_bug.cgi?id=1968

function f() {
  let obj = ({yield});
}

assertSyntaxError(`"use strict"; let obj = ({yield});`);
