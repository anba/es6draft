/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame,
  assertFalse,
  assertTrue,
  assertUndefined,
} = Assert;

// Indirect eval call
// TODO: test with multiple arguments, different thisArgument
{
  let called = false;
  let realm = new Realm({
    indirectEval() {
      assertFalse(called);
      called = true;
      return "123";
    }
  });
  assertFalse(called);
  let result = realm.eval("(0, eval)('1')");
  assertTrue(called);
  assertSame(123, result);
}

// Valid direct eval call
// TODO: test with multiple arguments, different thisArgument, 'fallback' present
{
  let called = false;
  let realm = new Realm({
    directEval: {
      translate(source) {
        assertFalse(called);
        called = true;
        return "123";
      }
    }
  });
  assertFalse(called);
  let result = realm.eval("eval('1')");
  assertTrue(called);
  assertSame(123, result);
}

// Invalid direct eval call
// TODO: test with multiple arguments, different thisArgument, 'translate' present
{
  let called = false;
  function notEval() {
    return 123;
  }
  let realm = new Realm({
    directEval: {
      fallback(thisArgument, callee, ...args) {
        assertFalse(called);
        called = true;
        assertUndefined(thisArgument);
        assertSame(notEval, callee);
        assertSame(1, args.length);
        assertSame('1', args[0]);
        return notEval();
      }
    }
  });
  assertFalse(called);
  realm.global.eval = notEval;
  let result = realm.eval("eval('1')");
  assertTrue(called);
  assertSame(123, result);
}
