/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertSyntaxError
} = Assert;

// 13.2.1.1: Missing early error restriction for "let" in "LexicalBinding : BindingPattern"
// https://bugs.ecmascript.org/show_bug.cgi?id=2295

assertSyntaxError(`let let = null`);
assertSyntaxError(`let [let] = null`);
assertSyntaxError(`let {let} = null`);
assertSyntaxError(`let {a: let} = null`);
{
  let {let: a} = {let: 0};
  assertSame(0, a);
}
