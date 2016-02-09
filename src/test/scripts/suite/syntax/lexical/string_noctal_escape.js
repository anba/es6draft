/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// noctal
for (let i = 8; i <= 9; ++i) {
  let n = i.toString(10);
  assertSame(eval(`"\\${n}"`), n);
  assertSame(eval(`"\\0${n}"`), ("\0" + n));
  assertSame(eval(`"\\00${n}"`), ("\0" + n));
  assertSame(eval(`"\\000${n}"`), ("\0" + n));
}

// Trailing 8-9
for (let j = 8; j <= 9; ++j) {
  for (let i = 0; i <= 07; ++i) {
    let n = i.toString(8);
    let s = String.fromCharCode(i);
    assertSame(eval(`"\\${n}${j}"`), s + j);
    assertSame(eval(`"\\0${n}${j}"`), s + j);
    assertSame(eval(`"\\00${n}${j}"`), s + j);

    assertSame(eval(`"\\000${n}${j}"`), ("\0" + n + j));
  }
}

for (let j = 8; j <= 9; ++j) {
  for (let i = 010; i <= 077; ++i) {
    let n = i.toString(8);
    let s = String.fromCharCode(i);
    assertSame(eval(`"\\${n}${j}"`), s + j);
    assertSame(eval(`"\\0${n}${j}"`), s + j);

    assertSame(eval(`"\\00${n}${j}"`), (String.fromCharCode(i >> 3) + n.slice(-1) + j));
    assertSame(eval(`"\\000${n}${j}"`), ("\0" + n + j));
  }
}

for (let j = 8; j <= 9; ++j) {
  for (let i = 0100; i <= 0377; ++i) {
    let n = i.toString(8);
    let s = String.fromCharCode(i);
    assertSame(eval(`"\\${n}${j}"`), s + j);

    assertSame(eval(`"\\0${n}${j}"`), (String.fromCharCode(i >> 3) + n.slice(-1) + j));
    assertSame(eval(`"\\00${n}${j}"`), (String.fromCharCode(i >> 6) + n.slice(-2) + j));
    assertSame(eval(`"\\000${n}${j}"`), ("\0" + n + j));
  }
}
