/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

// 12.13.4: "yield" should be allowed in Destructuring Assignment
// https://bugs.ecmascript.org/show_bug.cgi?id=1971

{
  let [yield] = [0];
  assertSame(0, yield);
}

{
  let {yield} = {yield: 0};
  assertSame(0, yield);
}

{
  let {a: yield} = {a: 0};
  assertSame(0, yield);
}
