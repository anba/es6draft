/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 22.2.1.2 %TypedArray%: Missing ReturnIfAbrupt after step 18c
// https://bugs.ecmascript.org/show_bug.cgi?id=4229

class MyError extends Error { }

var ctor = function(){}.bind(null);
Object.defineProperty(ctor, Symbol.species, {value: ctor});
Object.defineProperty(ctor, "prototype", {get(){ throw new MyError }});

var ta = new Uint8Array(0);
ta.buffer.constructor = ctor;

assertThrows(MyError, () => new Int8Array(ta));
