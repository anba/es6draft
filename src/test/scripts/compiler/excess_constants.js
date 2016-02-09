/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
// excess string constants
Function(new Array(0x7fff).fill(0).map((x, i) => `f('${i}')`).join(";"))

// excess number constants
Function(new Array(0x7fff).fill(0).map((x, i) => `f(${1 / i})`).join(";"))
