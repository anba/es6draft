/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 19.2.1.1, 25.2.1.1: Abrupt completion for MakeConstructor ignored
// https://bugs.ecmascript.org/show_bug.cgi?id=2248

function create(c, f, ...args) {
  let o = c[Symbol.create]();
  f(o);
  return c.call(o, ...args);
}

let GeneratorFunction = function*(){}.constructor;

assertThrows(TypeError, () => create(Function, f => { Object.defineProperty(f, "prototype", {}) }));
assertThrows(TypeError, () => create(GeneratorFunction, f => { Object.defineProperty(f, "prototype", {}) }));
