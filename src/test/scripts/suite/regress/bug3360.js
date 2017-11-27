/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 12.3.5.* Evaluation : Typo and missing ReturnIfAbrupt
// https://bugs.ecmascript.org/show_bug.cgi?id=3360


class ValidError extends Error {}
class InvalidError extends Error {}

assertThrows(ValidError, () => {
  var property = () => { throw new ValidError };
  var value = () => { throw new InvalidError };

  new class Derived extends class Base {} {
    m() {
      super[property()] = value();
    }
  }().m();
});

assertThrows(ValidError, () => {
  var property = () => { throw new ValidError };
  var value = () => { throw new InvalidError };

  new class Derived extends null {
    constructor() {
      return Object.create(new.target.prototype);
    }
    m() {
      super[property()] = value();
    }
  }().m();
});

assertThrows(ValidError, () => {
  var property = () => { return {toString() { throw new ValidError }} };
  var value = () => { throw new InvalidError };

  new class Derived extends class Base {} {
    m() {
      super[property()] = value();
    }
  }().m();
});

assertThrows(ValidError, () => {
  var property = () => { return {toString() { throw new ValidError }} };
  var value = () => { throw new InvalidError };

  new class Derived extends null {
    constructor() {
      return Object.create(new.target.prototype);
    }
    m() {
      super[property()] = value();
    }
  }().m();
});

assertThrows(ValidError, () => {
  var property = () => { return "" };
  var value = () => { throw new ValidError };

  new class Derived extends class Base { set "" (v) { throw new InvalidError } } {
    m() {
      super[property()] = value();
    }
  }().m();
});

assertThrows(TypeError, () => {
  var property = () => { return "" };
  var value = () => { throw new InvalidError };

  new class Derived extends null {
    constructor() {
      return Object.create(new.target.prototype);
    }
    m() {
      super[property()] = value();
    }
  }().m();
});
