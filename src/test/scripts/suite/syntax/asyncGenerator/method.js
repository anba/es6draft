/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

var o = {
  async* m() {},
  async* [Math.random()]() {},
};

class C {
  async* m() {}
  static async* m() {}
}

assertSyntaxError(`
class C {
  async* constructor() {}
}
`);
