/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

// Test cases from 'https://bugs.ecmascript.org/show_bug.cgi?id=1751'

{
  let realm = new Realm();
  realm.eval(`
    var o1, o2;
    with (o1 = {set x(v) { throw "error"; }}) {
      with (o2 = {get x() { delete this.x; return 2; }}) {
        x += 1;
      }
    }
    o2.x;
  `);

  assertSame(3, realm.global.o2.x);
}

{
  let realm = new Realm();
  let result = realm.eval(`
    var x = 1;
    (function() {
      x += eval("var x = 2; x");
      return x;
    })();
  `);

  assertSame(2, result);
  assertSame(3, realm.global.x);
}

{
  let realm = new Realm();
  realm.eval(`
    (function(global) {
      "use strict";
      Object.defineProperty(global, "x", {
        configurable: true,
        get: function() {
          delete this.x;
          return 2;
        }
      });
      x += 1;
    })(this);
  `);

  assertSame(3, realm.global.x);
}
