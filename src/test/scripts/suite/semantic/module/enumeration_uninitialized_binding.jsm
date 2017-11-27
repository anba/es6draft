/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, fail
} = Assert;

import* as self from "./enumeration_uninitialized_binding.jsm";

// ES2016 change: Uninitialized binding throws ReferenceError during [[GetOwnProperty]].
assertThrows(ReferenceError, () => {
  for (var k in self) fail `unreachable`;
});

export default 123;
