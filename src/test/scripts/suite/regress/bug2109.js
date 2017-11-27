/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 9.2.11 SetFunctionName: Assignment to wrong variable in step 3b
// https://bugs.ecmascript.org/show_bug.cgi?id=2109

let desc = "my-description"
let sym = Symbol(desc);
assertSame(`[${desc}]`, Object.getOwnPropertyDescriptor({[sym](){}}, sym).value.name);
