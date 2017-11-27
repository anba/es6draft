/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// Abrupt completions not handled for HasProperty?
// https://bugs.ecmascript.org/show_bug.cgi?id=4276

class MyError extends Error { }

function desc(o, attribute) {
  return new Proxy(o, {
    has(t, pk) {
      if (pk === attribute) throw new MyError;
      return Reflect.has(t, pk);
    }
  });
}

var dataProperty = {value: 0, writable: true, enumerable: true, configurable: true};
var accessorProperty = {get(){}, set(){}, enumerable: true, configurable: true};

assertThrows(MyError, () => Object.defineProperty({}, "", desc(dataProperty, "value")));
assertThrows(MyError, () => Object.defineProperty({}, "", desc(dataProperty, "writable")));
assertThrows(MyError, () => Object.defineProperty({}, "", desc(dataProperty, "enumerable")));
assertThrows(MyError, () => Object.defineProperty({}, "", desc(dataProperty, "configurable")));

assertThrows(MyError, () => Object.defineProperty({}, "", desc(accessorProperty, "get")));
assertThrows(MyError, () => Object.defineProperty({}, "", desc(accessorProperty, "set")));
assertThrows(MyError, () => Object.defineProperty({}, "", desc(accessorProperty, "enumerable")));
assertThrows(MyError, () => Object.defineProperty({}, "", desc(accessorProperty, "configurable")));
