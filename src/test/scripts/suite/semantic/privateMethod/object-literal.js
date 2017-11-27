/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

assertSyntaxError(`
var obj = {
  #m() {}
};
`);

assertSyntaxError(`
var obj = {
  get #m() {}
};
`);

assertSyntaxError(`
var obj = {
  set #m(v) {}
};
`);
