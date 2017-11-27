/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
// excess methods in class declaration
Function(`class C { ${ new Array(0x700).fill(0).map((x, i) => `m${i}(){}`).join("\n") } }`);

// excess methods in class expression
Function(`(class { ${ new Array(0x700).fill(0).map((x, i) => `m${i}(){}`).join("\n") } });`);
