/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 19.2.1.1, 25.2.1.1: Add HasOwnProperty() check for "name"
// https://bugs.ecmascript.org/show_bug.cgi?id=2335

function create(c, f, ...args) {
  let o = new class extends c { constructor() { /* no super */ } };
  f(o);
  return c.call(o, ...args);
}

// assertSame("anonymous", create(Function, () => {}).name);
// assertSame("asdf", create(Function, f => { Object.defineProperty(f, "name", {value: "asdf"}) }).name);

let GeneratorFunction = function*(){}.constructor;
// assertSame("anonymous", create(GeneratorFunction, () => {}).name);
// assertSame("asdf", create(GeneratorFunction, f => { Object.defineProperty(f, "name", {value: "asdf"}) }).name);
