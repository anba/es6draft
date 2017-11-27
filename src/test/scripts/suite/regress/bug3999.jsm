/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 15.2.1.17 HostResolveImportedModule: Missing requirement to instantiate modules ?
// https://bugs.ecmascript.org/show_bug.cgi?id=3999

assertSame("hello world", fooModule.foo);

import {} from "./resources/bug3999_1.jsm";
import* as fooModule from "./resources/bug3999_2.jsm";
export {fooModule};

assertSame("hello world", fooModule.foo);
