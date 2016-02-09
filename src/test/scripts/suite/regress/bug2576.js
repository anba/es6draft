/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 9.4.5.2 [[DefineOwnProperty]]: Condition in step 3.c.iii is never true
// https://bugs.ecmascript.org/show_bug.cgi?id=2576

// let ta = new class extends Int8Array { constructor() { /* no super */ } };
// for (let w of [{}, {writable: true}, {writable: false}]) {
//   for (let e of [{}, {enumerable: true}, {enumerable: false}]) {
//     for (let c of [{}, {configurable: true}, {configurable: false}]) {
//       let d = Object.assign({value: 1}, w, e, c);
//       for (let index of [-0, +0, -1, +1]) {
//         assertThrows(TypeError, () => Reflect.defineProperty(ta, index, d));
//       }
//     }
//   }
// }
