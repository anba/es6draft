/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
traceur = {
  get(name) {
    if (name !== "./Options.js") {
      throw new Error("unknown module: " + name);
    }
    class Options {
      get symbols() {
        return true;
      }
    }
    return {Options};
  }
};

$traceurRuntime = {
  // 7.1.13 ToObject
  toObject(value) {
    if (value == null) {
      throw TypeError();
    }
    return Object(value);
  },
  options: {
    symbols: true,
  },
};

assert = chai.assert;

assert.type = function(actual, type) {
  assert.typeOf(actual, type.name);
  return actual;
};

AssertionError = chai.AssertionError;

function fail(message) {
  throw new AssertionError(message);
}

function assertHasOwnProperty(object, ...keys) {
  keys = keys.filter(key => !Object.prototype.hasOwnProperty.call(object, key));
  if (keys.length) {
    fail(`Expected properties not found: ${keys.map(String).join(", ")}`);
  }
}

function assertLacksOwnProperty(object, ...keys) {
  keys = keys.filter(key => Object.prototype.hasOwnProperty.call(object, key));
  if (keys.length) {
    fail(`Unexpected properties found: ${keys.map(String).join(', ')}`);
  }
}

function assertNoOwnProperties(object) {
  let names = Object.getOwnPropertyNames(object);
  if (names.length) {
    fail(`Unexpected properties found: ${names.join(', ')}`);
  }
}

function assertArrayEquals(expected, actual) {
  assert.equal(JSON.stringify(actual, null, 2), JSON.stringify(expected, null, 2));
}
