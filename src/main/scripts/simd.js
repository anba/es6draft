/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
(function SIMD() {
"use strict";

// Return early if SIMD not enabled.
if (!%IsCompatibilityOptionEnabled("SIMD")) return;

const global = %GlobalTemplate();

const {
  SIMD,
  Object,
  Reflect: {
    apply: Reflect_apply
  }
} = global;

const {
  Float64x2, Float32x4,
  Int32x4, Int16x8, Int8x16,
  Uint32x4, Uint16x8, Uint8x16,
  Bool64x2, Bool32x4, Bool16x8, Bool8x16,
} = SIMD;

// shiftRightArithmeticByScalar()
for (const type of [Int32x4, Int16x8, Int8x16]) {
  const {shiftRightByScalar} = type;

  Object.defineProperty(Object.assign(type, {
    shiftRightArithmeticByScalar(a, bits) {
      return %CallFunction(shiftRightByScalar, this, a, bits);
    }
  }), "shiftRightArithmeticByScalar", {enumerable: false});
}
for (const type of [Uint32x4, Uint16x8, Uint8x16]) {
  const signed = type === Uint32x4 ? Int32x4 : type === Uint16x8 ? Int16x8 : Int8x16;
  const {[`from${type.name}Bits`]: toSigned, shiftRightByScalar: signedShift} = signed;
  const {[`from${signed.name}Bits`]: toUnsigned} = type;

  Object.defineProperty(Object.assign(type, {
    shiftRightArithmeticByScalar(a, bits) {
      return toUnsigned(signedShift(toSigned(a), bits));
    }
  }), "shiftRightArithmeticByScalar", {enumerable: false});
}

// shiftRightLogicalByScalar()
for (const type of [Int32x4, Int16x8, Int8x16]) {
  const {check, extractLane, splat, shiftRightByScalar, length} = type;
  const unsigned = type === Int32x4 ? Uint32x4 : type === Int16x8 ? Uint16x8 : Uint8x16;
  const {[`from${type.name}Bits`]: toUnsigned, shiftRightByScalar: unsignedShift} = unsigned;
  const {[`from${unsigned.name}Bits`]: toSigned} = type;
  const elementSize = (16 / length)|0;

  Object.defineProperty(Object.assign(type, {
    shiftRightLogicalByScalar(a, bits) {
      // TODO: Enable the following line when SpiderMonkey slr is fixed.
      // TODO: shiftRightLogicalByScalar will be removed in https://bugzilla.mozilla.org/show_bug.cgi?id=1201934
      // return toSigned(unsignedShift(toUnsigned(a), bits));
      check(a);
      const scalar = bits >>> 0;
      if (scalar >= elementSize * 8) {
        return splat(0);
      }
      let array = [];
      for (let i = 0; i < length; ++i) {
        // This shift is not correct (missing bit mask), but expected by jstests.
        %CreateDataPropertyOrThrow(array, i, extractLane(a, i) >>> scalar);
      }
      return Reflect_apply(type, null, array);
    }
  }), "shiftRightLogicalByScalar", {enumerable: false});
}
for (const type of [Uint32x4, Uint16x8, Uint8x16]) {
  const {shiftRightByScalar} = type;

  Object.defineProperty(Object.assign(type, {
    shiftRightLogicalByScalar(a, bits) {
      return %CallFunction(shiftRightByScalar, this, a, bits);
    }
  }), "shiftRightLogicalByScalar", {enumerable: false});
}

// toSource()
for (const type of [
  Float64x2, Float32x4,
  Int32x4, Int16x8, Int8x16,
  Uint32x4, Uint16x8, Uint8x16,
  Bool64x2, Bool32x4, Bool16x8, Bool8x16,
]) {
  const proto = type.prototype;
  const toString = proto.toString;
  Object.defineProperty(Object.assign(proto, {
    toSource() {
      return %CallFunction(toString, this);
    }
  }), "toSource", {enumerable: false});
}

})();
