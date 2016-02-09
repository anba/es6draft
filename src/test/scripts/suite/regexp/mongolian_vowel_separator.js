/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

let expressions = [
  {expr: /^\s$/, result: false},
  {expr: /^[\s]$/, result: false},
  {expr: /^[^\S]$/, result: false},

  {expr: /^\S$/, result: true},
  {expr: /^[\S]$/, result: true},
  {expr: /^[^\s]$/, result: true},
];

for (let {expr, result} of expressions) {
  assertSame(result, expr.test("\u180E"), `${expr}`);
}

