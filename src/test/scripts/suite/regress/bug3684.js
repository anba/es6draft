/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 9.2.3 [[Construct]], step 14: Handle explicit return with no value for derived constructor
// https://bugs.ecmascript.org/show_bug.cgi?id=3684

new class extends class {} {
  constructor() {
    super();
    return;
}};
