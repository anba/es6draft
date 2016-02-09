/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

var code = `
  var a = b = c = d = () => {};
  return do { ${"a();b();c();d();".repeat(45)} ${"0;".repeat(100)} };
`;

assertSame(0, Function(code)());
