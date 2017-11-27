/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 19.1.2.14 Object.keys: Missing ReturnIfAbrupt after step 3
// https://bugs.ecmascript.org/show_bug.cgi?id=3635

class Err extends Error {}
var proxy = new Proxy({}, {ownKeys() { throw new Err }});

assertThrows(Err, () => Object.keys(proxy));
