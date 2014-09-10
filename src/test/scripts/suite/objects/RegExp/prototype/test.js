/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertTrue, assertFalse, assertThrows
} = Assert;

// RegExp.prototype.test delegates to "exec"

// Test normal delegation, non-null return value is true, null is false
{
  for (let value of [void 0, null, true, false, 0, 1, "", "abc", {}, [], () => {}]) {
    let execCalled = false;
    let execResult = value !== null ? {} : null;
    let object = {
      test: RegExp.prototype.test,
      exec(v, ...more) {
        assertFalse(execCalled);
        execCalled = true;
        assertSame(object, this);
        assertSame(String(value), v);
        assertSame(0, more.length);
        return execResult;
      }
    };
    let result = object.test(value);
    assertTrue(execCalled);
    if (value !== null) {
      assertTrue(result);
    } else {
      assertFalse(result);
    }
  }

  class ExecCalledError extends Error { }
  for (let value of [Symbol()]) {
    let object = {
      test: RegExp.prototype.test,
      exec(v, ...more) { throw new ExecCalledError }
    };
    assertThrows(TypeError, () => object.test(value));
  }
}

// Test delegation with RegExp object
{
  for (let value of [void 0, null, true, false, 0, 1, "", "abc", {}, [], () => {}]) {
    let execCalled = false;
    let execResult = value !== null ? {} : null;
    let object = Object.assign(/abc/, {
      exec(v, ...more) {
        assertFalse(execCalled);
        execCalled = true;
        assertSame(object, this);
        assertSame(String(value), v);
        assertSame(0, more.length);
        return execResult;
      }
    });
    let result = object.test(value);
    assertTrue(execCalled);
    if (value !== null) {
      assertTrue(result);
    } else {
      assertFalse(result);
    }
  }

  class ExecCalledError extends Error { }
  for (let value of [Symbol()]) {
    let object = Object.assign(/abc/, {
      exec(v, ...more) { throw new ExecCalledError }
    });
    assertThrows(TypeError, () => object.test(value));
  }
}

// Test delegation with RegExp object, RegExp.prototype.exec replaced
{
  const RegExp_prototype_exec = RegExp.prototype.exec;
  for (let value of [void 0, null, true, false, 0, 1, "", "abc", {}, [], () => {}]) {
    let execCalled = false;
    let execResult = value !== null ? {} : null;
    let object = /abc/;
    RegExp.prototype.exec = function exec(v, ...more) {
      assertFalse(execCalled);
      execCalled = true;
      assertSame(object, this);
      assertSame(String(value), v);
      assertSame(0, more.length);
      return execResult;
    };
    let result = object.test(value);
    assertTrue(execCalled);
    if (value !== null) {
      assertTrue(result);
    } else {
      assertFalse(result);
    }
  }

  class ExecCalledError extends Error { }
  for (let value of [Symbol()]) {
    let object = /abc/;
    RegExp.prototype.exec = function exec(v, ...more) {
      throw new ExecCalledError
    };
    assertThrows(TypeError, () => object.test(value));
  }

  RegExp.prototype.exec = RegExp_prototype_exec;
}

// Test no TypeError when RegExp.prototype.exec was deleted
{
  const RegExp_prototype_exec = RegExp.prototype.exec;
  let deleteResult = delete RegExp.prototype.exec;
  assertTrue(deleteResult);
  assertFalse(/abc/.test(""));
  assertTrue(/def/.test("def"));
  RegExp.prototype.exec = RegExp_prototype_exec;
}

// Test TypeError realm when replacing RegExp.prototype.exec with foreign exec function (1)
{
  const {
    TypeError: foreignTypeError,
    RegExp: {
      prototype: {
        exec: foreignExec,
      }
    }
  } = new Reflect.Realm().global;

  assertThrows(foreignTypeError, () => ({__proto__: RegExp.prototype, exec: foreignExec}).test());
  assertThrows(foreignTypeError, () => Object.assign(RegExp[Symbol.create](), {exec: foreignExec}).test());
}

// Test TypeError realm when replacing RegExp.prototype.exec with foreign exec function (2)
{
  const {
    TypeError: foreignTypeError,
    RegExp: {
      prototype: {
        exec: foreignExec,
      }
    }
  } = new Reflect.Realm().global;

  const RegExp_prototype_exec = RegExp.prototype.exec;
  RegExp.prototype.exec = foreignExec;
  assertThrows(foreignTypeError, () => ({__proto__: RegExp.prototype}).test());
  assertThrows(foreignTypeError, () => RegExp[Symbol.create]().test());
  RegExp.prototype.exec = RegExp_prototype_exec;
}
