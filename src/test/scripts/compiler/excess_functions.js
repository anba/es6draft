/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
// excess function expressions
eval(new Array(0xfff).fill(0).map((x, i) => `a=function f${i}(){}`).join(";"))

// excess function declarations
// - still broken
// eval(new Array(0xfff).fill(0).map((x, i) => `function f${i}(){}`).join(";"))
// - still broken
// eval("{" + new Array(0xfff).fill(0).map((x, i) => `function f${i}(){}`).join(";") + "}")
