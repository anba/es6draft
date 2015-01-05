/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 8.1.6.2: [[Construct]] may also return non-object types
// https://bugs.ecmascript.org/show_bug.cgi?id=1422

// function F(){}
// Object.defineProperty(F, Symbol.create, {value() { return null }});
// assertThrows(TypeError, () => new F);

let P = new Proxy(function(){}, {construct() { return null }});
assertThrows(TypeError, () => new P);
