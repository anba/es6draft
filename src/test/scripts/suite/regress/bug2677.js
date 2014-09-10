/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows
} = Assert;

// 19.2.1.1 Function, 25.2.1.1 GeneratorFunction: Invalid assertion in step 18/17
// https://bugs.ecmascript.org/show_bug.cgi?id=2677

let f = Function[Symbol.create]();
Object.preventExtensions(f);
assertThrows(TypeError, () => Function.call(f, ""));

const GeneratorFunction = (function*(){}).constructor;
let g = GeneratorFunction[Symbol.create]();
Object.preventExtensions(g);
assertThrows(TypeError, () => GeneratorFunction.call(g, ""));
