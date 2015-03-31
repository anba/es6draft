/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 21.2.3.2.1 RegExpAlloc: Missing ReturnIfAbrupt after step 1
// https://bugs.ecmascript.org/show_bug.cgi?id=3712

class Err extends Error { }

var C = Object.defineProperty(function(){}.bind(), "prototype", {
  get() { throw new Err() }
});

assertThrows(Err, () => Reflect.construct(RegExp, [], C));
