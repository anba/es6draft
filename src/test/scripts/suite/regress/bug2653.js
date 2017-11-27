/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// Trailing Comma in ArrayBindingPattern, ArrayAssignmentPattern
// https://bugs.ecmascript.org/show_bug.cgi?id=2653

// No trailing comma after non-rest element
Function(`var [a] = []`);
Function(`let [a] = []`);
Function(`[a] = []`);
Function(`var {a} = []`);
Function(`let {a} = []`);
Function(`({a} = [])`);

// Trailing comma after non-rest element
Function(`var [a,] = []`);
Function(`let [a,] = []`);
Function(`[a,] = []`);
Function(`var {a,} = []`);
Function(`let {a,} = []`);
Function(`({a,} = [])`);

// No trailing comma after rest element
Function(`var [...a] = []`);
Function(`let [...a] = []`);
Function(`[...a] = []`);

// Trailing comma after rest element
assertSyntaxError(`var [...a,] = []`);
assertSyntaxError(`let [...a,] = []`);
assertSyntaxError(`[...a,] = []`);
