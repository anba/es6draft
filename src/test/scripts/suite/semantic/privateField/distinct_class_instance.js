/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

function create(v) {
  return class {
    #private = v;

    static get(o) {
      return o.#private;
    }
  }
}

let c1 = create(1);
let c2 = create(2);

let o1 = new c1();
let o2 = new c2();

assertSame(1, c1.get(o1));
assertSame(2, c2.get(o2));

assertThrows(TypeError, () => c1.get(o2));
assertThrows(TypeError, () => c2.get(o1));
