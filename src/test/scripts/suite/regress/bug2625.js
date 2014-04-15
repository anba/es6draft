/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

// 21.2.5.7 RegExp.prototype.replace, 21.2.5.2.1 RegExpExec: Dynamic flags retrieval is unsafe
// https://bugs.ecmascript.org/show_bug.cgi?id=2625

{
  let re = /test/;
  let glob = true;
  let c = 0;
  Object.defineProperty(re, "global", {
    get() {
      c += 1;
      if (c == 3) {
        re.compile(/pre/);
      }
      if (c == 4) {
        re.compile(/kaboom/);
      }
      let g = glob;
      glob = false;
      return g;
    }
  });
  let s = "pre-test".replace(re, () => {});
  assertSame("pre-undefined", s);
}
