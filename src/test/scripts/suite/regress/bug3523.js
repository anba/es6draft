/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// 7.3.21 SpeciesConstructor and 9.4.2.3 ArraySpeciesCreate: discrepancy on the condition under which the @@species property is examined
// https://bugs.ecmascript.org/show_bug.cgi?id=3523

// .constructor present but not a Constructor function, @@species not defined
for (let ctor of [void 0, () => {}, {}]) {
  class MyArray extends Array {
    get ["constructor"]() {
      return ctor;
    }
  }
  let array = MyArray.of(1, 2, 3).map(x => x);
  assertSame(Array.prototype, Object.getPrototypeOf(array));
}

// .constructor present but primitive except for undefined, @@species not defined
for (let ctor of [null, false, true, 0, 1, "", Symbol()]) {
  class MyArray extends Array {
    get ["constructor"]() {
      return ctor;
    }
  }
  assertThrows(TypeError, () => MyArray.of(1, 2, 3).map(x => x));
}

// .constructor present and a Constructor function, @@species not defined
for (let ctor of [Array, Date, class {}]) {
  class MyArray extends Array {
    get ["constructor"]() {
      return ctor;
    }
  }
  let array = MyArray.of(1, 2, 3).map(x => x);
  assertSame(Array.prototype, Object.getPrototypeOf(array));
}

// .constructor and @@species defined, @@species undefined
for (let species of [void 0]) {
  class MyArray extends Array {
    get ["constructor"]() {
      return {[Symbol.species]: species};
    }
  }
  let array = MyArray.of(1, 2, 3).map(x => x);
  assertSame(Array.prototype, Object.getPrototypeOf(array));
}

// .constructor and @@species defined, @@species primitive except for undefined
for (let species of [null, false, true, 0, 1, "", Symbol()]) {
  class MyArray extends Array {
    get ["constructor"]() {
      return {[Symbol.species]: species};
    }
  }
  assertThrows(TypeError, () => MyArray.of(1, 2, 3).map(x => x));
}

// .constructor and @@species defined, and @@species a Constructor function
for (let species of [Array, Date, class {}]) {
  class MyArray extends Array {
    get ["constructor"]() {
      return {[Symbol.species]: species};
    }
  }
  let array = MyArray.of(1, 2, 3).map(x => x);
  assertSame(species.prototype, Object.getPrototypeOf(array));
}

// .constructor and @@species defined, and @@species not a Constructor function
for (let species of [{}, () => {}]) {
  class MyArray extends Array {
    get ["constructor"]() {
      return {[Symbol.species]: species};
    }
  }
  assertThrows(TypeError, () => MyArray.of(1, 2, 3).map(x => x));
}
