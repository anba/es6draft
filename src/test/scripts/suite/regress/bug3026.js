/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
} = Assert;

// 9.5.12 [[OwnPropertyKeys]] ( ): Missing type check / conversion for property keys in step 20
// https://bugs.ecmascript.org/show_bug.cgi?id=3026

try {
  Reflect.ownKeys(new Proxy({}, {
    ownKeys() {
      return [0, null, {}];
    }
  }));
} catch (e) {
}
