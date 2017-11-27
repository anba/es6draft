/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 23.2.1.1, 23.3.1.1, 23.4.1.1: Missing constructor reentrancy checks
// https://bugs.ecmascript.org/show_bug.cgi?id=2397

assertThrows(ReferenceError, () => {
  new class extends Set {
    constructor() {
      let iter = {[Symbol.iterator]: () => {
        super();
        return [].values();
      }};
      super(iter);
    }
  }
});

assertThrows(ReferenceError, () => {
  new class extends WeakMap {
    constructor() {
      let iter = {[Symbol.iterator]: () => {
        super();
        return [].values();
      }};
      super(iter);
    }
  }
});

assertThrows(ReferenceError, () => {
  new class extends WeakSet {
    constructor() {
      let iter = {[Symbol.iterator]: () => {
        super();
        return [].values();
      }};
      super(iter);
    }
  }
});
