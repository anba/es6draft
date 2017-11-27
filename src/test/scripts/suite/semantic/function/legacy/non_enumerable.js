/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertNotSame
} = Assert;

let f = function(){};
let o = {length: null, caller: null};
Object.setPrototypeOf(f, o);

for (let k in f) {
  assertNotSame("length", k);
  assertNotSame("caller", k);
}
