/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// 7.3.19 SpeciesConstructor, 9.4.2.3 ArraySpeciesCreate: inconsistency in the handling of originalObj.constructor === null
// https://bugs.ecmascript.org/show_bug.cgi?id=3577

for (let ctor of [void 0]) {
  class MyArray extends Array {
    get ["constructor"]() {
      return ctor;
    }
  }
  let array = MyArray.of(1, 2, 3).map(x => x);
  assertSame(Array.prototype, Object.getPrototypeOf(array));
}

for (let ctor of [null, "", 0, false, Symbol.iterator]) {
  class MyArray extends Array {
    get ["constructor"]() {
      return ctor;
    }
  }
  assertThrows(TypeError, () => MyArray.of(1, 2, 3).map(x => x));
}

for (let species of [void 0, null]) {
  class MyArray extends Array {
    get ["constructor"]() {
      return {[Symbol.species]: species};
    }
  }
  let array = MyArray.of(1, 2, 3).map(x => x);
  assertSame(Array.prototype, Object.getPrototypeOf(array));
}

for (let species of ["", 0, false, Symbol.iterator, {}, () => {}]) {
  class MyArray extends Array {
    get ["constructor"]() {
      return {[Symbol.species]: species};
    }
  }
  assertThrows(TypeError, () => MyArray.of(1, 2, 3).map(x => x));
}
