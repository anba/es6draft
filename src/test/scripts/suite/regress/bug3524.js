/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// 7.3.21 SpeciesConstructor: Return defaultConstructor instead of throwing when the constructor property is absent?
// https://bugs.ecmascript.org/show_bug.cgi?id=3524

// .constructor is undefined
{
  class TypedArray extends Int8Array {
    get ["constructor"]() {
      return void 0;
    }
  }
  let ta = TypedArray.of(1, 2, 3).map(x => x);
  assertSame(Int8Array.prototype, Object.getPrototypeOf(ta));
}

// .constructor is null
{
  class TypedArray extends Int8Array {
    get ["constructor"]() {
      return null;
    }
  }
  assertThrows(TypeError, () => TypedArray.of(1, 2, 3).map(x => x));
}

// @@species is undefined or null
for (let species of [void 0, null]) {
  class TypedArray extends Int8Array {
    get ["constructor"]() {
      return {[Symbol.species]: species};
    }
  }
  let ta = TypedArray.of(1, 2, 3).map(x => x);
  assertSame(Int8Array.prototype, Object.getPrototypeOf(ta));
}

// @@species is neither undefined nor null
for (let species of [Int16Array, class extends Int32Array {}]) {
  class TypedArray extends Int8Array {
    get ["constructor"]() {
      return {[Symbol.species]: species};
    }
  }
  let ta = TypedArray.of(1, 2, 3).map(x => x);
  assertSame(species.prototype, Object.getPrototypeOf(ta));
}
