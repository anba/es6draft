/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue
} = Assert;

const values = [
  "\\u{10ffff}",
  "\u{10ffff}",
  "\\udbff\\udfff",
  "\udbff\udfff",
];

for (let {start, end} of [for (start of values) for (end of values) {start, end}]) {
  assertTrue(new RegExp(`[${start}-${end}]`, "u").test("\u{10ffff}"));
}
