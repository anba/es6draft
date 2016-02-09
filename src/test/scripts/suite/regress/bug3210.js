/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// 14.5.1 note banning "let" as class name is unnecessary
// https://bugs.ecmascript.org/show_bug.cgi?id=3210

assertSyntaxError(`class let {}`);
assertSyntaxError(`(class let {});`);
