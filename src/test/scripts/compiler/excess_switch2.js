/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
function switchBody(f, g) {
  var s = "";
  for (var i = 0; i < 20000; ++i) s += `case ${f(i)}: ${g(i)}`;
  return s
}

Function(`
  switch (v) {
    ${switchBody(i => `v${i}`, i => `r = w${i} * 10; break;`)}
  }
`);

void Function(`
  switch (v) {
    ${switchBody(i => `v${i}`, i => `r1 = w${i} * 10; r2 = w${i} * 20; r3 = w${i} * 30; break;`)}
  }
`);
