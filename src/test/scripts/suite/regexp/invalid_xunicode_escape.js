/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows
} = Assert;

// no lenient error behaviour for invalid extended unicode escape sequences

const values = [
  "\\u{",
  "\\u{0",
  "\\u{f",
  "\\u{}",
  "\\u{h}",
  "\\u{ah}",
  "\\u{ha}",
];

for (let v of values) {
  assertThrows(() => RegExp(v, "u"), SyntaxError);
}
