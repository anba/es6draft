/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 12.13: throw statement evaluates reference two times
// https://bugs.ecmascript.org/show_bug.cgi?id=1632

let counter = 0;
let o = {
  get prop() {
    counter += 1;
  }
};

try {
  throw o.prop;
} catch (e) {
}

assertSame(1, counter);
