/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
// No Crash
function f1() {
  function g() { }
  with ({}) {
    return g();
  }
}
f1();

function f2() {
  with ({}) {
    function g() {
      function h() { }
      return h();
    }
    g();
  }
}
f2();

function f3() {
  with ({}) {
    let g = () => {};
    g();
  }
}
f3();
