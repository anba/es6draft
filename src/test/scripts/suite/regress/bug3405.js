/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertFalse, assertThrows
} = Assert;

// 25.2.4 GeneratorFunction: Add note about [[CreateAction]] after MakeConstructor
// https://bugs.ecmascript.org/show_bug.cgi?id=3405

const GeneratorFunction = function*(){}.constructor;

let GClone = GeneratorFunction.bind(null);
Object.defineProperty(GClone, "prototype", {
  value: Array
});

let g = Reflect.construct(GeneratorFunction, ["yield this"], GClone);
assertSame(Array, Object.getPrototypeOf(g));

assertThrows(TypeError, () => new g());
