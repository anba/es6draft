/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 8.1.1.1.6 GetBindingValue: Throw ReferenceError on access to uninitialized binding
// https://bugs.ecmascript.org/show_bug.cgi?id=2709

// Global code, block level
{
  assertThrows(ReferenceError, () => x);
  let x;
}

// Function code, non-strict
(function() {
  assertThrows(ReferenceError, () => x);
  let x;
})();

// Function code, block level, non-strict
(function() {
  {
    assertThrows(ReferenceError, () => x);
    let x;
  }
})();

// Function code, strict
(function() {
  "use strict";
  assertThrows(ReferenceError, () => x);
  let x;
})();

// Function code, block level, strict
(function() {
  "use strict";
  {
    assertThrows(ReferenceError, () => x);
    let x;
  }
})();
