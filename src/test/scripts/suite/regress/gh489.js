/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  fail
} = Assert;

// Use [[OriginalFlags]] from within RegExpBuiltinExec?
// https://github.com/tc39/ecma262/issues/489

var re = /(?:)/;

Object.defineProperty(re, "global", {
  get() { fail `Executed global accessor` }
});
Object.defineProperty(re, "sticky", {
  get() { fail `Executed sticky accessor` }
});

re.exec("");
