/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

for (let i = 0; i <= 07; ++i) {
  let n = i.toString(8);
  assertSame(eval(`0${n}`), i);
  assertSame(eval(`00${n}`), i);
  assertSame(eval(`000${n}`), i);
}

for (let i = 010; i <= 077; ++i) {
  let n = i.toString(8);
  assertSame(eval(`0${n}`), i);
  assertSame(eval(`00${n}`), i);
  assertSame(eval(`000${n}`), i);
}

for (let i = 0100; i <= 0377; ++i) {
  let n = i.toString(8);
  assertSame(eval(`0${n}`), i);
  assertSame(eval(`00${n}`), i);
  assertSame(eval(`000${n}`), i);
}

for (let i = 0400; i <= 0777; ++i) {
  let n = i.toString(8);
  assertSame(eval(`0${n}`), i);
  assertSame(eval(`00${n}`), i);
  assertSame(eval(`000${n}`), i);
}
