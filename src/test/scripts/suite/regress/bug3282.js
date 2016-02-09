/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, assertSyntaxError
} = Assert;

// 12.3.5.3 step 6: throw TypeError instead of ReferenceError
// https://bugs.ecmascript.org/show_bug.cgi?id=3282

assertSyntaxError(`
assertThrows(TypeError, () => {
  Object.setPrototypeOf(function(){ new super }, null)();
});
`);
