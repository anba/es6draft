/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertThrows
} = Assert;

// 7.1.14 ToPropertyKey: accept objects whose primitive value is a symbol
// https://bugs.ecmascript.org/show_bug.cgi?id=3252

var stringKey = "str";
var symbolKey = Symbol();
var symbolObject = Object(symbolKey);
var obj = {
  [symbolKey]: 0,
  [stringKey]: 1,
};

assertSame("symbol", typeof symbolKey);
assertSame(0, obj[symbolKey]);

assertSame("object", typeof symbolObject);
assertSame(0, obj[symbolObject]);

// Hide inherited Symbol.toPrimitive, valueOf and toString
Object.defineProperty(symbolObject, Symbol.toPrimitive, {
  value: null, configurable: true, writable: true,
});
Object.defineProperty(symbolObject, "valueOf", {
  value: null, configurable: true, writable: true,
});
Object.defineProperty(symbolObject, "toString", {
  value: null, configurable: true, writable: true,
});

symbolObject.valueOf = () => symbolKey;
assertSame(0, obj[symbolObject]);

symbolObject.valueOf = () => stringKey;
assertSame(1, obj[symbolObject]);

symbolObject.toString = () => symbolKey;
assertSame(0, obj[symbolObject]);

symbolObject.toString = () => stringKey;
assertSame(1, obj[symbolObject]);

symbolObject[Symbol.toPrimitive] = () => symbolKey;
assertSame(0, obj[symbolObject]);

symbolObject[Symbol.toPrimitive] = () => stringKey;
assertSame(1, obj[symbolObject]);

assertSame(0, obj[{valueOf: () => symbolKey, toString: null}]);
assertSame(1, obj[{valueOf: () => stringKey, toString: null}]);

assertSame(0, obj[{toString: () => symbolKey}]);
assertSame(1, obj[{toString: () => stringKey}]);

assertSame(0, obj[{[Symbol.toPrimitive]: () => symbolKey}]);
assertSame(1, obj[{[Symbol.toPrimitive]: () => stringKey}]);
