/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

for (let i = 0; i <= 07; ++i) {
  let n = i.toString(8);
  let s = String.fromCharCode(i);
  assertSame(eval(`"\\${n}"`), s);
  assertSame(eval(`"\\0${n}"`), s);
  assertSame(eval(`"\\00${n}"`), s);

  assertSame(eval(`"\\000${n}"`), ("\0" + n));
}

for (let i = 010; i <= 077; ++i) {
  let n = i.toString(8);
  let s = String.fromCharCode(i);
  assertSame(eval(`"\\${n}"`), s);
  assertSame(eval(`"\\0${n}"`), s);

  assertSame(eval(`"\\00${n}"`), (String.fromCharCode(i >> 3) + n.slice(-1)));
  assertSame(eval(`"\\000${n}"`), ("\0" + n));
}

for (let i = 0100; i <= 0377; ++i) {
  let n = i.toString(8);
  let s = String.fromCharCode(i);
  assertSame(eval(`"\\${n}"`), s);

  assertSame(eval(`"\\0${n}"`), (String.fromCharCode(i >> 3) + n.slice(-1)));
  assertSame(eval(`"\\00${n}"`), (String.fromCharCode(i >> 6) + n.slice(-2)));
  assertSame(eval(`"\\000${n}"`), ("\0" + n));
}

for (let i = 0400; i <= 0777; ++i) {
  let n = i.toString(8);
  let s = String.fromCharCode(i);

  assertSame(eval(`"\\${n}"`), (String.fromCharCode(i >> 3) + n.slice(-1)));
  assertSame(eval(`"\\0${n}"`), (String.fromCharCode(i >> 3) + n.slice(-1)));
  assertSame(eval(`"\\00${n}"`), (String.fromCharCode(i >> 6) + n.slice(-2)));
  assertSame(eval(`"\\000${n}"`), ("\0" + n));
}
