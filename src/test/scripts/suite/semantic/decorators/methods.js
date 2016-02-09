/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertDataProperty,
} = Assert;

function CreateDecorator() {
  "use strict";
  let called = false, parameters;
  return {
    decorator(...rest) {
      assertUndefined(this);
      assertFalse(called);
      called = true;
      parameters = rest;
    },
    called() {
      return called;
    },
    parameters() {
      return parameters;
    },
  };
}

// Without return
{
  function nonEnumerable(obj, pk, desc) {
    Object.assign(desc, {enumerable: false});
  }

  function enumerable(obj, pk, desc) {
    Object.assign(desc, {enumerable: true});
  }

  let obj = { @enumerable m() {}, @nonEnumerable n() {} };
  assertDataProperty(obj, "m", {value: obj.m, writable: true, enumerable: true, configurable: true});
  assertDataProperty(obj, "n", {value: obj.n, writable: true, enumerable: false, configurable: true});

  class C { @enumerable m() {}; @nonEnumerable n() {} };
  assertDataProperty(C.prototype, "m", {value: C.prototype.m, writable: true, enumerable: true, configurable: true});
  assertDataProperty(C.prototype, "n", {value: C.prototype.n, writable: true, enumerable: false, configurable: true});
}

// With return
{
  function nonEnumerable(obj, pk, desc) {
    return Object.assign(desc, {enumerable: false});
  }

  function enumerable(obj, pk, desc) {
    return Object.assign(desc, {enumerable: true});
  }

  let obj = { @enumerable m() {}, @nonEnumerable n() {} };
  assertDataProperty(obj, "m", {value: obj.m, writable: true, enumerable: true, configurable: true});
  assertDataProperty(obj, "n", {value: obj.n, writable: true, enumerable: false, configurable: true});

  class C { @enumerable m() {}; @nonEnumerable n() {} };
  assertDataProperty(C.prototype, "m", {value: C.prototype.m, writable: true, enumerable: true, configurable: true});
  assertDataProperty(C.prototype, "n", {value: C.prototype.n, writable: true, enumerable: false, configurable: true});
}

// Parameterized
{
  function enumerable(enumerable) {
    return (obj, pk, desc) => Object.assign(desc, {enumerable});
  }

  let obj = { @enumerable(true) m() {}, @enumerable(false) n() {} };
  assertDataProperty(obj, "m", {value: obj.m, writable: true, enumerable: true, configurable: true});
  assertDataProperty(obj, "n", {value: obj.n, writable: true, enumerable: false, configurable: true});

  class C { @enumerable(true) m() {}; @enumerable(false) n() {} };
  assertDataProperty(C.prototype, "m", {value: C.prototype.m, writable: true, enumerable: true, configurable: true});
  assertDataProperty(C.prototype, "n", {value: C.prototype.n, writable: true, enumerable: false, configurable: true});
}
