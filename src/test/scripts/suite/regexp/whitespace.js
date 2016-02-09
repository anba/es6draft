/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 11.2 White Space
const whiteSpace = "\u0009\u000B\u000C\u0020\u00A0\uFEFF";
// 11.3 Line Terminators
const lineTerminators = "\u000A\u000D\u2028\u2029";
// Unicode character class Zs
const spaceSeparator = "\u0020\u00A0\u1680\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200A\u202F\u205F\u3000";

const allWhiteSpace = whiteSpace + lineTerminators + spaceSeparator;

let expressions = [
  {expr: /^\s$/, result: true},
  {expr: /^[\s]$/, result: true},
  {expr: /^[^\S]$/, result: true},

  {expr: /^\S$/, result: false},
  {expr: /^[\S]$/, result: false},
  {expr: /^[^\s]$/, result: false},
];

for (let {expr, result} of expressions) {
  for (let s of allWhiteSpace) {
    let codePoint = s.codePointAt(0);
    assertSame(result, expr.test(s), `${expr}.test("\\u${"0".repeat(3 - ((Math.log2(codePoint) / 4) >>> 0)) + codePoint.toString(16)}")`);
  }
}

