/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 21.2.5.7 RegExp.prototype.replace, 21.2.5.2.1 RegExpExec: Dynamic flags retrieval is unsafe
// https://bugs.ecmascript.org/show_bug.cgi?id=2625
/**
 * ES2015 section 21.2.5.8 ("RegExp.prototype [ @@replace ]"), step 16.p.i
 * reads:
 *
 * > NOTE position should not normally move backwards. If it does, it is an
 * > indication of an ill-behaving RegExp subclass or use of an access
 * > triggered side-effect to change the global flag or other characteristics
 * > of rx. In such cases, the corresponding substitution is ignored.
 */

{
  let re = /test/g;
  let c = 0;
  re.exec = function() {
    let result;
    c += 1;
    if (c == 1) {
      this.lastIndex = 8;
      let val = ["test"];
      val.index = 4;
      return val;
    }
    if (c == 2) {
      result = ["pre"];
      result.index = 0;
      return result;
    }
    return null;
  };
  let s = "pre-test".replace(re, () => {});
  assertSame("pre-undefined", s);
}
