/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame,
} = Assert;

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

function CreateDataPropertyOrThrow(object, propertyKey, value) {
  Object.defineProperty(object, propertyKey, {value, writable: true, enumerable: true, configurable: true});
}

function setPropertyValue(description, object, propertyKey, value, expected = value) {
  object[propertyKey] = value;
  var actual = object[propertyKey];
  assertSame(numberToRawBits(expected), numberToRawBits(actual),
    `${description}: ${numberToRawBits(expected).toString(16)} != ${numberToRawBits(actual).toString(16)}`
  );
}

function testSet(description, object, propertyKey) {
  CreateDataPropertyOrThrow(object, propertyKey, 0);

  setPropertyValue(description, object, propertyKey, 0/0);
  setPropertyValue(description, object, propertyKey, NaN, 0/0);
  setPropertyValue(description, object, propertyKey, Number.NaN, 0/0);
  setPropertyValue(description, object, propertyKey, Math.sqrt(-1), 0/0);
  setPropertyValue(description, object, propertyKey, Math.sin(Infinity), 0/0);
  setPropertyValue(description, object, propertyKey, Math.abs(0/0), 0/0);
}

function testSetMappedArguments(description, object, propertyKey) {
  CreateDataPropertyOrThrow(object, propertyKey, 0);

  setPropertyValue(description, object, propertyKey, 0/0);
  setPropertyValue(description, object, propertyKey, NaN);
  setPropertyValue(description, object, propertyKey, Number.NaN);
  setPropertyValue(description, object, propertyKey, Math.sqrt(-1));
  setPropertyValue(description, object, propertyKey, Math.sin(Infinity));
  setPropertyValue(description, object, propertyKey, Math.abs(0/0));
}

function* limits(v, d = 1) {
  for (let i = v - d; i < v; ++i) yield i;
  yield v;
  for (let i = v; i < (v + d); ++i) yield i + 1;
}

const MAX_INT32 = 0x7fffffff;
const MAX_UINT32 = 0xffffffff;
const numericPropertyKeys = [
  ...limits(0),
  ...limits(MAX_INT32),
  ...limits(MAX_UINT32),
  ...limits(Number.MAX_SAFE_INTEGER),
];
const propertyKeys = ["p", Symbol(), ...numericPropertyKeys];
const objects = {
  __proto__: null,
  object() { return {} },
  array() { return [] },
  mappedArgumentsNoParams() { return function(){return arguments}() },
  mappedArgumentsNoMapped() { return function(a){return arguments}() },
  mappedArguments() { return function(a){return arguments}(123) },
  unmappedArgumentsNoParams() { return function(){"use strict"; return arguments}() },
  unmappedArgumentsNoMapped() { return function(a){"use strict"; return arguments}() },
  unmappedArguments() { return function(a){"use strict"; return arguments}(123) },
};

for (let k in objects) {
  if (k === "mappedArguments") {
    propertyKeys.filter(p => (p !== 0))
                .forEach(p => testSet(k, objects[k](), p));
    testSetMappedArguments(k, objects[k](), 0);
  } else {
    propertyKeys.forEach(p => testSet(k, objects[k](), p));
  }
}
