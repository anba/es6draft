/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, fail
} = Assert;

// Remove use of @@species from Promise.all/race
// https://github.com/tc39/ecma262/pull/211

var obj = {};

function C(exec) {
  exec(() => {}, () => {});
  return obj;
}

Object.defineProperty(C, Symbol.species, {
  get() {
    fail `@@species getter called`;
  }
});

assertSame(obj, Promise.all.call(C, []));
assertSame(obj, Promise.all.call(C, [{}]));

assertSame(obj, Promise.race.call(C, []));
assertSame(obj, Promise.race.call(C, [{}]));
