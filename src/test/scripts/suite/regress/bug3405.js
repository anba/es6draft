/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertFalse
} = Assert;

// 25.2.4 GeneratorFunction: Add note about [[CreateAction]] after MakeConstructor
// https://bugs.ecmascript.org/show_bug.cgi?id=3405

const GeneratorFunction = function*(){}.constructor;

let GClone = GeneratorFunction.toMethod({});
Object.defineProperty(GClone, "prototype", {
  value: Array
});

let g = new GClone("yield this");
assertSame(Array, Object.getPrototypeOf(g));

let gen = new g();
let {value: result} = gen.next();
assertSame(gen, result);
assertFalse(Array.isArray(result));
assertSame(g.prototype, Object.getPrototypeOf(result));
assertSame(GeneratorFunction.prototype.prototype, Object.getPrototypeOf(Object.getPrototypeOf(result)));
