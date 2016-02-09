/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 24.3.1.1 Walk Abstract Operation: Missing ReturnIfAbrupt after step 3.b.i
// https://bugs.ecmascript.org/show_bug.cgi?id=3535

class Err extends Error {}

let json = '{"a": null, "b": null}';

function reviver(name, value) {
  if (name === "a") {
    this.b = new Proxy({}, {
      ownKeys() { throw new Err }
    });
  }
  return value;
}

assertThrows(Err, () => JSON.parse(json, reviver));
