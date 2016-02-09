/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertTrue
} = Assert;

// 6.1.6 The Number Type: Paragraph about NaN value detection requires updates
// https://bugs.ecmascript.org/show_bug.cgi?id=3508

function numberToRawBits(v) {
  var isLittleEndian = new Uint8Array(new Uint16Array([1]).buffer)[0] === 1;
  var reduce = Array.prototype[isLittleEndian ? 'reduceRight' : 'reduce'];
  var float64 = new Float64Array(1);
  float64[0] = v;
  var uint8 = new Uint8Array(float64.buffer);
  return reduce.call(uint8, function(a, v) {
    return (a * 256) + v;
  }, 0);
}

var o = {};
Object.defineProperty(o, "p", {value: 0, writable: true, enumerable: true, configurable: true});

// Initial NaN value
o.p = 0 / 0;

// Test NaN values
var nans = [
  NaN, Number.NaN, Math.sqrt(-1), Math.sin(Infinity), Math.abs(0 / 0),
];

var foundDifferent = false;
for (let nan of nans) {
  foundDifferent = foundDifferent || (numberToRawBits(0 / 0) !== numberToRawBits(nan));
  o.p = nan;
  assertSame(numberToRawBits(0 / 0), numberToRawBits(o.p));
}
assertTrue(foundDifferent);
