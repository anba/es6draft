/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertNotSame
} = Assert;

let a = [];
let o = {length: null};
Object.setPrototypeOf(a, o);

for (let k in a) {
  assertNotSame("length", k);
}
