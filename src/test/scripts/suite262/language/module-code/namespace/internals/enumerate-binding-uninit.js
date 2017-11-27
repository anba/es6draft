/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
id: sec-integer-indexed-exotic-objects-getownproperty-p
info: Test for-in enumeration with uninitialized binding.
description: >
  9.4.6.5 [[GetOwnProperty]] (P)
    ...
    4. Let value be ? O.[[Get]](P, O).
    ...

  9.4.6.8 [[Get]] (P, Receiver)
    ...
    12. Let targetEnvRec be targetEnv's EnvironmentRecord.
    13. Return ? targetEnvRec.GetBindingValue(binding.[[BindingName]], true). 

  13.7.5.15 EnumerateObjectProperties (O)
    ...
    EnumerateObjectProperties must obtain the own property keys of the
    target object by calling its [[OwnPropertyKeys]] internal method.
    Property attributes of the target object must be obtained by
    calling its [[GetOwnProperty]] internal method.

flags: [module]
---*/

import* as self from "./enumerate-binding-uninit.js";

assert.throws(ReferenceError, function() {
  for (var key in self) {
    throw new Test262Error();
  }
});

export default 0;
