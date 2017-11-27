/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// static get constructor should be valid
// https://bugs.ecmascript.org/show_bug.cgi?id=3223

class C1 { constructor() {} }
assertSyntaxError(`class C2 { *constructor() {} }`);
assertSyntaxError(`class C3 { get constructor() {} }`);
assertSyntaxError(`class C4 { set constructor(x) {} }`);

class C5 { static constructor() {} }
class C6 { static *constructor() {} }
class C7 { static get constructor() {} }
class C8 { static set constructor(x) {} }
