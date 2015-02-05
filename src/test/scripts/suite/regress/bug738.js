/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 15.14.1.1: algorithm control flow
// https://bugs.ecmascript.org/show_bug.cgi?id=738

assertThrows(ReferenceError, () => {
  new class extends Map {
    constructor() {
      let iter = {[Symbol.iterator]: () => {
        super();
        return [].values();
      }};
      super(iter);
    }
  }
});
