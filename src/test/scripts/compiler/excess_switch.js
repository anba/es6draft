/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
function switchBody(f) {
  var s = "";
  for (var i = 0; i < 20000; ++i) s += `case ${f(i)}:`;
  return s
}

Function(`
  switch (v) {
    ${switchBody(i => `v${i}`)}
  }
`);

Function(`
  switch (v) {
    ${switchBody(i => i)}
  }
`);

Function(`
  switch (v|0) {
    ${switchBody(i => i)}
  }
`);

Function(`
  switch (v) {
    ${switchBody(i => `'\\u${i.toString(16).padStart(4, "0")}'`)}
  }
`);

Function(`
  switch (v+"") {
    ${switchBody(i => `'\\u${i.toString(16).padStart(4, "0")}'`)}
  }
`);

Function(`
  switch (v) {
    ${switchBody(i => `'c${i}'`)}
  }
`);

Function(`
  switch (v+"") {
    ${switchBody(i => `'c${i}'`)}
  }
`);
