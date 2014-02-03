/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertUndefined
} = Assert;

// 8.4.2.4: ArraySetLength algorithm can produce broken arrays
// https://bugs.ecmascript.org/show_bug.cgi?id=1200

function defLen(arr, len, f) {
  Object.defineProperty(arr, "length", {
    value: {valueOf: function(){ f && f(); return len }}
  });
}

a = [];
defLen(a, 1, function() {defLen(a, 10); a[5]='test'});

assertSame(1, a.length);
for (let i = 0; i < 10; ++i) {
  assertUndefined(a[i]);
}
