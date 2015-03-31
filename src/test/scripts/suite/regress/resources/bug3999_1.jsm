/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertNotUndefined, assertThrows
} = Assert;

assertNotUndefined(main);
assertNotUndefined(main.fooModule);
assertThrows(ReferenceError, () => main.fooModule.foo);

import* as main from "../bug3999.jsm";

assertThrows(ReferenceError, () => main.fooModule.foo);

Promise.resolve(0).then(() => {
  assertSame("hello world", main.fooModule.foo);
});
