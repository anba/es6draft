/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
var tests = [
  [`[]`, `{}`],
  [`[]`, `/a/`],
  [`function(){}`, `{}`],
  [`function(){}`, `function*(){}`],
  [`void 0`, `null`],
  [`[]`, `null`],
  [`/a/`, `null`],
  [`0.1`, `null`],
  [`0`, `null`],
  [`0`, `0.2`],
  [`0`, `""`],
  [`0.4`, `""`],
  [`1`, `true`],
  [`c >>> 0`, `null`],
  [`true`, `null`],
  [`""`, `null`],
  [`"a" + b`, `null`],
  [`"a" + b`, `""`],
];

var GeneratorFunction = function*(){}.constructor;

for (var [left, right] of tests) {
  GeneratorFunction(`f(p() ? ${left} : ${right})`);
  GeneratorFunction(`f(p() ? ${right} : ${left})`);
}
