/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
// No Crash
throws(SyntaxError, () => parseScript("let {a:b,@"));
throws(SyntaxError, () => parseScript("({a:0,@"));
throws(SyntaxError, () => parseModule("import {a,@"));

function throws(error, fn) {
  try {
    fn();
  } catch (e) {
    if (e instanceof error) {
      return;
    }
  }
  throw new Error();
}
