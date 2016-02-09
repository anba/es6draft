/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
let n = 10000;
let locals = Array(n).fill(0).map((x, i) => `let a${i} = 1;`).join("");
let sum = `[${Array(n).fill(0).map((x, i) => "a" + i).join(",")}].reduce((a, b) => a + b, 0)`;

let blockResult = eval(`{${locals} ${sum}}`);
if (blockResult !== n) throw new Error(n);

let switchResult = eval(`switch(0) { default: ${locals} ${sum} }`);
if (switchResult !== n) throw new Error(n);
