/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

// Invalid direct eval call and tail calls:
// - direct eval fallback and 'wrong' eval function have both tail calls enabled
// - chaining them should preserve the tail call property

let realm = new Realm({
  directEval: {
    fallback(thisArgument, callee, ...args) {
      "use strict";
      return callee(...args);
    }
  }
});
realm.eval(`
  function returnCaller() {
    return returnCaller.caller;
  }

  function tailCall() {
    "use strict";
    return returnCaller();
  }

  function testFunction() {
    return eval("123");
  }

  eval = tailCall;
`);

assertSame(realm.global.testFunction, realm.eval(`testFunction()`));
