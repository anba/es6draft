/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue
} = Assert;

for (let i = 0; i <= 07; ++i) {
  let n = i.toString(8);
  let s = String.fromCharCode(i);
  assertTrue(new RegExp(`^\\${n}$`).test(s));
  assertTrue(new RegExp(`^\\0${n}$`).test(s));
  assertTrue(new RegExp(`^\\00${n}$`).test(s));

  assertTrue(new RegExp(`^\\000${n}$`).test("\0" + n));
}

for (let i = 010; i <= 077; ++i) {
  let n = i.toString(8);
  let s = String.fromCharCode(i);
  assertTrue(new RegExp(`^\\${n}$`).test(s));
  assertTrue(new RegExp(`^\\0${n}$`).test(s));

  assertTrue(new RegExp(`^\\00${n}$`).test(String.fromCharCode(i >> 3) + n.slice(-1)));
  assertTrue(new RegExp(`^\\000${n}$`).test("\0" + n));
}

for (let i = 0100; i <= 0377; ++i) {
  let n = i.toString(8);
  let s = String.fromCharCode(i);
  assertTrue(new RegExp(`^\\${n}$`).test(s));

  assertTrue(new RegExp(`^\\0${n}$`).test(String.fromCharCode(i >> 3) + n.slice(-1)));
  assertTrue(new RegExp(`^\\00${n}$`).test(String.fromCharCode(i >> 6) + n.slice(-2)));
  assertTrue(new RegExp(`^\\000${n}$`).test("\0" + n));
}

for (let i = 0400; i <= 0777; ++i) {
  let n = i.toString(8);
  let s = String.fromCharCode(i);

  assertTrue(new RegExp(`^\\${n}$`).test(String.fromCharCode(i >> 3) + n.slice(-1)));
  assertTrue(new RegExp(`^\\0${n}$`).test(String.fromCharCode(i >> 3) + n.slice(-1)));
  assertTrue(new RegExp(`^\\00${n}$`).test(String.fromCharCode(i >> 6) + n.slice(-2)));
  assertTrue(new RegExp(`^\\000${n}$`).test("\0" + n));
}
