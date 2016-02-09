/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// 12.3: Optional Arguments in "MemberExpression : new super Arguments" leads to Shift/Reduce conflicts
// https://bugs.ecmascript.org/show_bug.cgi?id=2665

(class extends null {
  constructor() {
    new super();
  }
});

(class extends null {
  constructor() {
    new super;
  }
});
