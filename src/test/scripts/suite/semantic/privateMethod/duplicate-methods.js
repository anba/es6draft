/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

let methods = [`#m(){}`, `get #m(){}`, `set #m(v){}`];
let placement = ["static", ""];

for (let p1 of placement) {
  for (let m1 of methods) {
    for (let p2 of placement) {
      for (let m2 of methods) {
        if (p1 === p2 && ((m1.startsWith("get") && m2.startsWith("set")) || (m1.startsWith("set") && m2.startsWith("get")))) {
          continue;
        }

        assertSyntaxError(`
          class C {
            ${p1} ${m1}
            ${p2} ${m2}
          }
        `);
      }
    }
  }
}